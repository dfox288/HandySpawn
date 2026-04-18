# Changelog

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
- World-spawn marker (F7) currently reads the same `Level.getRespawnData()` source as the personal-spawn marker (F1); when the player has set a bed spawn, both features resolve to the same position. Distinguishing true world spawn from personal respawn on the client requires snapshotting the initial `ClientboundSetDefaultSpawnPositionPacket` — deferred to a future release.
- Through-walls rendering for the personal-spawn marker currently falls back to depth-tested rendering; a dedicated no-depth `RenderType` variant is a polish task.
- Feature 8/9 (forceload visuals) are singleplayer-only because the forceload chunk set is not synced to clients on vanilla servers.
