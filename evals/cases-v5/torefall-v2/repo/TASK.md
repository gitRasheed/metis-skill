# Task: Torefall Shard v2 — Fix and Re-architect

You own this MMO shard server core. The happy path runs; production
would be a catastrophe: duped loot and gold, half-executed trades,
threat/aggro that ignores the rules, free resurrections, an auction
house that mints items, reconnects that wipe identity. Fix the
behaviors below and restructure so the NEXT class, boss mechanic, and
economy feature are cheap to add — a follow-up task will extend your
code and the size of that change is measured.

## Systems and exact rules (fixtures are hand-computed; integer floor
## at every step)

- Buff stacking: ability-specific modifiers first, then party auras,
  then res-sickness halving LAST. Amplified Mage bolt under Warrior
  shout = 40 -> 48 -> 52; the same attack under res sickness = 26.
- Mana: costs shout 10, taunt 5, amplify 15, bolt 10, bless 20,
  heal 12, resurrect 50; regen +2 per tick (cap 100). A cast with
  insufficient mana FAILS CLEANLY: no effect, no cost, explicit error.
- Threat: attack adds floor(damage x class mult) — warrior 1.5,
  mage 1.0, priest 0.8; heal adds floor(healed/2). Boss targets the
  highest-threat living CONNECTED raider each tick (stable tie-break by
  join order). Taunt: forces target to the warrior for exactly 3 ticks
  AND sets warrior threat to current-max + 10. Threat of a dead or
  disconnected player is ignored while they are down but not erased.
- Death and resurrection: hp <= 0 means dead — a dead player cannot
  act, generates no threat, cannot be boss target. Priest resurrect:
  costs 50 mana, only on a dead raider, AT MOST ONCE per target per
  encounter; returns them at 50 hp with 10 ticks of res sickness
  (outgoing damage halved, applied last). All raiders dead = wipe: the
  encounter ends, further raid casts fail cleanly.
- Loot: unique "tore_crown" to exactly ONE eligible raider — the claim
  set is classified as a batch before any grant; claim retries with the
  same request_key are idempotent; rejected claimants get an explicit
  rejection; "gold_pile" grants 50 gold exactly once per claimant.
- Trades: two-phase (both confirm), atomic, replay-safe; items in a
  pending trade are LOCKED (cannot be listed, crafted, or traded
  twice).
- Auction house with ESCROW: listing moves the item to escrow (fails if
  the item is locked); a bid escrows the bidder's gold and refunds the
  previous bidder in the same operation; buyout settles seller/bidder
  refunds atomically; expiry (50 ticks) returns escrowed item and
  refunds the standing bid. CONSERVATION INVARIANTS at every point
  between API calls: total gold across players+escrow is constant
  except explicit gold_pile grants; every item instance exists in
  exactly one place (an inventory, one escrow slot, or one pending
  trade side).
- Reconnect: preserves identity, inventory, gold, xp, mana, threat,
  raid membership, res-sickness and once-resurrected status; repeated
  reconnects idempotent; encounter continues while disconnected.
- Determinism: create_world(seed) + identical API call sequence =>
  identical serialize(w) output. No wall-clock, no unseeded RNG.
- Crafting: atomic (no partial consumption on failure).
- Raid phases: phase one / phase two / enraged are mutually exclusive
  and transition exactly once each.

## Fixed contract (harness calls exactly these, from torefall.world)

create_world, join, disconnect, reconnect, start_raid, cast(w,pid,
ability,target=None) for shout/taunt/amplify/bless/heal/resurrect/
attack, tick, claim_loot(w,pid,drop_id,request_key), offer_trade,
confirm_trade, auction_list(w,pid,item,min_bid,buyout),
auction_bid(w,pid,auction_id,amount), auction_buyout(w,pid,auction_id),
craft, get_inventory, get_gold, get_state, serialize. Keep existing
return keys (add freely). get_state must expose hp/mana/alive/
sick_ticks/in_raid/connected/threat and boss {hp, phase flags, alive,
target}. Python stdlib only. Restructure everything else freely. Public
tests in tests/; the hidden acceptance suite is broader but tests only
these rules through this contract.
