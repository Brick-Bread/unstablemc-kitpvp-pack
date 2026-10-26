# UnstableMC KitPvP resource pack

The menu art for the UnstableMC KitPvP server: a dark KitPvP restyle of the 6-row chest and full-menu
overlays (kit picker, kit preview/editor, leaderboards) drawn through a font glyph in the menu title.
The server requires it (server.properties `resource-pack` + `resource-pack-sha1`).

- `src/Gen.java` draws every texture and writes the font/lang/pack files.
- `./build.sh <vanilla ascii.png>` rebuilds `dist/kitpvp-pack-<hash>.zip`. Each build gets a new file
  name, so GitHub's raw CDN can never serve a stale copy that fails the SHA-1 check.

Font `kitpvp:gui`: `` kits, `` loadout, `` leaderboards, `` = -8 px,
`` = -249 px (back to the normal title position).
