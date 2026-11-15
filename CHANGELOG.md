# Changelog

## 0.2.0

- Two new loot-only enchantments, so packs without Apotheosis have a source of mana leech:
  - Mana-Thirsty (`runelessrpg:mana_leech`), melee and ranged weapons: +2 / 3.5 / 5 % Mana Leech.
  - Resonant (`runelessrpg:spell_mana_leech`), spell weapons: +1 / 2 / 3 % Spell Mana Leech.
  - Maximum level III. They only come from loot (enchanted weapons and books in chests, fishing), never from the
    enchanting table, villagers or mob equipment. They stack with the Apotheosis affixes.
  - All existing leech rules apply (less for further targets, no overkill, no summons, clouds or damage over time).
- New item tag `#runelessrpg:enchantable/weapon` (swords, axes, maces, bows, crossbows, tridents, without spell weapons).
- New server config options `manaLeechEnchantmentEnabled` and `spellManaLeechEnchantmentEnabled` in `[mana_leech]`.
  When turned off, the enchantment has no effect, also on items that already have it, and it is removed from newly
  generated loot. Changes apply immediately.
- Network protocol 7: clients and servers need the same RunelessRPG version.

## 0.1.0 – first release

- Mana pool for every player: `runelessrpg:max_mana` (20) and `runelessrpg:mana_regen` (0.5/s), base values from the
  server config; full on respawn, kept on login and dimension change.
- Spell Engine spells that cost runes cost mana instead; class skills, weapon skills and item abilities through spell
  tags (`#runelessrpg:cost/*`), free spells through `#runelessrpg:cost/free`.
- Cost by spell tier (`tierCosts`) and category multiplier, per-spell costs from datapacks
  (`data/<ns>/runelessrpg/spell_costs/*.json`), floor for Mana Cost.
- Mana modes `FULL_COST`, `DEBT`, `DEBT_LIMIT`; free casting in creative.
- Channeled spells pay per tick (`DRAIN`), at the start or at the end; cooldown on interrupted channels.
- Runes are never consumed; arrows and other cost items stay.
- Attributes Mana Cost, Overcharge, Mana Leech and Spell Mana Leech; Overcharge snapshot for summons and spell clouds.
- Spell Infinity reduces mana costs.
- Mana HUD: styles `THIN`, `MIKI`, `VERTICAL`, `MIKI_DIAMOND`, `MIKI_BAND`, screen and Miki's Fantasy HUD anchors,
  layout editor, mana number as its own element, `ON_CHANGE` visibility, flash on missing mana, hover tooltip.
- Seven enchantments: Manafused, Lucid, Mana-Stabilized, Cerulean, Resplendent, Azure, Inspired.
- Potions of Mana I/II, Magical Stability, Borrowed Power (with backlash), brewing ingredients in the common config.
- Apotheosis: six affixes and the affix type `runelessrpg:attribute` with item filters, guarded by the load condition
  `runelessrpg:apotheosis_integration`.
- Attribute icons (shown by Gearscope).
- Commands `/runelessrpg mana`, `/runelessrpg cost`, `/runelessrpg report` and the generated cost report.
- API `RunelessApi`, events `ManaCostEvent` and `ManaChangeEvent`.
