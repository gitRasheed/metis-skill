# cases-v7: v1.4.0 vs v1.3.2

v1.4.0 shipped after three preregistered waves (`PREREG.md`). The first two candidates were refused, and the third passed every bar.

**Setup.**
- **Arms:** v1.3.2 against each v1.4.0 candidate, with the skill force-loaded.
- **Cases:** two review cases scored against frozen v5 keys (R1, R2), two Java tasks in enterprise interface/Impl style (J1 feature, J2 dedupe bug), and a Python persisted-format change (P1).
- **Measures:** hidden tests, blinded "which codebase would you rather inherit" judging, review key points, a noise scan, and wall time.

**Waves 1 and 2** (Opus 5.5 and GPT-6.1 Sol, n=2):
- **Every candidate passed the hidden checks.**
- **Neither candidate cleared the inherit bar.** The bar was preferred or tied in at least 40% of pairs, and both fell short.
- **The judges named three causes:**
  - compatibility shims kept for callers inside the same codebase;
  - fixes inlined where a small seam was cleaner;
  - heavier tests.
- **Each cause traced to a specific edit and was fixed.** For example, constructor replacement in J1 went from 0 of 4 runs to 4 of 4.

**Wave 3** (Sol `#high`, n=6, 60 runs, judged blind by Astra):

| Bar | v1.3.2 | v1.4.0 | Result |
|---|---|---|---|
| Hidden checks | 18/18 | 18/18 | pass |
| R1 key score | 9.67 | 9.83 | pass |
| R2 key score | 23.83 | 22.83 | pass (bar: within 1.0) |
| Inherit, preferred or tied | | 8/18 (44%) | pass (bar: 40%) |
| Rule citations in code | 0 | 0 | pass |
| Judged noise, 1 to 5 | 1.06 | 1.11 | pass |
| Mean wall time | 120.5 s | 121.9 s | pass |

**Notes.**
- **Opus** ran only at n=2, in waves 1 and 2.
- **Untested additions.** The SOLID pointer line, the Design 19 rewrite and the two performance sections in `performance-and-safety.md` were added after wave 3 and not A/B tested. A 6-run Sol J2 check on the final text passed.
- **Description triggering** was measured separately on Opus. The shipped wording loads the skill on most feature, refactor, design, test and review tasks and skips small bug fixes.

**Files.**
- `PREREG.md` holds the rules and the per-wave journal.
- `FIXTURES.md` describes the Java cases.
- `run.sh`, `judge_w3.py`, `extract.py` and `noise.py` are the harness.
