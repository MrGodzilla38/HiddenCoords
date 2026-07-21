# Hidden Coords

A lightweight **client-side Fabric mod** that displays your coordinates, dimension, and biome in a separate desktop window — completely outside Minecraft's own game window.

## Why?

On SMP servers, revealing your coordinates on stream is a real problem: anyone watching can pinpoint your base, farms, or current location the moment your F3 screen shows up on camera. Hidden Coords solves this by rendering the info in its own native OS window instead of inside the game.

Since OBS "Game Capture" / "Window Capture" only grabs the Minecraft window, this overlay window simply never appears on stream — you can keep it open on a second monitor, or drag it off-screen, and still glance at your coordinates whenever you need them.

## Who is this for?

- **Streamers and content creators** who don't want to expose their base location, secret builds, or current position while broadcasting.
- **SMP / anarchy server players** who want to check their coordinates privately without opening the in-game debug screen (F3) on a shared or recorded screen.
- Anyone who just wants a small, always-available coordinate readout without cluttering their HUD.

## Features

- 🪟 Coordinates shown in a **real, separate desktop window** (Java Swing) — not an in-game overlay, so screen/window capture software won't pick it up.
- ⌨️ Toggle the window on/off with **F6** by default (fully rebindable in Minecraft's Controls menu).
- 📍 Displays **X / Y / Z**, current **dimension** (Overworld / Nether / End), and current **biome**.
- 💾 Remembers the window's position and size between sessions.
- 🖥️ Optional "Always on Top" behavior.
- 🛡️ Fails safely: if no display/graphics environment is available (e.g. headless setups) or the window can't be created, the mod simply disables the overlay and logs a warning instead of crashing the game.
- ✅ 100% client-side — no server-side installation required, safe to use on any server.

## Supported versions

- **Minecraft:** `1.21` – `1.21.11` (Fabric)
- **Fabric Loader:** `0.16.0+`
- **Fabric API:** required
- **Java:** `21+`

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version.
2. Drop the `hidden-coords-*.jar` file into your `mods` folder.
3. Launch the game.

## Usage

- Press **F6** to show or hide the coordinate window.
- Move and resize the window anywhere you like — its position is saved automatically.
- The window automatically hides when you're not in a world (e.g. at the main menu) and reappears when you rejoin.

## Platform notes

Works on Windows, macOS, and Linux. On Linux, the window renders through XWayland when running under Wayland sessions — this works out of the box in most desktop environments. "Always on Top" behavior may vary depending on your window manager/compositor.

## Building from source

```bash
./gradlew build
```

The compiled mod jar will be located in `build/libs/`.

## License

See [LICENSE](LICENSE) for details.
