# HandySpawn

Surfaces Minecraft's hidden spawn-related information — personal spawn markers, phantom insomnia countdown, daytime-thunderstorm sleep hint, destroyed-spawn ghost marker, world-spawn marker, and forceloaded-chunk indicators.

Client-side Fabric mod for Minecraft 26.1.2. No server-side component. Features tied to `/forceload` are singleplayer-only in v1 (the forceload chunk set isn't synced to clients on vanilla servers).

## Features

All toggleable via ModMenu → HandySpawn → Settings.

### Personal spawn
- **House icon** above your current spawn bed or charged respawn anchor (with optional through-walls visibility)
- **Sleep-readiness tint** — beds glow green when sleeping works right now (night or thunderstorm)
- **Destroyed-spawn ghost marker** — red marker at the coords where your spawn used to be

### Phantom awareness
- **Phantom insomnia HUD** — countdown in the final in-game day before phantoms start, then an "active danger" indicator until you sleep
- **Daytime-thunderstorm hint** — small icon when the non-obvious daytime sleep window is active

### Navigation
- **Cross-dimension spawn readout** — top-left HUD line showing spawn dimension and integrity when you're away from it

### World
- **World-spawn marker** — subtle glyph at the exact world-spawn block (overworld)
- **Forceloaded-chunk badge** — HUD badge when standing in a forceloaded chunk (singleplayer)
- **Forceload chunk-border overlay** — optional wireframe around forceloaded chunks (singleplayer)

## Build

```
./gradlew build
```

Jar under `build/libs/handyspawn-0.1.0.jar`.

## Dependencies

- Minecraft 26.1.2 + Fabric Loader 0.19.2
- Fabric API 0.146.1+26.1.2
- YACL 3.9.2+26.1-fabric (optional, for the config screen)
- ModMenu 18.0.0-alpha.8 (optional, for the config screen entry)
- Java 25

## License

MIT. See `LICENSE`.
