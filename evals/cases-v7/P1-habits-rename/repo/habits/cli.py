import argparse
from datetime import date, timedelta

from habits import store
from habits.model import Habit


def cmd_add(args):
    habits = store.load()
    habits.append(Habit(name=args.name, every_days=args.every, due=date.today()))
    store.save(habits)
    print(f"added {args.name}")


def cmd_done(args):
    habits = store.load()
    for h in habits:
        if h.name == args.name:
            h.done_count += 1
            h.due = date.today() + timedelta(days=h.every_days)
    store.save(habits)


def cmd_list(args):
    for h in store.load():
        marker = "!" if h.due <= date.today() else " "
        print(f"{marker} {h.name:20} due {h.due.isoformat()}  done {h.done_count}x")


def main(argv=None):
    parser = argparse.ArgumentParser(prog="habits")
    sub = parser.add_subparsers(required=True)
    p = sub.add_parser("add")
    p.add_argument("name")
    p.add_argument("--every", type=int, default=1)
    p.set_defaults(func=cmd_add)
    p = sub.add_parser("done")
    p.add_argument("name")
    p.set_defaults(func=cmd_done)
    p = sub.add_parser("list")
    p.set_defaults(func=cmd_list)
    args = parser.parse_args(argv)
    args.func(args)


if __name__ == "__main__":
    main()
