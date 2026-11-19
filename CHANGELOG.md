# Changelog

## 0.3.0

### Mana orbs

- New entity Mana Orb (`runelessrpg:mana_orb`): a blue orb that restores 3 % of the max mana of whoever picks it up, paying debt back first. Any player picks it up on touch, also with full mana (then the orb is used up). Mobs and spectators never pick orbs up.
- Orbs stay on the ground for 30 seconds and do not fly to players. Up to 5 nearby orbs merge into one bigger orb; the rest stays as an orb of its own (7 orbs give 5 + 2). The orb grows with every orb in it, up to 5.
- A touch takes the whole orb at once: a full orb gives 5 × 3 % = 15 % of max mana. You see at a glance what an orb is worth and decide whether to take it or leave it to a team mate.
- Full orbs sparkle: golden sparks rise slowly from them and they pulse gently, so you spot them on the ground. The sparkles follow the particle setting of the game.
- Orbs cannot be destroyed by explosions, fire, lava or attacks. In lava they are thrown up like experience orbs.
- Orbs neither cause Mana Sickness nor are they weakened by it. `ManaChangeEvent` has the new reason `ORB`.
- Without the new potion no orbs drop by default.

### Potion of Mana Harvest

- New effect Mana Harvest (`runelessrpg:mana_harvest`) and its potions: Potion of Mana + rabbit's foot (3:00), with redstone 8:00, with glowstone level II (1:30). Splash, lingering and tipped arrow forms work like vanilla.
- Monsters killed by a player with Mana Harvest drop an orb: 50 % chance with level I, 100 % with level II. Only kills that would drop experience count (the player hit the mob, `doMobLoot` on); fake players of mob farms, deployers and killer blocks never count. Looting changes nothing.
- Several kills by the same player in the same tick (area spells) count less: the first one gets the full chance, every further one 25 % of it.
- Bosses (`#runelessrpg:mana_orb_bosses`, contains `#c:bosses`) drop no orbs when they die. Instead, every hit the player causes on a boss creates an orb with 10 % (I) or 20 % (II) chance, at most one every 3 seconds per player. The orb appears on free ground 2 to 4 blocks around the player, so spellcasters at range and team mates can pick it up. Thorns damage does not count.
- The potion restores no mana itself and causes no Mana Sickness.
- The potion tooltips explain the chances with the values from the server config, and Just Enough Effect Descriptions shows a short description.

### For pack developers

- New server config section `[mana_orbs]`: `enabled`, `orbManaPercent`, `orbLifetimeSeconds`, `maxOrbsPerStack` (5, 1 = no merging), `killBaseChancePercent` (0), `killChancePercentPerLevel` (50), `extraKillPercent` (25), `killCooldownSeconds` (0), `killMobCategories` (`["MONSTER"]`), `bossKillOrbs` (0), `bossHitBaseChancePercent` (0), `bossHitChancePercentPerLevel` (10), `bossHitCooldownSeconds` (3), `bossOrbMinDistance` and `bossOrbMaxDistance` (2 and 4). A base chance above 0 turns on orbs without the potion; the kill cooldown can slow them down.
- New options in `[potions]`: `manaHarvestDurationSeconds` (180), `longManaHarvestDurationSeconds` (480), `strongManaHarvestDurationSeconds` (90). New brewing ingredients in the common config: `manaHarvestIngredient`, `longManaHarvestIngredient`, `strongManaHarvestIngredient`.
- New client config section `[mana_orbs]`: `fullOrbEffect` (`BOTH`, `SPARKLES`, `PULSE` or `NONE`) for the effect of full orbs.
- New data files `data/<ns>/runelessrpg/mana_orbs/*.json` with a chance `factor`, an orb `count` and an optional `baseChance` per mob or entity tag. A file with the same path replaces the one from a lower pack. A `count` above `maxOrbsPerStack` drops several orbs.
- New tags: `#runelessrpg:no_mana_orbs` (mobs that never drop orbs), `#runelessrpg:mana_orb_bosses` (boss rules) and the damage type tag `#runelessrpg:mana_orbs/ignored_damage` (thorns).

### Other

- Network protocol 10 (the cost settings packet now carries the mana orb and Mana Harvest values): clients and servers need the same RunelessRPG version.

## 0.2.2

- Shorter effect descriptions for Just Enough Effect Descriptions (JEED), English and German. Each one now fits into a single box on the JEI page instead of being split into two.
- No other changes. Network protocol 9 as in 0.2.1, so 0.2.1 and 0.2.2 can join each other.

## 0.2.1

### Lingering Potions of Mana

- A lingering cloud of Mana now restores **5 % of max mana per level each second** to every player inside (Mana I 5 %, Mana II 10 %), paying debt back first. It **causes no Mana Sickness and is not weakened by it**, so it works alongside a drunk potion. The cloud shrinks with every application: one player gets about five seconds of it (about 25 % / 50 % in total), several players share it.
- Before, a cloud gave half of a normal potion at the first touch and caused Mana Sickness, which then cut every further second down to a quarter.
- New server config option `lingeringManaPercentPerLevel` in `[potions]` (default 5).
- Drunk and splash potions and tipped arrows are unchanged.

### Potion tooltips

- Potions, splash and lingering potions and tipped arrows with a RunelessRPG effect now explain what they do, in short gray lines right below the effect line (always visible, also in the creative inventory and in JEI):
  - Potion of Mana I/II: how much max mana it restores (splash: up to; arrow: on hit) and the following Mana Sickness; lingering: the share each second in the cloud and that it causes no Mana Sickness.
  - Magical Stability: that mana cost still never drops below the floor (`minManaCostPercent`).
  - Borrowed Power: the debt limit and the backlash, in percent of max health or as fixed damage, depending on `backlashMode`.
- The numbers come from the server config and update when it is reloaded, without reconnecting. Lines whose value is turned off (e.g. `manaSicknessSeconds = 0`) are left out.
- Descriptions for effect description mods such as Just Enough Effect Descriptions (JEED): `effect.runelessrpg.<id>.description` for Mana Restoration, Magical Stability, Borrowed Power and Mana Sickness.

### Other

- Network protocol 9 (the cost settings packet now carries the Mana Sickness, backlash and lingering values): clients and servers need the same RunelessRPG version, 0.2.0 and 0.2.1 cannot join each other.

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
