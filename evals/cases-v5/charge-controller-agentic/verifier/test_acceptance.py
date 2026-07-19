"""Hidden acceptance suite: drives the compiled firmware through a
simulated RC pack via the fixed shim contract. Behavior-only — no
dependence on internal state or phase encodings."""
import ctypes
import pathlib
import random
import shutil
import tempfile
import unittest

ROOT = pathlib.Path(__file__).parent


def load():
    # fresh .so copy per test: firmware statics must start virgin each
    # scenario (a fault latch that rightly survives charger_init must not
    # leak between unrelated tests)
    tmp = tempfile.NamedTemporaryFile(suffix=".so", delete=False)
    tmp.close()
    shutil.copy(str(ROOT / "libcharger.so"), tmp.name)
    lib = ctypes.CDLL(tmp.name)
    lib.pid_step.restype = ctypes.c_int16
    lib.shim_get_pwm.restype = ctypes.c_int16
    lib.shim_get_eeprom_writes.restype = ctypes.c_uint32
    return lib


class Plant:
    """RC pack: duty drives current through 0.12 ohm off a 9 V rail."""

    def __init__(self, lib, pack_mv=7400.0, temp_dc=250, stuck=False):
        self.lib, self.pack_mv, self.temp_dc = lib, pack_mv, temp_dc
        self.stuck = stuck
        self.curr_ma = 0.0
        self.noise = 0.0
        self.rng = random.Random(7)

    def tick(self):
        n = self.rng.uniform(-self.noise, self.noise)
        self.lib.shim_set_pack_mv(int(self.pack_mv + n))
        self.lib.shim_set_curr_ma(int(self.curr_ma))
        self.lib.shim_set_temp_dc(self.temp_dc)
        self.lib.charger_tick()
        duty = self.lib.shim_get_pwm()
        if self.stuck:
            # saturated but not open-circuit: a trickle flows, so
            # latching an open-load fault here would be wrong
            self.curr_ma = 150.0 if duty > 0 else 0.0
        else:
            drive_mv = duty / 1023.0 * 9000.0
            self.curr_ma = max(0.0, (drive_mv - self.pack_mv) / 0.12)
            self.pack_mv = min(8450.0, self.pack_mv + self.curr_ma * 4e-5 / 10.0)
        return duty, self.curr_ma


def run(plant, ticks):
    trace = [plant.tick() for _ in range(ticks)]
    return trace


class Acceptance(unittest.TestCase):
    def setUp(self):
        self.lib = load()
        self.lib.shim_set_eeprom_phase(0)
        self.lib.pid_reset()

    def start(self, **kw):
        p = Plant(self.lib, **kw)
        self.lib.shim_set_pack_mv(int(p.pack_mv))
        self.lib.shim_set_curr_ma(0)
        self.lib.shim_set_temp_dc(p.temp_dc)
        self.lib.charger_init()
        return p

    def test_b01_room_temp_charges_full_current(self):
        p = self.start(temp_dc=250)
        trace = run(p, 6000)
        peak = max(c for _, c in trace)
        self.assertGreaterEqual(peak, 1700, "no full-current charge at 25C")

    def test_b02_deeply_discharged_pack_charges(self):
        p = self.start(pack_mv=4200.0, temp_dc=250)
        trace = run(p, 3000)
        self.assertGreater(max(d for d, _ in trace), 0,
                           "sub-5V pack never charged")

    def test_b03_saturation_recovery_bounded(self):
        p = self.start(temp_dc=250, stuck=True)
        run(p, 8000)                      # long stall, output saturated
        p.stuck = False
        trace = run(p, 4000)
        peak = max(c for _, c in trace)
        self.assertLess(peak, 2600, "windup overshoot after stall")
        tail = [c for _, c in trace[-500:]]
        self.assertGreater(sum(tail) / len(tail), 800,
                           "loop dead after long stall (integrator wrap)")

    def test_b04_no_threshold_chatter(self):
        p = self.start(pack_mv=8390.0, temp_dc=250)
        p.noise = 25.0
        trace = run(p, 4000)
        self.assertGreater(max(c for _, c in trace), 300,
                           'charger never charged (vacuous scenario)')
        duties = [d for d, _ in trace[500:]]
        swings = sum(1 for a, b in zip(duties, duties[1:])
                     if abs(a - b) > 150)
        self.assertLess(swings, 8, "CC/CV chatter under noise")

    def test_b05_cc_cv_transition_bumpless(self):
        p = self.start(pack_mv=8300.0, temp_dc=250)
        trace = run(p, 6000)
        self.assertGreater(max(c for _, c in trace), 300,
                           'charger never charged (vacuous scenario)')
        duties = [d for d, _ in trace]
        kick = max(abs(a - b) for a, b in zip(duties[200:], duties[201:]))
        self.assertLess(kick, 260, "output kick at mode transition")

    def test_b06_steady_state_error_removed(self):
        p = self.start(temp_dc=250)
        trace = run(p, 12000)
        tail = [c for _, c in trace[-1000:]]
        avg = sum(tail) / len(tail)
        self.assertLess(abs(2000 - avg), 50,
                        f"steady-state error {2000 - avg:.0f} mA persists")

    def test_b07_reset_requalifies_temperature(self):
        p = self.start(temp_dc=250)
        pre = run(p, 2000)                 # charging
        self.assertGreater(max(c for _, c in pre), 300,
                           'charger never charged (vacuous scenario)')
        self.lib.charger_init()            # brownout/reset
        p.temp_dc = 550                    # now dangerously hot
        trace = run(p, 1500)
        self.assertEqual(max(d for d, _ in trace[200:]), 0,
                         "resumed charging without re-qualification")

    def test_b08_fault_latched_across_reset(self):
        p = self.start(temp_dc=250)
        run(p, 500)
        self.lib.shim_set_pack_mv(8700)    # hard overvoltage
        self.lib.shim_set_curr_ma(0)
        for _ in range(20):
            self.lib.charger_tick()
        self.lib.charger_init()            # reset after latched fault
        p2 = Plant(self.lib, pack_mv=7400.0, temp_dc=250)
        trace = run(p2, 1500)
        self.assertEqual(max(d for d, _ in trace[100:]), 0,
                         "fault forgotten across reset")

    def test_b09_eeprom_write_budget(self):
        p = self.start(temp_dc=250)
        before = self.lib.shim_get_eeprom_writes()
        run(p, 48000)                      # ~1 simulated minute
        writes = self.lib.shim_get_eeprom_writes() - before
        self.assertLess(writes, 500, f"{writes} EEPROM writes per minute")

    def test_b10_thermal_limit_every_phase(self):
        p = self.start(pack_mv=8380.0, temp_dc=250)
        pre = run(p, 3000)                 # into CV region
        self.assertGreater(max(c for _, c in pre), 300,
                           'charger never charged (vacuous scenario)')
        p.temp_dc = 520                    # 52 C: out of range
        trace = run(p, 2000)
        tail = [c for _, c in trace[-500:]]
        self.assertLess(max(tail), 100, "charging continued above 50C")

    def test_b11_fault_drives_output_safe_same_tick(self):
        p = self.start(temp_dc=250)
        run(p, 2000)
        self.lib.shim_set_pack_mv(8700)
        self.lib.shim_set_curr_ma(1500)
        self.lib.shim_set_temp_dc(250)
        self.lib.charger_tick()
        self.assertEqual(self.lib.shim_get_pwm(), 0,
                         "PWM live for a tick after fault detection")


if __name__ == "__main__":
    unittest.main()
