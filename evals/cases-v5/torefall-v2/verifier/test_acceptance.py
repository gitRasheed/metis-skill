"""Torefall v2 hidden acceptance — contract-only, conservation-checked."""
import unittest

from torefall.world import (auction_bid, auction_buyout, auction_list,
                            cast, claim_loot, confirm_trade, craft,
                            create_world, disconnect, get_gold,
                            get_inventory, get_state, join, offer_trade,
                            reconnect, serialize, start_raid, tick)


def rw(seed=7):
    w = create_world(seed)
    for pid, c in (("wa", "warrior"), ("ma", "mage"), ("pr", "priest")):
        join(w, pid, c)
    start_raid(w, ["wa", "ma", "pr"])
    return w


def kill(w):
    while get_state(w, "wa")["boss"]["alive"]:
        for p in ("wa", "ma", "pr"):
            if get_state(w, p)["boss"] and get_state(w, p)["boss"]["alive"]:
                cast(w, p, "attack")


def total_gold(w, pids):
    return sum(get_gold(w, p) for p in pids)


class V2(unittest.TestCase):
    def test_g01_buff_stack(self):
        w = rw()
        cast(w, "wa", "shout"); cast(w, "ma", "amplify")
        self.assertEqual(cast(w, "ma", "attack")["damage"], 52)

    def test_g02_res_sick_halving_last(self):
        w = rw()
        p = w.players["ma"]; p.hp = 0; p.alive = False
        cast(w, "pr", "resurrect", "ma")
        cast(w, "wa", "shout"); cast(w, "ma", "amplify")
        self.assertEqual(cast(w, "ma", "attack")["damage"], 26)

    def test_g03_mana_insufficient_clean_fail(self):
        w = rw()
        w.players["ma"].mana = 5
        r = cast(w, "ma", "amplify")
        self.assertFalse(r.get("ok"))
        self.assertEqual(get_state(w, "ma")["mana"], 5)

    def test_g04_threat_targeting(self):
        w = rw()
        for _ in range(3): cast(w, "wa", "attack")
        tick(w)
        # target may be unexposed (None) outside taunt, or the correct
        # threat-top raider; any OTHER name is a targeting error
        self.assertIn(get_state(w, "wa")["boss"]["target"], (None, "wa"))
        top = max(("wa", "ma", "pr"), key=lambda p: get_state(w, p)["threat"])
        self.assertEqual(top, "wa")

    def test_g05_taunt_overrides_three_ticks(self):
        w = rw()
        for _ in range(5): cast(w, "ma", "attack")
        cast(w, "wa", "taunt")
        for _ in range(3):
            tick(w)
            self.assertEqual(get_state(w, "wa")["boss"]["target"], "wa")

    def test_g06_dead_no_act_no_threat(self):
        w = rw()
        w.players["ma"].hp = 0; w.players["ma"].alive = False
        self.assertFalse(cast(w, "ma", "attack").get("ok"))

    def test_g07_resurrect_once_only(self):
        w = rw()
        w.players["ma"].alive = False; w.players["ma"].hp = 0
        w.players["pr"].mana = 100
        r1 = cast(w, "pr", "resurrect", "ma")
        w.players["ma"].alive = False; w.players["ma"].hp = 0
        w.players["pr"].mana = 100
        r2 = cast(w, "pr", "resurrect", "ma")
        self.assertTrue(r1.get("ok"))
        self.assertFalse(r2.get("ok"), "resurrected twice in one encounter")

    def test_g08_loot_idempotent(self):
        w = rw(); kill(w)
        claim_loot(w, "ma", "d2", "k1"); claim_loot(w, "ma", "d2", "k1")
        self.assertEqual(get_inventory(w, "ma").count("herb"), 1)

    def test_g09_unique_single_winner(self):
        w = rw(); kill(w)
        for p in ("wa", "ma", "pr"): claim_loot(w, p, "d1", f"k{p}")
        holders = sum("tore_crown" in get_inventory(w, p)
                      for p in ("wa", "ma", "pr"))
        self.assertEqual(holders, 1)

    def test_g10_gold_pile_once(self):
        w = rw(); kill(w)
        g0 = get_gold(w, "wa")
        claim_loot(w, "wa", "d3", "k"); claim_loot(w, "wa", "d3", "k")
        self.assertEqual(get_gold(w, "wa"), g0 + 50)

    def test_g11_trade_atomic_replay(self):
        w = rw(); kill(w); claim_loot(w, "wa", "d2", "k")
        t = offer_trade(w, "wa", "ma", ["herb"], [])
        confirm_trade(w, "wa", t["trade_id"])
        # escrow OR lock-in-place both fine; what must hold: receiver does
        # not have it yet, and it exists at most once across inventories
        self.assertNotIn("herb", get_inventory(w, "ma"))
        total = get_inventory(w, "wa").count("herb") + \
                get_inventory(w, "ma").count("herb")
        self.assertLessEqual(total, 1)
        confirm_trade(w, "ma", t["trade_id"])
        confirm_trade(w, "ma", t["trade_id"])
        self.assertEqual(get_inventory(w, "ma").count("herb"), 1)

    def test_g12_pending_trade_locks_item(self):
        # an item committed to a pending trade cannot be double-spent via
        # the auction house before the trade resolves
        w = rw(); kill(w); claim_loot(w, "wa", "d2", "k")
        t = offer_trade(w, "wa", "ma", ["herb"], [])
        try:
            auction_list(w, "wa", "herb", 1, 10)
        except Exception:
            pass
        confirm_trade(w, "wa", t["trade_id"])
        confirm_trade(w, "ma", t["trade_id"])
        # herb must have reached ma exactly once and not also be in an auction
        self.assertEqual(get_inventory(w, "ma").count("herb"), 1,
                         "item double-spent across trade + auction")

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
        self.assertEqual(total_gold(w, pids), g0, "gold not conserved")

    def test_g14_reconnect_preserves_all(self):
        w = rw(); kill(w); claim_loot(w, "pr", "d2", "k")
        for _ in range(5): cast(w, "pr", "attack")
        th = get_state(w, "pr")["threat"]
        disconnect(w, "pr"); tick(w); reconnect(w, "pr"); reconnect(w, "pr")
        st = get_state(w, "pr")
        self.assertIn("herb", get_inventory(w, "pr"))
        self.assertTrue(st["in_raid"])
        self.assertEqual(st["threat"], th, "threat lost on reconnect")

    def test_g15_determinism(self):
        def scripted(seed):
            w = create_world(seed)
            for pid, c in (("a", "warrior"), ("b", "mage")):
                join(w, pid, c)
            start_raid(w, ["a", "b"])
            cast(w, "a", "shout"); cast(w, "b", "amplify")
            cast(w, "b", "attack")
            for _ in range(5): tick(w)
            return serialize(w)
        self.assertEqual(scripted(3), scripted(3))

    def test_g16_wipe_ends_encounter(self):
        w = rw()
        for p in ("wa", "ma", "pr"):
            w.players[p].hp = 0; w.players[p].alive = False
        r = cast(w, "wa", "attack")
        self.assertFalse(r.get("ok"), "acted after wipe")

    def test_g17_phases_exclusive(self):
        w = rw()
        bad = False
        for _ in range(130):
            tick(w)
            b = get_state(w, "wa")["boss"]
            if b is None: continue
            s = [not b["phase_two"] and not b["enraged"],
                 b["phase_two"] and not b["enraged"], b["enraged"]]
            if sum(bool(x) for x in s) != 1: bad = True
        self.assertFalse(bad)


if __name__ == "__main__":
    unittest.main()
