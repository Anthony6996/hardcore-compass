# Improvement suggestions

## Gameplay

- Add configurable behaviour for players at zero lives: spectator mode, temporary ban, permanent elimination, or a custom command.
- Consider making the first-death compass preservation configurable, so servers can choose whether the compass survives the first death, all deaths, or no deaths.
- Add an optional cooldown or cost before a player can use `/hcompass lives`, if intended for players rather than admins.
- Consider giving a configurable message when the compass is preserved or lost, so players understand the hardcore rule.
- Add support for a configurable compass display name, lore, or custom model data so the starter compass is visually distinct.

## Relocation

- Implement or remove `teleport.attempts`; currently it is documented but unused.
- Define explicit fallback behaviour when no safe location is found: retry later, use world spawn, use a fixed fallback location, or leave the player at the normal respawn point.
- Move expensive block scanning off the main thread where practical, or cache safe spawn candidates.
- Add a minimum distance from other players or claimed/protected regions if the server uses protection plugins.
- Log relocation failures with the player UUID, target coordinates, and reason to make support easier.
- Add a config option to prevent relocation into generated but unsafe areas such as ocean monuments, deep dark, lava lakes, or the Void.

## Death handling

- Re-check the death/respawn timing on the exact Paper/Purpur build used in production. Relocating during `PlayerDeathEvent` can interact unexpectedly with vanilla respawn handling.
- Consider performing relocation on `PlayerRespawnEvent` instead, or schedule it one tick later, if testing shows respawn-location conflicts.
- Make clearing the ender chest optional; some servers may want hardcore death to clear inventory and XP but preserve ender-chest contents.
- Add configurable toggles for each death penalty: drops, XP, ender chest, bed spawn, and relocation.
- Preserve only the intended starter/base compass, rather than the first arbitrary lodestone-bound compass found in inventory.

## Data and reliability

- Use atomic writes for `players.yml`, for example write to a temporary file and then move it into place, to reduce corruption risk on crashes.
- Add automatic backups or a save-on-disable hook for player data.
- Consider SQLite or MySQL as optional storage for larger servers or future multi-server support.
- Validate configuration values at startup: negative radii, minimum radius greater than maximum radius, invalid world names, and invalid language keys.
- Add a version field to `config.yml` and migrate old configs safely when defaults change.

## Commands and permissions

- Add tab completion for player names, subcommands, and life amounts.
- Add `/hcompass reset <player>` to reset lives and death count.
- Add `/hcompass info <player>` showing lives, deaths, and whether a base compass has been preserved.
- Consider a console-compatible version of every administrative command.
- Document whether `hardcorecompass.lives` should remain default-true; it may be preferable to make player lookup default-true but restrict administrative actions to `hardcorecompass.admin`.

## Developer experience

- Add unit tests for lives consumption, death counting, compass preservation, and configuration validation.
- Add a GitHub Actions workflow to build the jar with JDK 21 and Maven on every push or pull request.
- Publish release jars through GitHub Releases so server owners do not need to build from source.
- Add a `plugin.yml` example and a complete default `config.yml` to the README.
- Add a changelog and semantic versioning for future releases.
