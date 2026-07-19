"""Hidden v2 acceptance suite for the charger firmware.

The suite drives only the fixed host-shim contract.  Every scenario gets a
uniquely named library image so firmware static state cannot leak between
tests.  Temporary images are unlinked immediately after ``ctypes`` loads
them; the open dynamic-loader mapping remains valid on the host platform.
"""

import ctypes
import math
import pathlib
import random
import shutil
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parent
LIB_PATH = ROOT / "repo" / "libcharger.so"

TICK_HZ = 798.72
FULL_CURRENT_MA = 2000.0
PRECHARGE_CURRENT_MA = 200.0
CV_THRESHOLD_MV = 8400.0
OVERVOLTAGE_FAULT_MV = 8601       # the contract faults strictly above 8600
OVERCURRENT_FAULT_MA = 2501       # the contract faults strictly above 2500
CHATTER_WINDOW_TICKS = math.ceil(10.0 * TICK_HZ)
WINDUP_RECOVERY_TICKS = math.ceil(5.0 * TICK_HZ)


def _declare_contract(lib):
    """Give ctypes the exact widths used by host/shim.c."""

    lib.charger_init.argtypes = []
    lib.charger_init.restype = None
    lib.charger_tick.argtypes = []
    lib.charger_tick.restype = None
    lib.pid_step.argtypes = [ctypes.c_int16, ctypes.c_int16]
    lib.pid_step.restype = ctypes.c_int16
    lib.pid_reset.argtypes = []
    lib.pid_reset.restype = None

    lib.shim_set_pack_mv.argtypes = [ctypes.c_uint16]
    lib.shim_set_pack_mv.restype = None
    lib.shim_set_curr_ma.argtypes = [ctypes.c_int16]
    lib.shim_set_curr_ma.restype = None
    lib.shim_set_temp_dc.argtypes = [ctypes.c_int16]
    lib.shim_set_temp_dc.restype = None
    lib.shim_get_pwm.argtypes = []
    lib.shim_get_pwm.restype = ctypes.c_int16
    lib.shim_get_eeprom_writes.argtypes = []
    lib.shim_get_eeprom_writes.restype = ctypes.c_uint32
    lib.shim_get_eeprom_phase.argtypes = []
    lib.shim_get_eeprom_phase.restype = ctypes.c_uint8
    lib.shim_set_eeprom_phase.argtypes = [ctypes.c_uint8]
    lib.shim_set_eeprom_phase.restype = None


def load():
    """Load a virgin firmware image and unlink its temporary pathname."""

    if not LIB_PATH.is_file():
        raise FileNotFoundError(
            f"{LIB_PATH} is missing; build it first with `make -C repo lib`"
        )

    tmp = tempfile.NamedTemporaryFile(suffix="-charger-v2.so", delete=False)
    tmp_path = pathlib.Path(tmp.name)
    tmp.close()
    try:
        shutil.copyfile(LIB_PATH, tmp_path)
        lib = ctypes.CDLL(str(tmp_path))
        _declare_contract(lib)
        return lib
    finally:
        # Linux retains the loaded mapping after unlink.  Keeping the name
        # would leak one tempfile for every acceptance scenario.
        tmp_path.unlink(missing_ok=True)


def prime(lib):
    """Apply v1's deterministic initial EEPROM/PID setup to a fresh image."""

    lib.shim_set_eeprom_phase(0)
    lib.pid_reset()
    return lib


class Plant:
    """RC pack driven through 0.12 ohm from a duty-scaled 9 V rail.

    ``leakage_ma`` is normally zero.  B10 uses a small load, subtracted
    only from stored pack charge, so a charger in CV must sustain positive
    terminal current instead of immediately tapering on a monotonic plant.
    """

    def __init__(
        self,
        lib,
        pack_mv=7400.0,
        temp_dc=250,
        stuck=False,
        leakage_ma=0.0,
    ):
        self.lib = lib
        self.pack_mv = float(pack_mv)
        self.temp_dc = temp_dc
        self.stuck = stuck
        self.leakage_ma = float(leakage_ma)
        self.curr_ma = 0.0
        self.noise = 0.0
        self.rng = random.Random(7)
        self.last_sensed_pack_mv = int(self.pack_mv)

    def install_sensors(self):
        self.lib.shim_set_pack_mv(int(self.pack_mv))
        self.lib.shim_set_curr_ma(int(self.curr_ma))
        self.lib.shim_set_temp_dc(self.temp_dc)

    def tick(self, measurement_noise_mv=None, held_pack_mv=None):
        if held_pack_mv is not None:
            self.pack_mv = float(held_pack_mv)

        if measurement_noise_mv is None:
            noise_mv = self.rng.uniform(-self.noise, self.noise)
        else:
            noise_mv = float(measurement_noise_mv)

        self.last_sensed_pack_mv = int(self.pack_mv + noise_mv)
        self.lib.shim_set_pack_mv(self.last_sensed_pack_mv)
        self.lib.shim_set_curr_ma(int(self.curr_ma))
        self.lib.shim_set_temp_dc(self.temp_dc)
        self.lib.charger_tick()
        duty = self.lib.shim_get_pwm()

        if self.stuck:
            # Saturated but not open-circuit: a trickle flows, and the pack
            # is held fixed by the stall fixture.
            self.curr_ma = 150.0 if duty > 0 else 0.0
        else:
            drive_mv = duty / 1023.0 * 9000.0
            self.curr_ma = max(0.0, (drive_mv - self.pack_mv) / 0.12)
            net_ma = self.curr_ma - self.leakage_ma
            delta_mv = net_ma * 4e-5 / 10.0
            self.pack_mv = min(8450.0, max(0.0, self.pack_mv + delta_mv))

        return duty, self.curr_ma


def run(plant, ticks):
    return [plant.tick() for _ in range(ticks)]


def mean(values):
    values = list(values)
    if not values:
        raise AssertionError("fixture produced an empty sample set")
    return sum(values) / len(values)


class AcceptanceV2(unittest.TestCase):
    def setUp(self):
        self.lib = prime(load())

    def fresh_lib(self):
        return prime(load())

    def start(self, lib=None, **plant_kwargs):
        lib = self.lib if lib is None else lib
        plant = Plant(lib, **plant_kwargs)
        plant.install_sensors()            # qualification data precedes init
        lib.charger_init()
        return plant

    def test_b01_room_temp_charges_full_current(self):
        plant = self.start(temp_dc=250)
        trace = run(plant, 6000)
        average_ma = mean(current for _, current in trace[-2000:])
        self.assertGreaterEqual(average_ma, 1900.0)
        self.assertLessEqual(
            average_ma,
            2100.0,
            f"room-temperature tail averaged {average_ma:.1f} mA",
        )

    def test_b02_precharge_current_is_bounded(self):
        plant = self.start(pack_mv=4200.0, temp_dc=250)
        trace = run(plant, 3000)
        self.assertLess(plant.pack_mv, 5000.0, "fixture escaped precharge")
        self.assertGreater(max(duty for duty, _ in trace), 0,
                           "sub-5 V pack never charged")
        average_ma = mean(current for _, current in trace)
        self.assertGreaterEqual(average_ma, 0.5 * PRECHARGE_CURRENT_MA)
        self.assertLessEqual(
            average_ma,
            1.5 * PRECHARGE_CURRENT_MA,
            f"precharge averaged an unsafe {average_ma:.1f} mA",
        )

    def test_b03_saturation_recovery_bounded(self):
        plant = self.start(temp_dc=250, stuck=True)
        run(plant, 8000)
        plant.stuck = False
        trace = run(plant, WINDUP_RECOVERY_TICKS)

        peak_ma = max(current for _, current in trace)
        settled_average_ma = mean(current for _, current in trace[-500:])
        self.assertLessEqual(peak_ma, 1.30 * FULL_CURRENT_MA,
                             f"release peak was {peak_ma:.1f} mA")
        self.assertGreaterEqual(settled_average_ma, 0.90 * FULL_CURRENT_MA)
        self.assertLessEqual(
            settled_average_ma,
            1.10 * FULL_CURRENT_MA,
            f"5 s recovery tail averaged {settled_average_ma:.1f} mA",
        )

    def test_b04_no_threshold_chatter_while_tracking(self):
        plant = self.start(pack_mv=8390.0, temp_dc=250)
        warm = run(plant, 500)
        self.assertGreater(mean(current for _, current in warm[-200:]), 0.0,
                           "charger was inactive before threshold fixture")

        samples = []
        for _ in range(CHATTER_WINDOW_TICKS):
            # An externally held pack plus seeded endpoint noise repeatedly
            # crosses the 8.4 V boundary without plant drift quietly ending
            # the scenario.
            noise_mv = -25.0 if plant.rng.random() < 0.5 else 25.0
            duty, current = plant.tick(
                measurement_noise_mv=noise_mv,
                held_pack_mv=CV_THRESHOLD_MV,
            )
            samples.append((plant.last_sensed_pack_mv, duty, current))

        duties = [duty for _, duty, _ in samples]
        big_swings = sum(
            1 for previous, current in zip(duties, duties[1:])
            if abs(previous - current) > 150
        )
        below_currents = [current for sensed, _, current in samples
                          if sensed < CV_THRESHOLD_MV]
        above_currents = [current for sensed, _, current in samples
                          if sensed > CV_THRESHOLD_MV]
        below_mean = mean(below_currents)
        above_mean = mean(above_currents)

        self.assertLessEqual(big_swings, 8,
                             f"{big_swings} large PWM swings in 10 s")
        self.assertGreater(below_mean, 0.0,
                           "no charging below the CC/CV threshold")
        self.assertGreater(above_mean, 0.0,
                           "no charging above the CC/CV threshold")
        self.assertGreater(
            below_mean,
            above_mean,
            "output did not respond directionally to voltage crossings",
        )

    def test_b05_cc_to_cv_transition_is_bumpless(self):
        plant = self.start(pack_mv=8390.0, temp_dc=250)
        duties = []
        crossing_index = None
        for index in range(3000):
            before_mv = plant.pack_mv
            duty, _ = plant.tick()
            duties.append(duty)
            if crossing_index is None and before_mv < CV_THRESHOLD_MV <= plant.pack_mv:
                crossing_index = index

        self.assertIsNotNone(crossing_index,
                             "correct 2 A fixture never crossed 8400 mV")
        start = max(0, crossing_index - 3)
        stop = min(len(duties), crossing_index + 5)
        local_duties = duties[start:stop]
        largest_step = max(
            abs(previous - current)
            for previous, current in zip(local_duties, local_duties[1:])
        )
        self.assertLessEqual(
            largest_step,
            260,
            f"PWM stepped {largest_step} counts around CC->CV crossing",
        )

    def test_b06_steady_state_from_above_and_below(self):
        for label, injected_current_ma in (("below", 1600.0), ("above", 2400.0)):
            with self.subTest(approach=label):
                lib = self.fresh_lib()
                plant = self.start(lib=lib, pack_mv=7400.0, temp_dc=250)
                run(plant, 4000)          # enter and stabilize in CC
                plant.curr_ma = injected_current_ma
                trace = run(plant, 12000)
                average_ma = mean(current for _, current in trace[-1000:])
                error_ma = abs(average_ma - FULL_CURRENT_MA)
                self.assertLessEqual(
                    error_ma,
                    50.0,
                    f"{label}-side approach retained {error_ma:.1f} mA error",
                )

    def test_b07_reset_requalifies_hot_and_absent_pack(self):
        cases = (
            ("hot", 7400.0, 550),
            ("pack_absent", 0.0, 250),
        )
        for label, reset_pack_mv, reset_temp_dc in cases:
            with self.subTest(condition=label):
                lib = self.fresh_lib()
                plant = self.start(lib=lib, pack_mv=7400.0, temp_dc=250)
                charging = run(plant, 2000)
                self.assertGreater(
                    mean(current for _, current in charging[-500:]),
                    0.0,
                    "fixture was not charging before reset",
                )

                # Install unsafe qualification inputs before charger_init,
                # not on some later tick after stale state has resumed.
                plant.pack_mv = reset_pack_mv
                plant.temp_dc = reset_temp_dc
                plant.curr_ma = 0.0
                plant.install_sensors()
                lib.charger_init()
                trace = run(plant, 1500)
                self.assertEqual(
                    max(duty for duty, _ in trace),
                    0,
                    "PWM was nonzero from the first post-reset tick",
                )

    def test_b08_fault_latch_survives_fresh_library_brownout(self):
        plant = self.start(temp_dc=250)
        run(plant, 2000)
        self.lib.shim_set_pack_mv(OVERVOLTAGE_FAULT_MV)
        self.lib.shim_set_curr_ma(0)
        self.lib.shim_set_temp_dc(250)
        self.lib.charger_tick()
        saved_eeprom_phase = self.lib.shim_get_eeprom_phase()

        # A new image models erased RAM/BSS.  Only raw EEPROM contents cross
        # the brownout boundary.
        post_brownout = load()
        post_brownout.shim_set_eeprom_phase(saved_eeprom_phase)
        recovered = Plant(post_brownout, pack_mv=7400.0, temp_dc=250)
        recovered.install_sensors()
        post_brownout.charger_init()
        trace = run(recovered, 1500)
        self.assertEqual(max(duty for duty, _ in trace), 0,
                         "EEPROM-restored hard fault self-cleared")

    def test_b09_eeprom_writes_only_on_transitions(self):
        plant = self.start(temp_dc=250)
        before = self.lib.shim_get_eeprom_writes()
        warm = run(plant, 12000)
        warm_average_ma = mean(current for _, current in warm[-2000:])
        self.assertGreaterEqual(warm_average_ma, 1900.0,
                                "fixture did not stabilize in CC")
        self.assertLessEqual(warm_average_ma, 2100.0,
                             "fixture did not stabilize in CC")
        after_warm = self.lib.shim_get_eeprom_writes()

        run(plant, 48000)
        after_plateau = self.lib.shim_get_eeprom_writes()
        plateau_writes = after_plateau - after_warm
        total_writes = after_plateau - before
        self.assertEqual(plateau_writes, 0,
                         f"stable CC plateau added {plateau_writes} writes")
        self.assertLessEqual(total_writes, 8,
                             f"scenario used {total_writes} EEPROM writes")

    def test_b10_thermal_limit_in_precharge_cc_and_cv(self):
        cases = (
            ("precharge", 4200.0, 3000, 0.0),
            ("CC", 7400.0, 6000, 0.0),
            ("CV", 8390.0, 6000, 300.0),
        )
        for label, pack_mv, warm_ticks, leakage_ma in cases:
            with self.subTest(phase=label):
                lib = self.fresh_lib()
                plant = self.start(
                    lib=lib,
                    pack_mv=pack_mv,
                    temp_dc=250,
                    leakage_ma=leakage_ma,
                )
                maximum_pack_mv = plant.pack_mv
                warm = []
                for _ in range(warm_ticks):
                    warm.append(plant.tick())
                    maximum_pack_mv = max(maximum_pack_mv, plant.pack_mv)

                active_average_ma = mean(
                    current for _, current in warm[-500:]
                )
                self.assertGreaterEqual(
                    active_average_ma,
                    100.0,
                    "not actively charging during the 500 pre-heat ticks",
                )
                if label == "precharge":
                    self.assertLess(plant.pack_mv, 5000.0)
                elif label == "CC":
                    self.assertGreaterEqual(plant.pack_mv, 6000.0)
                    self.assertLess(plant.pack_mv, CV_THRESHOLD_MV)
                else:
                    self.assertGreaterEqual(
                        maximum_pack_mv,
                        CV_THRESHOLD_MV,
                        "CV fixture never reached the voltage threshold",
                    )

                plant.temp_dc = 520
                hot_trace = run(plant, 2000)
                self.assertEqual(
                    max(current for _, current in hot_trace),
                    0.0,
                    f"charging continued at 52 C during {label}",
                )

    def test_b11_each_hard_fault_is_safe_same_tick_and_latched(self):
        cases = (
            ("overvoltage", OVERVOLTAGE_FAULT_MV, 0),
            ("overcurrent", 7400, OVERCURRENT_FAULT_MA),
        )
        for label, fault_pack_mv, fault_current_ma in cases:
            with self.subTest(fault=label):
                lib = self.fresh_lib()
                plant = self.start(lib=lib, pack_mv=7400.0, temp_dc=250)
                charging = run(plant, 2000)
                self.assertGreater(
                    mean(current for _, current in charging[-500:]),
                    0.0,
                    "fixture was inactive before fault injection",
                )

                lib.shim_set_pack_mv(fault_pack_mv)
                lib.shim_set_curr_ma(fault_current_ma)
                lib.shim_set_temp_dc(250)
                lib.charger_tick()
                self.assertEqual(lib.shim_get_pwm(), 0,
                                 f"PWM live on {label} detection tick")

                latched = Plant(lib, pack_mv=7400.0, temp_dc=250)
                trace = run(latched, 100)
                self.assertEqual(max(duty for duty, _ in trace), 0,
                                 f"{label} fault did not remain latched")

    def test_b12_temperature_band_boundaries(self):
        cases = (
            ("10.0 C inclusive", 100, 2000.0, 100.0),
            ("45.0 C inclusive", 450, 2000.0, 100.0),
            ("5.0 C half", 50, 1000.0, 100.0),
            ("47.0 C half", 470, 1000.0, 100.0),
        )
        for label, temp_dc, target_ma, tolerance_ma in cases:
            with self.subTest(temperature=label):
                lib = self.fresh_lib()
                plant = self.start(lib=lib, pack_mv=7400.0, temp_dc=temp_dc)
                trace = run(plant, 6000)
                average_ma = mean(current for _, current in trace[-2000:])
                self.assertGreaterEqual(average_ma, target_ma - tolerance_ma)
                self.assertLessEqual(
                    average_ma,
                    target_ma + tolerance_ma,
                    f"{label} averaged {average_ma:.1f} mA",
                )


if __name__ == "__main__":
    unittest.main()
