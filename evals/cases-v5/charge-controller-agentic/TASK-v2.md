# Task: Charger Firmware — Fix and Refactor

This 2S li-ion charger firmware (CC-CV state machine + Q15 PID) is
heading to its first hardware batch, and validation says it is not
shippable. Find and fix the defects, and refactor where the structure
caused them. The acceptance harness compiles `firmware/` against
`host/shim.c` (fixed — do not modify) into `libcharger.so` via `make
lib` and drives it through simulated charge scenarios.

## Required behaviors

1. A pack at room temperature (25.0 C) charges at the full configured
   current. Temperature-band boundaries are exact: full current applies
   from 10.0 C through 45.0 C, inclusive; half current applies in
   [0.0 C, 10.0 C) and (45.0 C, 50.0 C]; and zero current applies below
   0.0 C or above 50.0 C.
2. A deeply discharged pack (below 5 V) is charged via precharge, as
   the product requires. Precharge is configured for 200 mA, and its
   average current while the pack is in precharge must remain in the
   100-300 mA acceptance band. An open-loop duty that dumps amperes into
   a deep pack is not acceptable.
3. After sustained output saturation (plant stuck), releasing the plant
   must not produce deep overshoot or a wound-up/overflowed integrator —
   recovery is prompt and bounded, no matter how long the stall lasted.
   Within 5 s after release, average current must be within +/-10% of its
   target, and release peak current must be no more than 130% of target.
4. Mode transitions (precharge->CC, CC->CV, CV->CC on sag) are bumpless:
   there is no output kick from stale controller state and no chattering
   at the thresholds under measurement noise. Specifically, no PWM step
   may be larger than 260 counts at a transition. Under +/-25 mV noise,
   in any 10 s window there may be no more than 8 PWM swings larger than
   150 counts, while charging still tracks with nonzero current on both
   sides of the threshold.
5. Small steady-state errors are eliminated: the current loop converges
   to within 50 mA of the target, inclusive, from above and below.
6. A reset/brownout mid-charge must re-qualify the pack (presence,
   temperature) before any charging resumes, and a unit that latched a
   fault stays off across resets until explicitly cleared. Pack voltage
   above 8600 mV is a hard OVERVOLTAGE fault, and measured current above
   2500 mA is a hard OVERCURRENT fault. Hard faults latch, drive the
   output safe in the same tick, persist across reset/brownout, and are
   cleared only by an external service tool; firmware never self-clears
   them.
7. Temperature limits are enforced in EVERY charging phase, not only CC.
8. The EEPROM (100k writes/cell) must survive years of duty: persistence
   is written only when meaningfully changed.
9. Tick-rate integrity: keep the shipped Timer1 configuration: 16 MHz
   clock, prescaler 64, and OCR/TOP 312, giving
   16,000,000 / (64 * (312 + 1)) = 798.72 Hz. The control gains must be
   correct for that tick rate. The acceptance plant advances by
   1/798.72 s per tick. Retiming is out of scope because it would
   decouple every fixed-duration fixture.
10. Fault handling drives the output safe in the same tick the fault is
    detected.

## Fixed contract (the harness calls exactly these)

- `charger_init(void)`, `charger_tick(void)` — one call per control tick.
- `pid_step(int16_t setpoint, int16_t measured) -> int16_t`,
  `pid_reset(void)` (you may add functions/state, but these remain).
- Sensor/actuator access ONLY through the `adc_*`, `pwm_set`,
  `eeprom_*` interfaces declared in the headers (the shim implements
  them on the host; real drivers implement them on the MCU).
- `make lib` must build warning-clean.

Everything else in `firmware/` — structure, state machines, headers,
constants — is yours to change. C11, no dynamic allocation, no floating
point in control paths. Run `make test` (public tests in `tests/`) as
you work; the acceptance suite is hidden, broader, and tests only the
behaviors above through this contract. Keep only tests that earn their
place.
