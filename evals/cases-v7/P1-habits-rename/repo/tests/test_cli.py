from habits import cli, store


def test_add_then_list(tmp_path, monkeypatch, capsys):
    monkeypatch.setenv("HABITS_FILE", str(tmp_path / "h.json"))
    cli.main(["add", "stretch", "--every", "2"])
    cli.main(["list"])
    out = capsys.readouterr().out
    assert "stretch" in out


def test_done_increments(tmp_path, monkeypatch):
    monkeypatch.setenv("HABITS_FILE", str(tmp_path / "h.json"))
    cli.main(["add", "read"])
    cli.main(["done", "read"])
    assert store.load()[0].done_count == 1
