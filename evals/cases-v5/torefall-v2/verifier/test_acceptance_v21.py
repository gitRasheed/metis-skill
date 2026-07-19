"""Torefall v2 hidden acceptance — v2.1 (post-xhigh-critique).

Changes vs v2.0 (adjudication 2026-07-19, critique at scratchpad/critique-tf2):
- g03 mana-fail now driven purely through the contract (six amplifies drain
  to 10, the seventh must fail cleanly) instead of poking player fields.
- g04 asserts the contract-mandated target exposure strictly ("wa", not
  None) and adds a disconnect-exclusion fixture (contract-only).
- g05 asserts the taunt threat fixture (warrior -> 210) and that the boss
  REVERTS to the new threat leader on the fourth tick (kills permanent-taunt
  implementations).
- g09 asserts explicit rejections for losers (TASK: "rejected claimants get
  an explicit rejection"); g10 retries the gold pile under a DIFFERENT
  request key (exactly-once per claimant, not per key).
- g12 asserts the item exists exactly once across all inventories after
  settlement (double-spend via auction must not mint a second instance).
- g15 adds a divergence check (a different script must serialize
  differently) so a constant serializer fails.
- g17 tautology fixed: the invalid state (phase_two AND enraged) now fails,
  and each phase flag may transition at most once (no oscillation).
- NEW g19 auction expiry (50 ticks: item returned, standing bid refunded,
  gold conserved); NEW g20 mana regen +2/tick capped at 100.
- Stage-B awareness: TOREFALL_STAGE=B makes g13 expect g0 - 2 (the 5%%
  burned buyout fee on the 40-gold buyout: floor(40*0.05) = 2).
KNOWN LIMITS (need TASK v3, kept for run-comparability): death-dependent
gates still force hp/alive through player objects because boss tick damage
is unspecified in TASK (a contract-only death path needs that number);
loot IDs d1/d2/d3 remain starter-seeded pending TASK documentation; the
g11 escrow-vs-destroyed window stays unobservable through this contract.
"""
import os
import unittest

from torefall.world import (auction_bid, auction_buyout, auction_list,
                            cast, claim_loot, confirm_trade, craft,
                            create_world, disconnect, get_gold,
                            get_inventory, get_state, join, offer_trade,
                            reconnect, serialize, start_raid, tick)

STAGE_B = os.environ.get("TOREFALL_STAGE") == "B"


def rw(seed=7):
    w = create_world(seed)
    for pid, c in (("wa", "warrior"), ("ma", "mage"), ("pr", "priest")):
        join(w, pid, c)
    start_raid(w, ["wa", "ma", "pr"])
    return w


def player(w, pid):
    ps = w.players
    return ps[pid] if isinstance(ps, dict) else getattr(ps, pid)


def force_dead(w, pid):
    p = player(w, pid)
    try:
        p.hp = 0
        p.alive = False
    except AttributeError:
        p["hp"] = 0
        p["alive"] = False


def set_mana(w, pid, value):
    p = player(w, pid)
    try:
        p.mana = value
    except AttributeError:
        p["mana"] = value


def kill(w):
    while get_state(w, "wa")["boss"]["alive"]:
        for p in ("wa", "ma", "pr"):
            if get_state(w, p)["boss"] and get_state(w, p)["boss"]["alive"]:
                cast(w, p, "attack")


def total_gold(w, pids):
    return sum(get_gold(w, p) for p in pids)


class V21(unittest.TestCase):
    def test_g01_buff_stack(self):
        w = rw()
        cast(w, "wa", "shout"); cast(w, "ma", "amplify")
        self.assertEqual(cast(w, "ma", "attack")["damage"], 52)

    def test_g02_res_sick_halving_last(self):
        w = rw()
        force_dead(w, "ma")
        cast(w, "pr", "resurrect", "ma")
        cast(w, "wa", "shout"); cast(w, "ma", "amplify")
        self.assertEqual(cast(w, "ma", "attack")["damage"], 26)

    def test_g03_mana_insufficient_clean_fail(self):
        # contract-only: amplify costs 15, no ticks => no regen; the 7th
        # amplify from 100 mana (6x15 = 90 spent, 10 left) must fail cleanly
        w = rw()
        for _ in range(6):
            self.assertTrue(cast(w, "ma", "amplify").get("ok", True))
        self.assertEqual(get_state(w, "ma")["mana"], 10)
        r = cast(w, "ma", "amplify")
        self.assertFalse(r.get("ok"))
        self.assertEqual(get_state(w, "ma")["mana"], 10, "failed cast cost mana")

    def test_g04_threat_targeting(self):
        w = rw()
        for _ in range(3): cast(w, "wa", "attack")
        cast(w, "ma", "attack")
        tick(w)
        # contract mandates boss target exposure in get_state
        self.assertEqual(get_state(w, "wa")["boss"]["target"], "wa")
        # disconnect exclusion: threat is ignored while down, not erased
        disconnect(w, "wa")
        tick(w)
        self.assertEqual(get_state(w, "ma")["boss"]["target"], "ma",
                         "disconnected player still targeted")
        reconnect(w, "wa")
        tick(w)
        self.assertEqual(get_state(w, "wa")["boss"]["target"], "wa",
                         "threat erased by disconnect")

    def test_g05_taunt_overrides_three_ticks_then_reverts(self):
        w = rw()
        for _ in range(5): cast(w, "ma", "attack")  # ma threat 200
        cast(w, "wa", "taunt")
        self.assertEqual(get_state(w, "wa")["threat"], 210,
                         "taunt must set warrior threat to current-max + 10")
        for _ in range(3):
            tick(w)
            self.assertEqual(get_state(w, "wa")["boss"]["target"], "wa")
            cast(w, "ma", "attack")  # ma overtakes during the taunt window
        tick(w)
        self.assertEqual(get_state(w, "wa")["boss"]["target"], "ma",
                         "taunt did not expire after exactly 3 ticks")

    def test_g06_dead_no_act_no_threat(self):
        w = rw()
        force_dead(w, "ma")
        self.assertFalse(cast(w, "ma", "attack").get("ok"))

    def test_g07_resurrect_once_only(self):
        w = rw()
        force_dead(w, "ma")
        set_mana(w, "pr", 100)
        r1 = cast(w, "pr", "resurrect", "ma")
        force_dead(w, "ma")
        set_mana(w, "pr", 100)
        r2 = cast(w, "pr", "resurrect", "ma")
        self.assertTrue(r1.get("ok"))
        self.assertFalse(r2.get("ok"), "resurrected twice in one encounter")

    def test_g08_loot_idempotent(self):
        w = rw(); kill(w)
        claim_loot(w, "ma", "d2", "k1"); claim_loot(w, "ma", "d2", "k1")
        self.assertEqual(get_inventory(w, "ma").count("herb"), 1)

    def test_g09_unique_single_winner_explicit_rejections(self):
        w = rw(); kill(w)
        results = [claim_loot(w, p, "d1", f"k{p}") for p in ("wa", "ma", "pr")]
        holders = sum("tore_crown" in get_inventory(w, p)
                      for p in ("wa", "ma", "pr"))
        self.assertEqual(holders, 1)
        oks = sum(bool(r.get("ok")) for r in results if isinstance(r, dict))
        self.assertEqual(oks, 1, "losers must receive an explicit rejection")

    def test_g10_gold_pile_once_per_claimant_any_key(self):
        w = rw(); kill(w)
        g0 = get_gold(w, "wa")
        claim_loot(w, "wa", "d3", "k1")
        claim_loot(w, "wa", "d3", "k1")
        claim_loot(w, "wa", "d3", "k2")  # different key, same claimant
        self.assertEqual(get_gold(w, "wa"), g0 + 50,
                         "gold pile granted more than once per claimant")

    def test_g11_trade_atomic_replay(self):
        w = rw(); kill(w); claim_loot(w, "wa", "d2", "k")
        t = offer_trade(w, "wa", "ma", ["herb"], [])
        confirm_trade(w, "wa", t["trade_id"])
        self.assertNotIn("herb", get_inventory(w, "ma"))
        total = get_inventory(w, "wa").count("herb") + \
                get_inventory(w, "ma").count("herb")
        self.assertLessEqual(total, 1)
        confirm_trade(w, "ma", t["trade_id"])
        confirm_trade(w, "ma", t["trade_id"])
        self.assertEqual(get_inventory(w, "ma").count("herb"), 1)

    def test_g12_pending_trade_locks_item(self):
        w = rw(); kill(w); claim_loot(w, "wa", "d2", "k")
        t = offer_trade(w, "wa", "ma", ["herb"], [])
        listed_ok = False
        try:
            r = auction_list(w, "wa", "herb", 1, 10)
            listed_ok = isinstance(r, dict) and r.get("ok", True) \
                and "auction_id" in r
        except Exception:
            pass
        self.assertFalse(listed_ok, "locked item accepted for listing")
        confirm_trade(w, "wa", t["trade_id"])
        confirm_trade(w, "ma", t["trade_id"])
        herb_total = sum(get_inventory(w, p).count("herb")
                         for p in ("wa", "ma", "pr"))
        self.assertEqual(get_inventory(w, "ma").count("herb"), 1,
                         "item double-spent across trade + auction")
        self.assertEqual(herb_total, 1, "auction minted a second instance")

    def test_g13_auction_escrow_conservation(self):
        w = rw(); kill(w); claim_loot(w, "wa", "d2", "k")
        pids = ("wa", "ma", "pr")
        g0 = total_gold(w, pids)
        aid = auction_list(w, "wa", "herb", 5, 40)["auction_id"]
        auction_bid(w, "ma", aid, 10)
        auction_bid(w, "pr", aid, 20)  # ma must be refunded
        self.assertEqual(get_gold(w, "ma"), 100, "outbid gold not refunded")
        auction_buyout(w, "ma", aid)
        self.assertEqual(get_gold(w, "pr"), 100, "buyout did not refund bidder")
        self.assertIn("herb", get_inventory(w, "ma"))
        expected = g0 - 2 if STAGE_B else g0  # stage B burns floor(40*5%)
        self.assertEqual(total_gold(w, pids), expected, "gold not conserved")

    def test_g14_reconnect_preserves_all(self):
        w = rw(); kill(w); claim_loot(w, "pr", "d2", "k")
        for _ in range(5): cast(w, "pr", "attack")
        th = get_state(w, "pr")["threat"]
        disconnect(w, "pr"); tick(w); reconnect(w, "pr"); reconnect(w, "pr")
        st = get_state(w, "pr")
        self.assertIn("herb", get_inventory(w, "pr"))
        self.assertTrue(st["in_raid"])
        self.assertEqual(st["threat"], th, "threat lost on reconnect")

    def test_g15_determinism_and_divergence(self):
        def scripted(seed, extra_attack=False):
            w = create_world(seed)
            for pid, c in (("a", "warrior"), ("b", "mage")):
                join(w, pid, c)
            start_raid(w, ["a", "b"])
            cast(w, "a", "shout"); cast(w, "b", "amplify")
            cast(w, "b", "attack")
            if extra_attack:
                cast(w, "b", "attack")
            for _ in range(5): tick(w)
            return serialize(w)
        self.assertEqual(scripted(3), scripted(3))
        self.assertNotEqual(scripted(3), scripted(3, extra_attack=True),
                            "serializer blind to state differences")

    def test_g16_wipe_ends_encounter(self):
        w = rw()
        for p in ("wa", "ma", "pr"):
            force_dead(w, p)
        r = cast(w, "wa", "attack")
        self.assertFalse(r.get("ok"), "acted after wipe")

    def test_g17_phases_exclusive_and_monotonic(self):
        w = rw()
        seen = []
        for _ in range(130):
            tick(w)
            b = get_state(w, "wa")["boss"]
            if b is None: continue
            self.assertFalse(b["phase_two"] and b["enraged"],
                             "phase_two and enraged simultaneously")
            seen.append((bool(b["phase_two"]), bool(b["enraged"])))
        for idx in (0, 1):  # each flag transitions at most once, never back
            flips = sum(1 for a, b in zip(seen, seen[1:]) if a[idx] != b[idx])
            self.assertLessEqual(flips, 1,
                                 "phase flag oscillated (transition not exactly-once)")

    def test_g19_auction_expiry_returns_and_refunds(self):
        w = rw(); kill(w); claim_loot(w, "wa", "d2", "k")
        pids = ("wa", "ma", "pr")
        g0 = total_gold(w, pids)
        aid = auction_list(w, "wa", "herb", 5, 40)["auction_id"]
        auction_bid(w, "ma", aid, 10)
        for _ in range(50): tick(w)
        self.assertIn("herb", get_inventory(w, "wa"),
                      "expired auction did not return the item")
        self.assertEqual(get_gold(w, "ma"), 100,
                         "standing bid not refunded on expiry")
        self.assertEqual(total_gold(w, pids), g0, "gold not conserved on expiry")

    def test_g20_mana_regen_capped(self):
        w = rw()
        cast(w, "ma", "amplify")  # 100 -> 85
        self.assertEqual(get_state(w, "ma")["mana"], 85)
        tick(w)
        self.assertEqual(get_state(w, "ma")["mana"], 87, "regen is +2 per tick")
        for _ in range(20): tick(w)
        self.assertEqual(get_state(w, "ma")["mana"], 100, "mana exceeded cap")


if __name__ == "__main__":
    unittest.main()
