class RaidBoss:
    def __init__(self):
        self.hp = 5000
        self.max_hp = 5000
        self.phase_two = False
        self.enraged = False
        self.ticks = 0
        self.alive = True
        self.threat = {}
        self.taunt_target = None
        self.taunt_ticks = 0

    def tick(self, party):
        self.ticks += 1
        if self.hp < self.max_hp // 2:
            self.phase_two = True
        if self.ticks > 100:
            self.enraged = True
        dmg = 8
        if self.phase_two:
            dmg = 14
        if self.enraged:
            dmg = dmg * 3
        if self.taunt_ticks > 0:
            self.taunt_ticks -= 1
            target = self.taunt_target
        else:
            target = None
            best = -1
            for p in party:
                t = self.threat.get(p.name, 0)
                if t > best:
                    best = t
                    target = p
        if target is not None:
            target.take_damage(dmg)

    def take_damage(self, amount):
        self.hp -= amount
        if self.hp <= 0:
            self.alive = False
