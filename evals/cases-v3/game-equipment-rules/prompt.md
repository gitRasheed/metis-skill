# Game Equipment Refactor

You are reviewing this simplified implementation from a game backend. External game code already imports and instantiates `Sword`, `Bow`, and `FireSword`, so this public interface needs to keep working during the first refactor.

```python
class Equipment:
    def __init__(self, owner_class, min_level):
        self.owner_class = owner_class
        self.min_level = min_level

    def can_equip(self, character):
        return character.level >= self.min_level and character.kind == self.owner_class

    def damage(self, target, range_to_target=None):
        raise NotImplementedError

    def apply_effects(self, target):
        return []


class Sword(Equipment):
    def __init__(self, rarity, durability, owner_class="warrior", min_level=1):
        super().__init__(owner_class, min_level)
        self.rarity = rarity
        self.durability = durability

    def can_equip(self, character):
        if self.durability <= 0:
            return False
        return super().can_equip(character)

    def damage(self, target, range_to_target=None):
        damage = 10
        if self.rarity == "rare":
            damage += 3
        if self.durability < 20:
            damage -= 2
        if target.armor == "heavy":
            damage -= 1
        return max(damage, 0)


class Bow(Equipment):
    def __init__(self, rarity, durability, owner_class="ranger", min_level=1):
        super().__init__(owner_class, min_level)
        self.rarity = rarity
        self.durability = durability

    def can_equip(self, character):
        if self.durability <= 0:
            return False
        return super().can_equip(character)

    def damage(self, target, range_to_target=None):
        if range_to_target is None:
            raise ValueError("range required")
        damage = 7
        if range_to_target > 30:
            damage -= 4
        if self.rarity == "rare":
            damage += 3
        if self.durability < 20:
            damage -= 2
        return max(damage, 0)


class FireSword(Sword):
    def damage(self, target, range_to_target=None):
        damage = super().damage(target, range_to_target)
        if target.is_wet:
            damage -= 2
        return max(damage + 5, 0)

    def apply_effects(self, target):
        if target.fire_resistant:
            return []
        return ["burn"]
```

A new balance rule now needs to reduce damage for low-durability rare equipment across every equipment kind. Designers also want rarity, durability, elemental effects, and character restrictions to combine with any kind in the future.

Constraints:

- `Sword`, `Bow`, and `FireSword` must remain constructible and keep their public methods for now.
- Bow damage still needs range-specific input.
- Fire effects still need special validation.
- The first change should be reviewable and low-risk, not a big-bang rewrite.
- You do not need to provide a full implementation, but your answer should be concrete enough that another engineer could start the refactor.

Return:

1. The main design problem in this implementation.
2. The first refactor you would make while preserving existing callers.
3. The internal model or helper boundaries you would introduce.
4. How the new balance rule would land without editing every class.
5. The behavior checks that should exist before and after the refactor.
6. Which abstractions you would avoid for now.

Do not edit files. Return your answer in Markdown.
