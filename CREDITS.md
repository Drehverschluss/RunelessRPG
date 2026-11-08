# Credits

## RPGMana

[RPGMana](https://github.com/cleannrooster/rpgmana) by cleannrooster, MIT License, Copyright (c) 2024 cleannrooster
(full text below). The repository's `LICENSE` file is MIT; the mod metadata (`fabric.mod.json`, and the
`neoforge.mods.toml` of the NeoForge release) says `CC0-1.0`. Earlier versions of these credits wrongly said CC0-1.0;
the files are used under the MIT License. The license text is also bundled in the mod jar as
`META-INF/licenses/RPGMana-LICENSE.txt`.

- `assets/runelessrpg/textures/item/mana.png`: the mana item texture, taken unchanged from
  `assets/rpgmana/textures/item/mana.png`.
- `assets/runelessrpg/textures/mob_effect/borrowed_power.png` and `magical_stability.png`: the effect icons of the same
  name, placed on an 18×18 canvas.
- `assets/runelessrpg/textures/mob_effect/mana_restore.png`: RPGMana's `magical_stability` icon, recolored.
- Idea source for several enchantments (Manafused, Lucid, Mana-Stabilized, Cerulean, Resplendent, Azure, Inspired)
  and the Borrowed Power and Magical Stability potions. RunelessRPG has its own implementation; no code of RPGMana is
  used.

<details>
<summary>RPGMana license (MIT)</summary>

```
MIT License

Copyright (c) 2024 cleannrooster

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

</details>

## Attribute icons

- `assets/runelessrpg/textures/attribute_icon/*.png` (the six attribute icons): from
  **"RPG Icon Pack – 400+ Skills and Spells"** by Clockwork Raven Studios. `mana_cost` is recolored; `mana_leech` and
  `spell_mana_leech` have an added "+".

## Spell cost report

- The idea of a generated cost overview (`spell_costs_report.json`) is inspired by **Savaru's Mana** by RowLan_232oO.
  No code or assets of it are used.

## Mods RunelessRPG works with (not bundled)

RunelessRPG contains no code, textures, models, sounds or translations of these mods. It is compiled against the APIs
listed below and otherwise only refers to their registry ids (spells in the `runelessrpg:cost/*` spell tags, item and
damage type tags, mod ids in `neoforge:mod_loaded` conditions).

- [Spell Engine](https://modrinth.com/mod/spell-engine) by Daedelus (GPL-3.0): required; RunelessRPG replaces its
  rune costs with mana. Its weapon skills (`rpg_series:*`) are listed in the weapon skill cost tag. Only compiled
  against it, not bundled.
- [Spell Power](https://modrinth.com/mod/spell-power) by Daedelus (GPL-3.0): required by Spell Engine; its damage type
  tag `spell_power:all` and item tag `spell_power:enchantable/haste` are used by id. Not bundled.
- [Runes](https://modrinth.com/mod/runes) by Daedelus (All Rights Reserved): the tag `runes:runes` is used by id to
  recognize rune costs. Not bundled.
- The RPG Series class mods by Daedelus (Wizards, Paladins & Priests, Rogues & Warriors, Archers; All Rights Reserved)
  and other Spell Engine add-ons (Archers Expansion, Bards RPG, Berserker RPG, Forcemaster RPG, More RPG Library,
  Relics, More Relics, the `lne_*` add-ons): their spell ids are listed in the cost tags (all as optional entries).
  Not bundled.
- [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) by Shadows_of_Fire (MIT code / All Rights
  Reserved assets), with [Placebo](https://www.curseforge.com/minecraft/mc-mods/placebo) (MIT): optional; RunelessRPG
  registers an affix type and six affixes when it is installed. Only compiled against their API, not bundled.
- [Apothic Attributes](https://www.curseforge.com/minecraft/mc-mods/apothic-attributes) by Shadows_of_Fire (MIT):
  optional, comes with Apotheosis. Not bundled.
- Miki's Fantasy HUD by Miki (All Rights Reserved): optional; the mana display can dock to its frames. RunelessRPG reads a
  few of its state flags by reflection; nothing of it is bundled.
- [Gearscope](https://github.com/Drehverschluss/Gearscope) by Drehverschluss (MIT): optional; shows the attribute
  icons. Not bundled.

## License

RunelessRPG is licensed under the [MIT License](LICENSE).
