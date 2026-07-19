# Follow-up: extend your Torefall implementation

Add the Necromancer class: ability "drain" (base 25 damage to the boss,
heals the caster for half the damage dealt, integer floor); Priest bless
adds +5 to drain's HEAL (not its damage). Add a boss phase-three
mechanic: below 25% HP the boss summons one add per living raider every
10 ticks (adds have 40 HP, deal 3 damage each tick to a random raider
via the seeded RNG). All stage-A behaviors must still hold. Keep the
same contract; extend the code you wrote.

Also: the Necromancer drain must integrate with threat (mult 1.2), mana (cost 18), and res sickness; and add one auction-house rule: a 5% (floor) seller fee on buyout, burned (removed from the economy) — conservation invariants must account for it.
