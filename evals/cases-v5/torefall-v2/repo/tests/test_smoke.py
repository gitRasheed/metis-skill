import sys, pathlib, unittest
sys.path.insert(0, str(pathlib.Path(__file__).parent.parent))
from torefall.world import *


class Smoke(unittest.TestCase):
    def test_join_raid_cast(self):
        w = create_world(1)
        join(w, "a", "warrior"); join(w, "m", "mage")
        start_raid(w, ["a", "m"])
        cast(w, "a", "shout"); cast(w, "m", "amplify")
        self.assertEqual(cast(w, "m", "attack")["damage"], 52)
        tick(w)
        self.assertTrue(get_state(w, "a")["boss"]["alive"])


if __name__ == "__main__":
    unittest.main()
