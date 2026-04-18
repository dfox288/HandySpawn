# Changelog

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
