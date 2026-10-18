import json
import os
from datetime import date
from pathlib import Path

from habits.model import Habit


def data_path() -> Path:
    return Path(os.environ.get("HABITS_FILE", Path.home() / ".habits.json"))


def load() -> list[Habit]:
    path = data_path()
    if not path.exists():
        return []
    raw = json.loads(path.read_text())
    return [
        Habit(
            name=h["name"],
            every_days=h["every_days"],
            due=date.fromisoformat(h["due"]),
            done_count=h.get("done_count", 0),
        )
        for h in raw["habits"]
    ]


def save(habits: list[Habit]) -> None:
    payload = {
        "habits": [
            {
                "name": h.name,
                "every_days": h.every_days,
                "due": h.due.isoformat(),
                "done_count": h.done_count,
            }
            for h in habits
        ]
    }
    data_path().write_text(json.dumps(payload, indent=2))
