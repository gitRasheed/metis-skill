from dataclasses import dataclass
from datetime import date


@dataclass
class Habit:
    name: str
    every_days: int
    due: date
    done_count: int = 0
