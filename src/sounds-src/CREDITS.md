# Sound sources

Every file the pack's sounds are mixed from (by `sounds.sh`). All are public domain (CC0): no
attribution is required, but here they are. The freesound files in `fs/` are the HQ previews;
`fs/meta.json` has the same titles, authors and links.

| File | Source | Author | Licence |
|---|---|---|---|
| spaceEngineLarge_000, lowFrequency_explosion_000, explosionCrunch_000 | [Sci-Fi Sounds](https://kenney.nl/assets/sci-fi-sounds) | Kenney | CC0 |
| impactMetal_heavy_001 | [Impact Sounds](https://kenney.nl/assets/impact-sounds) | Kenney | CC0 |
| freesound-506313-daleonfire-falling-bomb | [Falling Bomb.wav](https://freesound.org/people/Daleonfire/sounds/506313/) (HQ preview) | Daleonfire | CC0 |
| fs/610562-BrickDeveloper171 | [Charge up 2](https://freesound.org/people/BrickDeveloper171/sounds/610562/) | BrickDeveloper171 | CC0 |
| fs/351512-nsstudios | [air raid sirens](https://freesound.org/people/nsstudios/sounds/351512/) | nsstudios | CC0 |
| fs/366091-bevibeldesign | [Huge Explosion Part 1 - Shockwave](https://freesound.org/people/bevibeldesign/sounds/366091/) | bevibeldesign | CC0 |
| fs/822677-AudioPapkin | [Sound Design Elements Impact SFX PS 100](https://freesound.org/people/AudioPapkin/sounds/822677/) | AudioPapkin | CC0 |
| fs/812709-AudioPapkin | [Sound Design Elements Impact SFX PS 043](https://freesound.org/people/AudioPapkin/sounds/812709/) | AudioPapkin | CC0 |
| fs/827562-AudioPapkin | [Sound Design Elements Impact SFX PS 103](https://freesound.org/people/AudioPapkin/sounds/827562/) | AudioPapkin | CC0 |
| fs/813053-AudioPapkin | [Big robot footstep 002](https://freesound.org/people/AudioPapkin/sounds/813053/) | AudioPapkin | CC0 |
| fs/345736-noahpardo | [NPX Deep Roar Echo 1.wav](https://freesound.org/people/noahpardo/sounds/345736/) | noahpardo | CC0 |
| fs/735168-Irolan | [Item Pickup Chime](https://freesound.org/people/Irolan/sounds/735168/) | Irolan | CC0 |
| fs/683185-NearTheAtmoshphere | [Power Up](https://freesound.org/people/NearTheAtmoshphere/sounds/683185/) | NearTheAtmoshphere | CC0 |
| fs/222493-qubodup | [Digital Data Whoosh](https://freesound.org/people/qubodup/sounds/222493/) | qubodup | CC0 |
| fs/570335-User391915396 | [Hitmarker Sound Effect](https://freesound.org/people/User391915396/sounds/570335/) | User391915396 | CC0 |
| fs/566203-colorsCrimsonTears | [Fanfare - Rpg](https://freesound.org/people/colorsCrimsonTears/sounds/566203/) | colorsCrimsonTears | CC0 |
| fs/848312-dwswords | [Glass Smash](https://freesound.org/people/dwswords/sounds/848312/) | dwswords | CC0 |
| fs/473601-CZghost | [war_horn.wav](https://freesound.org/people/CZghost/sounds/473601/) | CZghost | CC0 |
| fs/232481-bluejello13 | [RWHeart.wav](https://freesound.org/people/bluejello13/sounds/232481/) | bluejello13 | CC0 |
| fs/677859-el_boss | [Game Success Fanfare](https://freesound.org/people/el_boss/sounds/677859/) | el_boss | CC0 |
| fs/515317-basarunder | [Countdown 1.wav](https://freesound.org/people/basarunder/sounds/515317/) | basarunder | CC0 |
| fs/839832-TommyListens | [UI Click](https://freesound.org/people/TommyListens/sounds/839832/) | TommyListens | CC0 |
| fs/836200-MATUSTRM | [UI_Confirm](https://freesound.org/people/MATUSTRM/sounds/836200/) | MATUSTRM | CC0 |

No longer used, but kept for older mixes: forceField_003 and explosionCrunch_004 (Kenney, [Sci-Fi
Sounds](https://kenney.nl/assets/sci-fi-sounds), CC0), oga-rubberduck-cannon_02 (rubberduck,
[25 CC0 bang / firework SFX](https://opengameart.org/content/25-cc0-bang-firework-sfx), CC0) and
oga-aquinn-sirens (aquinn, [Sirens and Alarm Noise](https://opengameart.org/content/sirens-and-alarm-noise), CC0).

What each pack sound is made of:

- `strike/charge`: BrickDeveloper171's Charge up 2, swelled so it gets louder as it rises, over a low Kenney spaceEngineLarge_000 rumble.
- `strike/siren`: the rising wail from nsstudios' air raid sirens (4.2-7.6 s), sped up 1.2× to 2.8 s.
- `strike/impact`: AudioPapkin's Impact PS 100 + bevibeldesign's Huge Explosion shockwave reversed (so it becomes a decaying rumble tail) + Kenney lowFrequency_explosion_000.
- `strike/wither`: AudioPapkin's Impact PS 043 + Kenney explosionCrunch_000.
- `strike/mob`: noahpardo's Deep Roar Echo (1.35-4.6 s).
- `drop/incoming`: Daleonfire's falling bomb whistle.
- `drop/land`: AudioPapkin's Big robot footstep 002 + Kenney impactMetal_heavy_001 + a low-passed Kenney lowFrequency_explosion_000.
- `drop/pickup`: Irolan's Item Pickup Chime.
- `gadget/ready`: NearTheAtmoshphere's Power Up.
- `gadget/use`: one burst (3.0-3.75 s) of qubodup's Digital Data Whoosh.
- `kill/confirm`: User391915396's Hitmarker.
- `kill/streak`: colorsCrimsonTears' Fanfare - Rpg.
- `kill/shutdown`: dwswords' Glass Smash + a low-passed Kenney lowFrequency_explosion_000.
- `round/start`: CZghost's war horn (first 3.6 s).
- `round/border`: AudioPapkin's Impact PS 103 slowed to 0.8× + bevibeldesign's shockwave reversed and low-passed.
- `round/sudden`: bluejello13's RWHeart heartbeat (four beats) + a low-passed Kenney lowFrequency_explosion_000.
- `round/win`: el_boss' Game Success Fanfare.
- `ui/tick`: basarunder's Countdown 1.
- `ui/click`: TommyListens' UI Click.
- `ui/confirm`: MATUSTRM's UI_Confirm.

## Lobby music

| Event | Source | Author | Licence |
|---|---|---|---|
| `music.lobby1` | [Synthwave Loop [95 BPM]](https://freesound.org/people/LulSayer/sounds/699241/) | LulSayer | CC0 |
| `music.lobby2` | [JH Synthwave \ Synth Pop (100% loopable)](https://freesound.org/people/Johnnie_Holiday/sounds/697437/) | Johnnie_Holiday | **CC-BY 4.0**: credit required (this line, and the pack README) |
