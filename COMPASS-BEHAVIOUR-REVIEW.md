# Compass behaviour review — updated

Reviewed against commit `197e4036fb49a031bc05e6bd90addbc7bc45948f`.

## Intended behaviour

- On first join, the player receives a compass.
- Once bound to a lodestone, it becomes the player’s base compass.
- The compass is preserved only on the player’s **first death**.
- From the second death onwards, it is not preserved.
- On every death, the player is relocated to a random safe location within the configured radius, subject to configuration.

## What has been fixed

The death listener now tracks a death count and preserves the compass only on the first death:

```java
int deaths = lives.deaths(p.getUniqueId()) + 1;
boolean preserve = deaths == 1 && bound != null
        && lives.markCompassPreserved(p.getUniqueId());
int remaining = lives.recordDeath(p.getUniqueId());

if (preserve) {
    e.getDrops().add(bound);
}
```

This matches the requested rule: with the default two lives, the compass survives the first death but is lost on the second and every later death.

The death penalties are now configurable:

- Inventory drops can be cleared with `wipeInventory`.
- XP can be cleared with `clearXp`.
- The ender chest can be cleared with `clearEnderChest`.
- Bed spawn can be cleared with `clearBedSpawn`.
- Relocation can be enabled or disabled with `enableRtp`.

The relocation system now uses the configured `attempts` value, retries safe-location searches, and falls back to the world spawn area if no safe destination is found. The final teleport uses Paper’s async teleport API.

## Current assessment

The core requested behaviour is now implemented correctly.

| Death | Compass | Relocation |
|---|---|---|
| First | Preserved, if a lodestone-bound compass is present | Yes, if RTP is enabled |
| Second and later | Not preserved | Yes, if RTP is enabled |
| Zero-life death | Not preserved | Yes, if RTP is enabled |

## Remaining checks

- Confirm that `markCompassPreserved` cannot be bypassed by a player dying twice in rapid succession or by a server restart between deaths.
- Test that the first-death preservation works when the player has multiple lodestone-bound compasses; currently `findBoundCompass` selects one bound compass.
- Test relocation after death on the exact production Purpur build, especially whether the async teleport occurs at the desired point relative to vanilla respawn handling.
- Test the world-spawn fallback when all RTP attempts fail.
- Verify that RTP disabled still leaves normal vanilla respawn behaviour intact.
- Verify all configurable penalties independently: inventory, XP, ender chest, bed spawn, and relocation.
