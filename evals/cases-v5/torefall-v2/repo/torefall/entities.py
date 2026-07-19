import random


class Entity:
    def __init__(self, eid):
        self.eid = eid
        self.hp = 100
        self.alive = True

    def take_damage(self, amount):
        self.hp -= amount
        if self.hp <= 0:
            self.alive = False


class Character(Entity):
    def __init__(self, eid, name):
        super().__init__(eid)
        self.name = name
        self.level = 1
        self.inventory = []
        self.xp = 0


class Player(Character):
    threat_mult = 1.0

    def __init__(self, eid, name):
        super().__init__(eid, name)
        self.in_raid = False
        self.shout_ticks = 0
        self.amplified = False
        self.blessed = False
        self.connected = True
        self.mana = 100
        self.gold = 100
        self.sick_ticks = 0
        self.was_resurrected = False


class Warrior(Player):
    base_damage = 30
    threat_mult = 1.5

    def attack_damage(self):
        dmg = self.base_damage
        if self.shout_ticks > 0:
            dmg = dmg * 110 // 100
        if self.blessed:
            dmg = dmg + 5
        return dmg

    def shout(self, party):
        for p in party:
            p.shout_ticks = 3


class Mage(Player):
    base_damage = 40

    def attack_damage(self):
        dmg = self.base_damage
        if self.amplified:
            dmg = dmg * 120 // 100
            self.amplified = False
        if self.shout_ticks > 0:
            dmg = dmg * 110 // 100
        if self.blessed:
            dmg = dmg + 5
        return dmg

    def amplify(self):
        self.amplified = True


class Priest(Player):
    base_damage = 20
    threat_mult = 0.8

    def attack_damage(self):
        dmg = self.base_damage
        if self.shout_ticks > 0:
            dmg = dmg * 110 // 100
        return dmg

    def bless(self, target):
        target.blessed = True


CLASSES = {"warrior": Warrior, "mage": Mage, "priest": Priest}
