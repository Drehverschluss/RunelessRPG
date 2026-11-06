# Changelog

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
