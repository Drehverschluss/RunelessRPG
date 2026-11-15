# Changelog

## 0.2.0

> **Existing configs change automatically:** the new option `backlashMode` in `[potions]` is added to existing
> `runelessrpg-server.toml` files with its default `PERCENT_MAX_HEALTH`. The Borrowed Power backlash then scales with
> max health instead of the old fixed values. Set `backlashMode = "FLAT"` to keep the old behavior; your values in
> `backlashDamagePer10Percent` and `backlashMaxDamage` stay in the file and apply again.

### Leech enchantments

- Two new loot-only enchantments, so packs without Apotheosis have a source of mana leech:
  - Mana-Thirsty (`runelessrpg:mana_leech`), melee and ranged weapons: +1.5 / 2.5 / 3.5 % Mana Leech.
  - Resonant (`runelessrpg:spell_mana_leech`), spell weapons: +1 / 2 / 3 % Spell Mana Leech.
  - Maximum level III. They only come from loot (enchanted weapons and books in chests, fishing), never from the
    enchanting table, villagers or mob equipment. They stack with the Apotheosis affixes.
  - All existing leech rules apply (less for further targets, no overkill, no summons, clouds or damage over time).
- New item tag `#runelessrpg:enchantable/weapon` (swords, axes, maces, bows, crossbows, tridents, without spell weapons).
- New server config options `manaLeechEnchantmentEnabled` and `spellManaLeechEnchantmentEnabled` in `[mana_leech]`.
  When turned off, the enchantment has no effect, also on items that already have it, and it is removed from newly
  generated loot. Changes apply immediately.

### Mana Leech limit

- New server config option `manaLeechMaxPerSecond` in `[mana_leech]` (default 2.5, 0 = no limit): weapon mana leech
  gives at most this much mana per second, from all sources of the attribute together (affixes, enchantment, gear).
  Up to one second of it can be stored for a single big hit. Spell Mana Leech has no limit.

### Spell costs for the RPG Series

- Built-in cost factors for the fast staff spells and the channeled spells of the RPG Series, so the four wizard
  schools run out of mana at a similar rate. The factor multiplies the tier cost:

  | Spell | Tier | Factor | Cost (default tier costs) |
  |---|---|---|---|
  | Frostbolt, Lightning Bolt (Stormcaller) | 1 | × 0.7 | 3.5 instead of 5 |
  | Arcane Missile | 2 | × 1.5 | 10.5 instead of 7 |
  | Fire Breath | 2 | × 2 | 14 instead of 7 |
  | Arcane Beam, Fire Storm | 3 | × 2 | 20 instead of 10 |
  | Frost Blizzard | 4 | × 2 | 28 instead of 14 |

  They live in `data/runelessrpg/runelessrpg/spell_costs/rpg_series.json`. Spells that are not installed are skipped.
- Spell cost files have a new key `"factors"` next to `"costs"`: a factor on the tier cost. Unlike a fixed cost, it
  follows `tierCosts`, the category multiplier and turning the category off.
- Files with the same path now replace each other (the highest pack wins), like recipes or loot tables. A datapack with
  `data/runelessrpg/runelessrpg/spell_costs/rpg_series.json` and `{"costs": {}}` turns the built-in values off.
- New cost source `BUILTIN`. `/runelessrpg cost` and the cost report show the calculation, e.g.
  `tier 1: 5 × 0.7 = 3.5 (BUILTIN)`.

### Potions

- New effect Mana Sickness (`runelessrpg:mana_sickness`): every Potion of Mana gives it for 20 seconds (drunk, splash,
  lingering cloud, tipped arrow). During Mana Sickness a Potion of Mana restores only 25 % of its normal amount and
  renews the effect. Milk and totems do not remove it, death does. New options `manaSicknessSeconds` (0 = off) and
  `manaSicknessRestorePercent` in `[potions]`.
- Borrowed Power backlash in percent of max health: new option `backlashMode` (`PERCENT_MAX_HEALTH` or `FLAT`) and
  the options `backlashHealthPercentPer10Percent` (default 10) and `backlashMaxHealthPercent` (default 50). With 20
  health this is the same as before (10 damage at 50 % debt); with more health the backlash grows with it.

### Other

- German texts: "Mana-Entzug" everywhere (config used "Manaraub").
- Shorter names in the config screen sections Mana Leech and Potions, so they are no longer cut off (English and German).
- Network protocol 8: clients and servers need the same RunelessRPG version.

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
