# Improvement suggestions — updated

## Compass identity

- Track the starter/base compass by a persistent identifier, such as Persistent Data Container key, UUID, or custom model data, rather than preserving the first lodestone-bound compass found.
- Prevent players from creating an unlimited supply of preserveable compasses by binding ordinary compasses to lodestones.
- Add configurable compass name, lore, and custom model data.
- Add messages when the compass is preserved on the first death and lost thereafter.

## Zero-life handling

- Add a configurable outcome at zero lives: spectator mode, temporary ban, permanent elimination, custom command, or current respawn-only behaviour.
- Ensure the chosen zero-life outcome is clearly documented and tested.

## Relocation

- Consider moving the bounded safe-location scan off the main thread or limiting its per-tick work for busy servers.
- Add exclusion zones for claims, protected regions, other players, and dangerous generated structures.
- Log the selected destination and fallback reason at debug level to simplify support.
- Consider separating horizontal target selection from vertical safe-surface search more explicitly, and expose a maximum search area or timeout.
- Add an optional minimum distance from world spawn, other players, or prior death locations.

## Death penalties

- Keep the individual penalty toggles, and consider adding an option to keep specific items, such as the base compass, even when inventory wiping is enabled.
- Add an optional death-chest or grave system so wiped inventory is recoverable for a configurable period.
- Consider a configurable delay before relocation, or relocation on respawn, if testing reveals conflicts with vanilla respawn.

## Data and reliability

- Use atomic file writes for `players.yml` and save on server disable.
- Add optional SQLite/MySQL storage for larger servers or future multi-server setups.
- Validate configuration at startup: invalid world, minimum radius greater than maximum radius, non-positive attempts, and invalid message keys.
- Add config versioning and safe migration.

## Commands and permissions

- Add `/hcompass reset <player>` to reset lives, deaths, and compass-preservation state.
- Add `/hcompass info <player>` showing lives, deaths, and whether the first-death compass has already been used.
- Add tab completion for subcommands, player names, and numeric arguments.
- Ensure all admin commands work from console.

## Developer experience

- Add unit tests for first-death compass preservation, second-death loss, death counting, penalty toggles, and RTP fallback.
- Add a GitHub Actions workflow to build with JDK 21 and Maven.
- Publish built jars in GitHub Releases.
- Add a changelog and semantic versioning.
