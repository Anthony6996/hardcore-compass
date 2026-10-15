# HardcoreCompass

A Java 21 plugin for Paper/Purpur 1.21.1. Server target verified on the user's PebbleHost instance: `purpur-1.21.1.jar`, server version history `1.21.1-2329-803bf62 (MC: 1.21.1)`, configured Java 21.

## Features

- Persistent UUID-based lives in `plugins/HardcoreCompass/players.yml` (default 2).
- Optional first-join lodestone and compass items.
- On death: clears drops/XP, ender chest, XP levels, and bed spawn, consumes one life; carries the first lodestone-bound compass into the survivor's drops while lives remain.
- Random horizontal relocation around configured world's spawn, within configured min/max radius, with border clamping and safe-surface checks. Teleport uses Paper's `teleportAsync`.
- Optional reduced debug information (set true for currently online players, joins, and respawns).
- English/Spanish configurable messages; `/hcompass` commands.

## Build

Install JDK 21 and Maven 3.9+, then run `mvn clean test package`. The Paper API is a provided dependency; output jar is `target/hardcore-compass-1.0.0.jar`. No server jar is bundled. Install the jar into `plugins/` and restart the server.

## Commands and permissions

| Command | Permission | Description |
|---|---|---|
| `/hcompass lives` | `hardcorecompass.lives` (default true; currently command implementation allows own lookup) | Show own lives |
| `/hcompass lives <player>` | `hardcorecompass.admin` | Show online player's lives |
| `/hcompass setlives <player> <amount>` | `hardcorecompass.admin` | Set online player's lives (nonnegative) |
| `/hcompass reload` | `hardcorecompass.admin` | Reload config |

## Configuration

`src/main/resources/config.yml` documents defaults. `lives.starting` defaults 2; `lives.debug-info` enables reduced debug info. `world` is destination world. `teleport.min-radius` / `max-radius` bound radial distance; `attempts` is reserved for search policy; `search-radius` bounds local safe-block search; `border-margin` prevents border-edge targets; avoid-water/lava/void control checks. Starter items can each be disabled. Set `messages.language` to `en` or `es`; customize keys and placeholders `{lives}`, `{player}`.

## Layout

- `listener`: join, death, respawn event handling
- `manager`: lives, compass-item and teleport behavior
- `storage`: UUID YAML persistence
- `config`: configuration access
- `command`: administrative/player command

## Important behavior and limitations

A death consumes one life; at zero, the player remains able to respawn and rejoin but stays at zero lives. The current code does not ban/eliminate zero-life players. Death events clear event drops and clear the ender chest immediately. Java/Paper world block inspection is scheduled on the main server thread; teleport is asynchronous. Relocation occurs from death handling and may overlap the vanilla respawn transition; validate this timing on the target server before production use. Search is bounded and may fail to relocate if no safe surface is found near the chosen target. Custom-bound compass preservation is based on finding a compass with a lodestone binding in inventory. Starter compass is unbound until a lodestone is assigned in-game. `teleport.attempts` is currently a configuration placeholder and not used; remove or implement before relying on it.

## Auditing

This repository aims to keep Bukkit API calls in listeners/managers, uses UUID keys for persistence, and has no runtime third-party dependencies. Review asynchronous API usage and death/respawn timing for the server's exact Purpur build before production deployment.
