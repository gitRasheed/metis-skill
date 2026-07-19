import random

from torefall.boss import RaidBoss
from torefall.entities import CLASSES, Mage, Priest, Warrior

RECIPES = {
    "healing_draught": {"needs": {"herb": 2, "vial": 1}, "xp": 10},
    "iron_blade": {"needs": {"ore": 3}, "xp": 25},
}
UNIQUE_ITEMS = {"tore_crown"}
MANA_COSTS = {"amplify": 15, "bolt": 10, "bless": 20, "heal": 12,
              "resurrect": 50, "shout": 10, "taunt": 5}
AUCTION_TICKS = 50


class World:
    def __init__(self, seed):
        self.rng = random.Random(seed)
        self.players = {}
        self.next_eid = 1
        self.raid = None
        self.boss = None
        self.drops = {}
        self.trades = {}
        self.next_trade = 1
        self.auctions = {}
        self.next_auction = 1
        self.ticks = 0


def create_world(seed=0):
    return World(seed)


def join(w, player_id, cls):
    p = CLASSES[cls](w.next_eid, player_id)
    w.next_eid += 1
    w.players[player_id] = p
    return {"player": player_id, "cls": cls}


def disconnect(w, player_id):
    w.players[player_id].connected = False


def reconnect(w, player_id):
    old = w.players.get(player_id)
    cls = type(old).__name__.lower() if old else "warrior"
    p = CLASSES[cls](w.next_eid, player_id)
    w.next_eid += 1
    w.players[player_id] = p
    return {"player": player_id, "rejoined": True}


def start_raid(w, party_ids):
    w.raid = list(party_ids)
    w.boss = RaidBoss()
    for pid in party_ids:
        w.players[pid].in_raid = True
    return {"raid": True, "boss_hp": w.boss.hp}


def _spend(p, ability):
    cost = MANA_COSTS.get(ability, 0)
    p.mana -= cost
    return True


def cast(w, player_id, ability, target=None):
    p = w.players[player_id]
    if not p.alive:
        return {"ok": False, "reason": "dead"}
    if ability == "shout" and isinstance(p, Warrior):
        _spend(p, ability)
        p.shout([w.players[i] for i in (w.raid or []) if i in w.players])
        return {"ok": True}
    if ability == "taunt" and isinstance(p, Warrior):
        _spend(p, ability)
        w.boss.taunt_target = p
        w.boss.taunt_ticks = 3
        best = max(w.boss.threat.values()) if w.boss.threat else 0
        w.boss.threat[p.name] = best + 10
        return {"ok": True}
    if ability == "amplify" and isinstance(p, Mage):
        _spend(p, ability)
        p.amplify()
        return {"ok": True}
    if ability == "bless" and isinstance(p, Priest):
        _spend(p, ability)
        p.bless(w.players[target])
        return {"ok": True}
    if ability == "heal" and isinstance(p, Priest):
        _spend(p, ability)
        t = w.players[target]
        healed = min(25, 100 - t.hp)
        t.hp += healed
        w.boss.threat[p.name] = w.boss.threat.get(p.name, 0) + healed // 2
        return {"ok": True, "healed": healed}
    if ability == "resurrect" and isinstance(p, Priest):
        _spend(p, ability)
        t = w.players[target]
        t.alive = True
        t.hp = 50
        t.sick_ticks = 10
        return {"ok": True}
    if ability == "attack":
        dmg = p.attack_damage()
        if p.sick_ticks > 0:
            dmg = dmg * 50 // 100
        if isinstance(p, Mage):
            _spend(p, "bolt")
        w.boss.take_damage(dmg)
        w.boss.threat[p.name] = (w.boss.threat.get(p.name, 0)
                                 + int(dmg * p.threat_mult))
        if not w.boss.alive and not w.drops:
            _roll_drops(w)
        return {"ok": True, "damage": dmg}
    return {"ok": False}


def tick(w):
    w.ticks += 1
    if w.boss and w.boss.alive and w.raid:
        party = [w.players[i] for i in w.raid if i in w.players]
        w.boss.tick(party)
        for p in party:
            if p.shout_ticks > 0:
                p.shout_ticks -= 1
            if p.sick_ticks > 0:
                p.sick_ticks -= 1
            if p.mana < 100:
                p.mana += 2
    for aid in list(w.auctions):
        a = w.auctions[aid]
        if w.ticks - a["listed_at"] > AUCTION_TICKS:
            w.players[a["seller"]].inventory.append(a["item"])
            del w.auctions[aid]
    return {"boss_hp": w.boss.hp if w.boss else None}


def _roll_drops(w):
    w.drops["d1"] = {"item": "tore_crown", "claimed_by": []}
    w.drops["d2"] = {"item": "herb", "claimed_by": []}
    w.drops["d3"] = {"item": "gold_pile", "claimed_by": []}


def claim_loot(w, player_id, drop_id, request_key):
    drop = w.drops.get(drop_id)
    if drop is None:
        return {"ok": False}
    drop["claimed_by"].append(player_id)
    if drop["item"] == "gold_pile":
        w.players[player_id].gold += 50
    else:
        w.players[player_id].inventory.append(drop["item"])
    return {"ok": True, "item": drop["item"]}


def offer_trade(w, a, b, items_a, items_b):
    tid = f"t{w.next_trade}"
    w.next_trade += 1
    w.trades[tid] = {"a": a, "b": b, "items_a": list(items_a),
                     "items_b": list(items_b)}
    return {"trade_id": tid}


def confirm_trade(w, player_id, trade_id):
    t = w.trades.get(trade_id)
    if t is None:
        return {"ok": False}
    pa, pb = w.players[t["a"]], w.players[t["b"]]
    for item in t["items_a"]:
        pa.inventory.remove(item)
        pb.inventory.append(item)
    for item in t["items_b"]:
        pb.inventory.remove(item)
        pa.inventory.append(item)
    del w.trades[trade_id]
    return {"ok": True}


def auction_list(w, player_id, item, min_bid, buyout):
    p = w.players[player_id]
    aid = f"a{w.next_auction}"
    w.next_auction += 1
    p.inventory.remove(item)
    w.auctions[aid] = {"seller": player_id, "item": item,
                       "min_bid": min_bid, "buyout": buyout,
                       "bid": 0, "bidder": None, "listed_at": w.ticks}
    return {"auction_id": aid}


def auction_bid(w, player_id, auction_id, amount):
    a = w.auctions.get(auction_id)
    p = w.players[player_id]
    if a is None or amount < a["min_bid"] or amount <= a["bid"]:
        return {"ok": False}
    p.gold -= amount
    a["bid"] = amount
    a["bidder"] = player_id
    return {"ok": True}


def auction_buyout(w, player_id, auction_id):
    a = w.auctions.get(auction_id)
    p = w.players[player_id]
    if a is None:
        return {"ok": False}
    p.gold -= a["buyout"]
    w.players[a["seller"]].gold += a["buyout"]
    p.inventory.append(a["item"])
    del w.auctions[auction_id]
    return {"ok": True}


def craft(w, player_id, recipe):
    p = w.players[player_id]
    r = RECIPES[recipe]
    for item, n in r["needs"].items():
        for _ in range(n):
            p.inventory.remove(item)
    p.inventory.append(recipe)
    p.xp += r["xp"]
    return {"ok": True, "xp": p.xp}


def get_inventory(w, player_id):
    return list(w.players[player_id].inventory)


def get_gold(w, player_id):
    return w.players[player_id].gold


def serialize(w):
    parts = []
    for pid in sorted(w.players):
        p = w.players[pid]
        parts.append((pid, p.hp, p.mana, p.gold, p.alive, p.sick_ticks,
                      tuple(sorted(p.inventory))))
    return repr(parts)


def get_state(w, player_id):
    p = w.players[player_id]
    b = w.boss
    return {"hp": p.hp, "mana": p.mana, "alive": p.alive,
            "sick_ticks": p.sick_ticks, "in_raid": p.in_raid,
            "connected": p.connected,
            "threat": (b.threat.get(p.name, 0) if b else 0),
            "boss": None if b is None else
            {"hp": b.hp, "phase_two": b.phase_two, "enraged": b.enraged,
             "alive": b.alive,
             "target": (b.taunt_target.name if b.taunt_ticks > 0 and
                        b.taunt_target else None)}}
