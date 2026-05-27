# HandySpawn

Surfaces Minecraft's hidden spawn-related information — personal spawn markers, phantom insomnia countdown, daytime-thunderstorm sleep hint, destroyed-spawn ghost marker, world-spawn marker, and forceloaded-chunk indicators.

Client+server Fabric mod, currently tracking **Minecraft 26.2-pre-1**. Install on both sides for full features. Vanilla servers fall back gracefully — the client continues to track world spawn and keeps the phantom / thunder / world-spawn features working.

## Features

All toggleable via ModMenu → HandySpawn → Settings.

### Personal spawn (server-synced)
- **House icon** above your current spawn bed or charged respawn anchor
- **Sleep-readiness tint** — beds glow green when sleeping works right now (night or thunderstorm)
- **Destroyed-spawn ghost marker** — red marker at the coords where your spawn used to be

### Phantom awareness (client-only)
- **Phantom insomnia HUD** — countdown in the final in-game day before phantoms start, then an "active danger" indicator until you sleep
- **Daytime-thunderstorm hint** — small icon when the non-obvious daytime sleep window is active

### Navigation (server-synced)
- **Cross-dimension spawn readout** — top-left HUD line showing spawn dimension and integrity when you're away from it

### World (server-synced where applicable)
- **World-spawn marker** — glyph at the exact world-spawn block (overworld)
- **Forceloaded-chunk badge** — HUD badge when standing in a forceloaded chunk
- **Forceload chunk-border overlay** — optional wireframe around forceloaded chunks

## Install

One jar, works on both sides. Drop `handyspawn-0.3.0-beta.4.jar` in the `mods/` folder of your client AND your server. For singleplayer, the client install is enough (the integrated server runs the server component automatically).

## Build

```
./gradlew build
```

Jar appears under `build/libs/`.

## Dependencies

- Minecraft 26.1.2 + Fabric Loader 0.19.2
- Fabric API 0.146.1+26.1.2
- YACL 3.9.2+26.1-fabric (optional, for the config screen)
- ModMenu 18.0.0-alpha.8 (optional, for the config screen entry)
- Java 25

## License

MIT. See `LICENSE`.
