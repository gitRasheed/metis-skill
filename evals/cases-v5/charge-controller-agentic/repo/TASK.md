# Task: Charger Firmware — Fix and Refactor

This 2S li-ion charger firmware (CC-CV state machine + Q15 PID) is
heading to its first hardware batch, and validation says it is not
shippable. Find and fix the defects, and refactor where the structure
caused them. The acceptance harness compiles `firmware/` against
`host/shim.c` (fixed — do not modify) into `libcharger.so` via `make
lib` and drives it through simulated charge scenarios.

## Required behaviors

1. A pack at room temperature (25.0 C) charges at the full configured
   current; derating engages only in the configured bands (full current
   10-45 C, half 0-10 and 45-50, zero outside).
2. A deeply discharged pack (below 5 V) is charged via precharge, as
   the product requires.
3. After sustained output saturation (plant stuck), releasing the plant
   must not produce deep overshoot or a wound-up/overflowed integrator —
   recovery is prompt and bounded, no matter how long the stall lasted.
4. Mode transitions (precharge->CC, CC->CV, CV->CC on sag) are bumpless:
   no output kick from stale controller state, no chattering at the
   thresholds under measurement noise.
5. Small steady-state errors are eliminated: the current loop converges
   to within 50 mA of the target, from above and below.
6. A reset/brownout mid-charge must re-qualify the pack (presence,
   temperature) before any charging resumes, and a unit that latched a
   fault stays off across resets until explicitly cleared.
7. Temperature limits are enforced in EVERY charging phase, not only CC.
8. The EEPROM (100k writes/cell) must survive years of duty: persistence
   is written only when meaningfully changed.
9. Tick-rate integrity: the control constants and the timer
   configuration must agree — state the actual tick rate and make the
   gains correct for it.
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
