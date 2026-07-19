import ctypes
import pathlib
import unittest

ROOT = pathlib.Path(__file__).parent.parent


def load():
    lib = ctypes.CDLL(str(ROOT / "libcharger.so"))
    lib.pid_step.restype = ctypes.c_int16
    lib.shim_get_pwm.restype = ctypes.c_int16
    lib.shim_get_eeprom_writes.restype = ctypes.c_uint32
    return lib


class Smoke(unittest.TestCase):
    def test_builds_loads_and_ticks(self):
        lib = load()
        lib.shim_set_pack_mv(7400)
        lib.shim_set_curr_ma(0)
        lib.shim_set_temp_dc(250)
        lib.charger_init()
        for _ in range(10):
            lib.charger_tick()
        pwm = lib.shim_get_pwm()
        self.assertTrue(0 <= pwm <= 1023)

    def test_pid_step_returns_bounded(self):
        lib = load()
        lib.pid_reset()
        out = lib.pid_step(2000, 0)
        self.assertTrue(0 <= out <= 1023)


if __name__ == "__main__":
    unittest.main()
