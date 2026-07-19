# V2 acceptance fixture derivations

## Where the equations come from

`repo/host/shim.c` does not itself simulate a battery: it stores the exact
pack-voltage, current, and temperature values supplied by the harness and
returns the firmware's PWM value. It performs no scaling. The RC plant is
the `Plant` model inherited from `verifier_v1.py`; v2 preserves its equations
and adds the specified leakage term only for the sustained-CV thermal case.
That distinction is important when checking the arithmetic against the fixed
shim contract.

For duty `d`, actual pack voltage `V` in mV, charger current `I` in mA, and
v2 leakage load `L` in mA, the equations used by the suite are:

```text
drive_mv = (d / 1023) * 9000
I        = max(0, (drive_mv - V) / 0.12)
delta_V  = (I - L) * 4e-5 / 10
         = (I - L) * 0.000004 mV per tick
V_next   = clamp(V + delta_V, 0, 8450)
```

The `/10` is applied explicitly. Thus 200 mA changes the pack by only
`200 * 0.000004 = 0.0008 mV/tick`, and 2000 mA changes it by
`2000 * 0.000004 = 0.008 mV/tick`. In the stall fixture the inherited
special branch reports 150 mA when duty is positive and holds voltage fixed;
it does not run the normal pack-update equation.

The inverse rail equation is useful for sanity-checking nonzero operating
points:

```text
d = (V + 0.12 * I) * 1023 / 9000
```

Examples, without rounding them into acceptance criteria:

- 200 mA at 4200 mV needs
  `(4200 + 0.12*200) * 1023 / 9000`
  `= 4224 * 1023 / 9000 = 480.128` duty counts.
- 2000 mA at 7400 mV needs
  `(7400 + 0.12*2000) * 1023 / 9000`
  `= 7640 * 1023 / 9000 = 868.413` counts.
- 2000 mA at 8390 mV needs
  `(8390 + 240) * 1023 / 9000 = 980.943` counts; at 8400 mV it
  needs `(8400 + 240) * 1023 / 9000 = 982.080` counts.
- Sustaining the 300 mA leakage load at 8400 mV needs
  `(8400 + 0.12*300) * 1023 / 9000`
  `= 8436 * 1023 / 9000 = 958.892` counts, so the CV fixture can
  require positive current without requiring full CC current.

## Tick arithmetic

Timer1's counter visits `TOP + 1 = 312 + 1 = 313` counts per tick:

```text
16,000,000 / (64 * 313)
= 16,000,000 / 20,032
= 798.722... Hz
```

The authoritative fixture rate is the pinned rounded value 798.72 Hz, so one
tick represents `1 / 798.72 = 0.001252... s`. The duration conversions used
in the suite are:

| Ticks | Arithmetic | Simulated duration |
|---:|---:|---:|
| 100 | `100 / 798.72` | 0.1252 s |
| 200 | `200 / 798.72` | 0.2504 s |
| 500 | `500 / 798.72` | 0.6260 s |
| 1000 | `1000 / 798.72` | 1.2520 s |
| 1500 | `1500 / 798.72` | 1.8780 s |
| 2000 | `2000 / 798.72` | 2.5040 s |
| 3000 | `3000 / 798.72` | 3.7560 s |
| 4000 | `4000 / 798.72` | 5.0080 s |
| 6000 | `6000 / 798.72` | 7.5120 s |
| 8000 | `8000 / 798.72` | 10.0160 s |
| 12000 | `12000 / 798.72` | 15.0240 s |
| 48000 | `48000 / 798.72` | 60.0962 s |

Two windows are calculated directly from requirements rather than chosen:

- Five-second windup deadline: `ceil(5 * 798.72) = ceil(3993.6) = 3994`
  ticks, which is `3994 / 798.72 = 5.0005 s`.
- Ten-second chatter window: `ceil(10 * 798.72) = ceil(7987.2) = 7988`
  ticks, which is `7988 / 798.72 = 10.0010 s`.

## Per-gate arithmetic

### B01 — room-temperature full current

The fixture starts at 7400 mV and 25.0 C (`250` deci-Celsius), then runs
6000 ticks. At ideal 2000 mA the voltage rise is
`6000 * 2000 * 0.000004 = 48 mV`, ending near 7448 mV and therefore well
below the 8400 mV CC/CV boundary. The last 2000 ticks span about 2.504 s.
The `[1900, 2100] mA` average band is the v2-pinned `2000 +/- 5%` band,
not a value inferred from the plant.

### B02 — bounded precharge

The start voltage is 4200 mV, 800 mV below 5 V. At the configured 200 mA,
3000 ticks add
`3000 * 200 * 0.000004 = 2.4 mV`, so the final voltage is about
4202.4 mV and every sample remains in precharge. Reaching 5000 mV at that
current would take
`(5000 - 4200) / 0.0008 = 1,000,000 ticks`, far longer than this fixture.
The 100-300 mA limits equal `0.5 * 200` and `1.5 * 200`; those factors and
the requirement for positive duty are pinned by v2.

### B03 — windup release

The 8000-tick stall lasts `8000 / 798.72 = 10.016 s`, holds the pack at
7400 mV, and returns the inherited non-open-circuit trickle of 150 mA for
positive duty. Release runs for the calculated 3994-tick five-second
deadline. For the 2000 mA target:

```text
maximum peak = 1.30 * 2000 = 2600 mA
settled band = [0.90 * 2000, 1.10 * 2000]
             = [1800, 2200] mA
```

The final 500 samples form a 0.626 s average at the end of that deadline.
Even the allowed 2600 mA peak changes pack voltage by only
`2600 * 0.000004 = 0.0104 mV` in one tick.

### B04 — noisy CC/CV threshold

Warm-up starts at 8390 mV for 500 ticks. At ideal 2 A its largest expected
rise is `500 * 0.008 = 4 mV`, to about 8394 mV, still below 8400 mV. The
last 200 warm-up samples cover 0.2504 s and establish that the charger is
active.

For the calculated 7988-tick window, the external fixture holds the actual
pack at 8400 mV. Seed `7` selects endpoints from the pinned +/-25 mV noise,
so the shim repeatedly receives `8400 - 25 = 8375 mV` and
`8400 + 25 = 8425 mV`. Holding the physical voltage makes a sensor-blind
constant duty produce the same current in both groups; v2 additionally
requires a directional response (below-threshold mean greater than
above-threshold mean), while both means must be positive. The limits of no
more than 8 adjacent PWM changes strictly larger than 150 counts are copied
directly from the v2 contract.

### B05 — real CC to CV crossing

Starting 10 mV below the 8400 mV boundary at 8390 mV, ideal 2 A CC reaches
the boundary in

```text
(8400 - 8390) / (2000 * 0.000004)
= 10 / 0.008
= 1250 ticks.
```

The 3000-tick run provides 1750 ticks of margin after that nominal crossing;
uninterrupted 2 A would add `3000 * 0.008 = 24 mV`. The suite detects the
actual floating-point plant crossing rather than assuming tick 1250. It
checks adjacent duty steps from three samples before through four samples
after the detected tick, covering the tick that first exposes the integer
8400 mV sensor value and a one-tick state-machine handoff. The maximum
permitted step, 260 counts, is pinned by v2.

### B06 — steady state from both directions

Each side starts at 7400 mV and warms for 4000 ticks (5.008 s). Ideal CC adds
`4000 * 0.008 = 32 mV`. Current is then perturbed to 1600 mA (400 mA below
target) or 2400 mA (400 mA above target); 2400 remains 100 mA below the
strictly-above-2500-mA fault threshold. The 12000-tick convergence run lasts
15.024 s and at ideal current adds another 96 mV. Even the combined ideal
128 mV rise leaves the pack near 7528 mV, safely in CC. The final 1000
samples cover 1.252 s. `abs(average - 2000) <= 50 mA` uses the inclusive
v2 boundary exactly.

### B07 — reset requalification

Both cases first charge from 7400 mV for 2000 ticks; ideal CC adds 16 mV and
the last 500 ticks establish 0.626 s of immediately preceding activity.
Before `charger_init`, the hot case installs `550` deci-Celsius = 55.0 C,
which is 5.0 C above the zero-current boundary. The absent-pack case installs
0 mV while retaining a valid 25.0 C temperature. All 1500 post-reset ticks
(1.878 s) are checked from the first tick, with required maximum duty 0.

### B08 — EEPROM-only brownout persistence

After the same 2000-tick active setup, 8601 mV is injected because the fault
condition is strictly greater than 8600 mV; 8600 itself is not a fault.
Current is 0 mA and temperature is 25.0 C so overvoltage is the only injected
fault source. `host/shim.c` exposes the complete simulated EEPROM as one
`uint8_t` phase byte, so that byte—and no RAM—is copied into a new library
image. A safe 7400 mV/25.0 C plant is then run for 1500 ticks, all of which
must retain duty 0.

### B09 — EEPROM economy

Warm-up runs 12000 ticks. Ideal 2 A raises 7400 mV by
`12000 * 0.008 = 96 mV`, to about 7496 mV; the last 2000 ticks use B01's
1900-2100 mA stable-CC band. The plateau then runs 48000 ticks = 60.0962 s.
At ideal CC it adds `48000 * 0.008 = 384 mV`, so the whole 60000-tick
scenario ends near `7400 + 96 + 384 = 7880 mV`, never reaching CV. Therefore
the stable plateau has zero legitimate phase changes and must add zero
writes. The total transition budget of 8 writes is pinned by v2 rather than
derived from the RC model.

### B10 — thermal limit in three phases

Every case evaluates the 500 ticks (0.626 s) immediately before heat is
injected, then installs `520` deci-Celsius = 52.0 C, which is 2.0 C above the
zero-current boundary, for 2000 ticks (2.504 s). A pre-heat mean of at least
100 mA uses the lowest already-accepted active precharge current, so a lone
old spike cannot establish that the phase is active; every post-heat current
sample must be exactly zero.

- Precharge starts at 4200 mV and runs 3000 ticks. At 200 mA it rises only
  2.4 mV, as calculated for B02, and remains below 5 V.
- CC starts at 7400 mV and runs 6000 ticks. At 2 A it rises 48 mV to about
  7448 mV, between the 6000 mV precharge exit and 8400 mV CV threshold.
- CV starts at 8390 mV with a 300 mA leakage load. While CC supplies 2 A,
  net pack current is `2000 - 300 = 1700 mA`, giving
  `1700 * 0.000004 = 0.0068 mV/tick`. The nominal crossing therefore takes
  `10 / 0.0068 = 1470.59`, or 1471 ticks, well inside the 6000-tick warm-up.
  At equilibrium the charger must supply the positive 300 mA leakage current.
  If hot shutdown supplies 0 mA, 2000 ticks of leakage reduce voltage by
  `2000 * 300 * 0.000004 = 2.4 mV`, keeping the pack near the CV boundary.

### B11 — independent same-tick hard faults

Each source gets a fresh library and the 2000-tick active setup from B07.
The overvoltage input is `8600 + 1 = 8601 mV`; the overcurrent input is
`2500 + 1 = 2501 mA`. Adding one is the smallest integer stimulus satisfying
the contract's strict “above” comparisons. The other source is kept safe
(0 mA for overvoltage, 7400 mV for overcurrent), so either case cannot pass
by detecting only the other fault. After the detection-tick duty check, 100
safe ticks (0.1252 s) verify that the in-RAM latch does not self-clear.

### B12 — exact temperature boundaries

The shim and firmware temperature unit is deci-Celsius, so the four values
are `10.0*10 = 100`, `45.0*10 = 450`, `5.0*10 = 50`, and
`47.0*10 = 470`. Each starts at 7400 mV and runs 6000 ticks. Full current
would add 48 mV; half current (1000 mA) adds
`6000 * 1000 * 0.000004 = 24 mV`. Both remain in CC. The final 2000-sample
averages must be 1900-2100 mA at the inclusive 10.0/45.0 C boundaries and
`1000 +/- 100 = 900-1100 mA` in the 5.0/47.0 C half-current bands.
