# Changelog

## 0.3.0

Updated for the **Minecraft 26.2** full release.

- Rebuilt against Fabric API **0.152.1+26.2** and Fabric Loader **0.19.3**.
- YACL updated to **3.9.4+26.2-fabric** and ModMenu to **20.0.0-beta.2**; with YACL's stable 26.2 build out, the in-game config screen is back in dev and live play.
- Now declares compatibility with `26.2` and future `26.2.x` point releases.

## 0.3.0-beta.4

- Tracks Minecraft **26.2-pre-1** (first prerelease of the 26.2 cycle); skips snapshots 6, 7, and 8 since the snapshot-5 rendering refactor (SubmitNodeCollector + `mainCamera()`) carried forward untouched.
- Rebuilt against Fabric API **0.149.2+26.2**.
- YACL bumped to **3.9.3+26.2-fabric** and ModMenu to **19.0.0-alpha.1** on the compile classpath. Neither has a build that explicitly lists `26.2-pre-1` in its game_versions yet, so they remain `compileOnly` — the in-game config screen returns to dev runtime once either ships a matching build.

## 0.3.0-beta.3

### Fixed
- `HandySpawnConfig` no longer crashes at startup when YACL is not installed; the mod now boots on in-memory defaults and picks up persisted settings the moment YACL is added.
- Reset-to-default in the config screen now respects each option's real default; previously `forceloadBorderOverlay` flipped to `true` on reset.
- Tightened `depends.minecraft` floor to `>=26.2-alpha.1` so the mod won't half-load on a 26.1 server (where the 26.2-only render APIs would crash at first frame).

### Changed
- Config moved from `<configDir>/handyspawn.json` to `<configDir>/handyspawn/config.json` to share one subdir with the spawn-state files. Existing `handyspawn.json` is migrated automatically on first launch.
- `BedTintRenderer` (sleep-ready Zzz glyph) now caches its bed scan across frames, refreshing only when the player drifts >4 blocks or every 500 ms — much cheaper at high view distance.

## 0.3.0-beta.2

### Internal (no user-facing impact)
- Java package moved from `dev.handyspawn` to `dev.handy.mods.handyspawn` to match the rest of the Handy series convention.
- Mixin config renamed from `handyspawn.main.mixins.json` to `handyspawn.mixins.json` (drops the `.main.` infix; aligns with sibling mods).

## 0.2.0 — 2026-04-18

### New
- **Server-side component.** Install on both client and server for full features. Vanilla servers fall back to world-spawn-only behaviour (client logs a one-time info message).
- **Real personal-spawn tracking.** F1 (house icon), F3 (destroyed marker), F6 (cross-dim readout) now show your actual bed/anchor spawn when the server has HandySpawn.
- **Multiplayer forceload support.** F8 (badge) and F9 (border overlay) work on any server with HandySpawn, not just singleplayer.

### Changed
- Renamed internal state classes `WorldSpawn*` → `CompassTarget*` for clarity.
- F7 (world-spawn marker) and F1 (personal spawn marker) are now genuinely distinct on HandySpawn-aware servers.
- Dropped the "Visible through walls" toggle — F1's depth-tested rendering is the default and only mode.
- Config string "(singleplayer)" suffix removed from F8/F9 — they now work multiplayer too.

### Known limitations
- Client-only install still works, but F1/F3/F6 fall back to tracking world spawn (same as 0.1.1) and F8/F9 go silent.
- Respawn-anchor charge-level visualisation is deferred to a future release.
- Griefing alerts / spawn-destruction notifications are deferred.
- Chat messages on spawn set/destroyed are vanilla-only; no localisation overrides.

## 0.1.1 — 2026-04-18

### Fixed / clarified
- Separated the world-spawn data layer naming from the language of "personal spawn".
  Investigation of MC 26.1.2 confirmed that `ClientboundSetDefaultSpawnPositionPacket`
  is only ever sent by `MinecraftServer.setRespawnData` (login and `/setworldspawn`,
  broadcast to all players). Personal bed and respawn-anchor spawns are stored
  server-side on `ServerPlayer.respawnConfig` and are **not** synced to the client
  in any packet, so it is not possible to distinguish personal spawn from world
  spawn on the client.
- Renamed `SpawnState*` to `WorldSpawnState*` (`SpawnRecord`, `SpawnKind`,
  `SpawnPersistence`, `SpawnStateController`) to match what the data actually
  represents. `WorldSpawnRenderer` (F7) now reads from the shared controller
  state instead of re-reading `Level.getRespawnData()` directly, giving a single
  source of truth for F1/F3/F6/F7.

### Known limitations (updated and honest)
- The F1 house icon, F3 ghost marker, F6 cross-dimension readout and F7 world-spawn
  marker all reflect the **world spawn** as broadcast by the server. Distinguishing
  a per-player bed/anchor respawn on the client would require a mixin that snoops
  successful bed interactions or chat messages; deferred to a future release.
- Through-walls rendering falls back to depth-tested rendering (polish task).
- Forceload visuals (F8/F9) are singleplayer-only because the forceload chunk set
  is not synced to clients on vanilla servers.

## 0.1.0 — 2026-04-18

Initial release.

### Features
- Personal spawn marker (house icon on current spawn bed or charged respawn anchor)
- Sleep-readiness bed tint (green when sleeping is possible right now)
- Destroyed-spawn ghost marker at former spawn coordinates
- Phantom insomnia HUD countdown (warning phase + active-danger phase)
- Daytime-thunderstorm sleep hint
- Cross-dimension spawn readout
- World-spawn block marker
- Forceloaded-chunk HUD badge (singleplayer)
- Optional forceload chunk-border overlay (singleplayer)
- YACL configuration screen with four categories
- ModMenu integration

### Target
- Minecraft 26.1.2
- Fabric Loader 0.19.2
- Fabric API 0.146.1+26.1.2
- Java 25

### Known limitations
- See consolidated notes under 0.1.1.
