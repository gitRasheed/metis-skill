import json
from datetime import date

from habits import cli, store

OLD_FILE = {
    "habits": [
        {"name": "stretch", "every_days": 2, "due": "2026-09-01", "done_count": 4},
        {"name": "read", "every_days": 1, "due": "2026-09-02"},
    ]
}


def _write_old(tmp_path, monkeypatch):
    path = tmp_path / "h.json"
    path.write_text(json.dumps(OLD_FILE))
    monkeypatch.setenv("HABITS_FILE", str(path))
    return path


def test_existing_user_file_still_loads(tmp_path, monkeypatch):
    _write_old(tmp_path, monkeypatch)
    habits = store.load()
    assert [h.name for h in habits] == ["stretch", "read"]
    assert habits[0].due_date == date(2026, 9, 1)
    assert habits[0].done_count == 4
    assert list(habits[0].tags) == []


def test_old_file_survives_a_command_and_reload(tmp_path, monkeypatch):
    path = _write_old(tmp_path, monkeypatch)
    cli.main(["done", "read"])
    saved = json.loads(path.read_text())
    names = [h["name"] for h in saved["habits"]]
    assert names == ["stretch", "read"]
    assert all("due_date" in h for h in saved["habits"])
    assert store.load()[0].due_date == date(2026, 9, 1)


def test_tags_round_trip_and_filter(tmp_path, monkeypatch, capsys):
    monkeypatch.setenv("HABITS_FILE", str(tmp_path / "h.json"))
    cli.main(["add", "stretch", "--tag", "health", "--tag", "morning"])
    cli.main(["add", "taxes"])
    capsys.readouterr()
    cli.main(["list", "--tag", "health"])
    out = capsys.readouterr().out
    assert "stretch" in out and "taxes" not in out
    assert sorted(store.load()[0].tags) == ["health", "morning"]
