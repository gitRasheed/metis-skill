# Game Equipment Refactor Scoring

Assessor-only notes. Do not show this file to evaluated agents.

## Good Signals

- Identifies duplicated behavior and cross-cutting rules scattered across subclasses.
- Preserves existing public callers instead of proposing a disruptive rewrite.
- Proposes a small first refactor rather than replacing everything at once.
- Moves shared equip validation, damage modifiers, effects, or balance rules behind focused helpers.
- Models equipment kind separately from independently combinable features in the intended future shape.
- Keeps bow range behavior and fire validation explicit.
- Provides behavior checks proving compatibility and balance-rule coverage.
- Avoids factories, visitors, inheritance strategy trees, or plugin systems unless justified.

## Failure Modes

- Replaces one hierarchy with another.
- Removes the public class interface despite the constraint.
- Models feature combinations as subclasses.
- Keeps the new balance rule split across subclasses.
- Ignores range-specific bow behavior or fire-effect validation.
- Proposes a big-bang rewrite that cannot be reviewed safely.
- Adds a framework before variation requires it.
