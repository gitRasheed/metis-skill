# Controller-agentic v2 — authoritative spec (2026-07-19)

Adjudicated from the xhigh critique (scratchpad/critique-ctl-ag/final.md).
TASK-v2.md and verifier v2 must implement exactly this; numbers are pinned
here so the verifier stops inheriting starter internals.

## TASK-v2.md additions (append to the existing behaviors; do not weaken)

1. Temperature bands, explicit boundaries: full current 10.0-45.0 C
   INCLUSIVE; half current [0.0, 10.0) and (45.0, 50.0]; zero current
   below 0.0 or above 50.0.
2. Fault sources, pinned: pack voltage above 8600 mV is a hard
   OVERVOLTAGE fault; measured current above 2500 mA is a hard
   OVERCURRENT fault. Hard faults latch, drive the output safe in the
   same tick, persist across reset/brownout, and are cleared only by an
   external service tool (firmware never self-clears).
3. Quantified vague terms: windup recovery = after plant release,
   average current within +/-10% of target within 5 s, peak no more than
   130% of target. Bumpless = no PWM step larger than 260 counts at a
   mode transition. No chattering = under +/-25 mV measurement noise
   near a threshold, no more than 8 PWM swings larger than 150 counts
   per 10 s window, while still tracking (nonzero current on both sides).
   Steady-state = converges to WITHIN 50 mA inclusive, from above and
   below.
4. Precharge: configured 200 mA; acceptance band 100-300 mA average
   while in precharge (an open-loop duty that dumps amps into a deep
   pack is a failure).
5. Tick rate pinned: keep the shipped Timer1 configuration
   (16 MHz / 64 / OCR 312 = 798.72 Hz). State it; make the gains correct
   for it. The acceptance plant advances 1/798.72 s per tick. (Retiming
   is out of scope for v2 — it would decouple every fixed-duration
   fixture.)

## Verifier v2 test changes (vs v1 at verifier/test_acceptance.py)

- B01: room temp full current — require the AVERAGE of the last 2000
  ticks within [1900, 2100] mA (not one momentary >1700 sample).
- B02: precharge — average current in [100, 300] mA while pack < 5 V
  (kills the 28.7 A open-loop pass), duty > 0 retained.
- B03: windup — keep the existing stall/release shape; bounds now come
  from the spec (130% peak, 10% settle within 5 s = 3994 ticks).
- B04: chatter — force repeated threshold crossings with +/-25 mV noise;
  bound big-swing count per spec AND require tracking on both sides
  (mean current nonzero below and above); a sensor-blind constant duty
  must fail.
- B05: CC->CV — start the pack at 8390 mV so a correct 2 A charger
  genuinely crosses 8400 mV within 3000 ticks; detect the actual
  crossing tick and bound the PWM step around it by 260 counts.
- B06: steady-state — assertLessEqual(abs(avg - target), 50), both
  approach directions.
- B07: reset requalification — install the hot (55 C) or absent sensor
  value BEFORE charger_init(); assert PWM is 0 from tick zero onward;
  separate case for pack-absent.
- B08: fault persistence — model brownout honestly: capture the EEPROM
  bytes through the shim, load a FRESH library copy, restore the bytes,
  charger_init(); a RAM-only latch must fail; assert output stays safe.
- B09: EEPROM economy — warm into stable CC, then over a 48000-tick
  plateau require ZERO additional writes; allow <= 8 writes total across
  all phase transitions in the scenario.
- B10: thermal in every phase — assert active charging in the 500 ticks
  immediately before heat injection, then excursions tested separately
  in precharge, CC, and CV; plant gains a small leakage load so CV can
  sustain positive current.
- B11: overcurrent + overvoltage same-tick safety at the pinned
  thresholds (8600 mV / 2500 mA), fault latched, output safe same tick.
- NEW B12: band boundaries — 10.0 C and 45.0 C give FULL current;
  5.0 C and 47.0 C give half (within +/-100 mA of 1000).
- Mechanics kept from v1: per-test fresh .so copy, seeded RNG,
  simulated time only; tempfile libraries must be unlinked after load.

## Validation gates before v2 is formal

1. Starter fails all-but-<=2 gates.
2. A golden reference implementation (built separately) passes 12/12.
3. Every fixture number recomputed by hand against the plant equations
   (the /10 lesson).
