# Compass behaviour review

## Intended behaviour

- On first join, the player receives a compass.
- After the player binds that compass to a lodestone, it becomes their base compass.
- On the player's **first death only**, the lodestone-bound compass should be preserved in their death drops.
- From the **second death onwards**, the compass should not be preserved.
- On **every** death, the player should be relocated to a random safe location within the configured radius.

## Current behaviour

`PlayerDeathListener` currently preserves the bound compass on every death while the player has lives remaining:

```java
if (remaining > 0 && bound != null) {
    e.getDrops().add(bound);
}
```

With the default of 2 starting lives, this means:

| Death | Lives before | Compass after death | Relocated? |
|---|---:|---|---|
| First | 2 | Kept | Yes |
| Second | 1 | Kept — does not match intended design | Yes |
| Third / zero-life | 0 | Lost | Yes |

The relocation call is already made on every death:

```java
teleport.relocate(p);
```

So the relocation requirement matches the intended design, but the compass-retention rule does not.

## Recommended fix

Track a separate `deaths` count for each player UUID in `players.yml`, alongside the existing lives value. Then preserve the compass only when the death being processed is the player's first death.

Example logic:

```java
int deaths = lives.incrementDeaths(p.getUniqueId());
boolean firstDeath = deaths == 1;

if (firstDeath && bound != null) {
    e.getDrops().add(bound);
}
```

This is preferable to using `remaining > 1`, because it keeps the rule correct even if an administrator changes a player's starting or current lives with `/hcompass setlives`.

## Storage change

Add a deaths field to the persisted player data, for example:

```yaml
players:
  <uuid>:
    lives: 2
    deaths: 0
```

On death:

1. Load or initialise the player's UUID record.
2. Increment `deaths`.
3. Save the updated record.
4. Use `deaths == 1` to decide whether to preserve the bound compass.

## Testing checklist

- New player joins and receives the starter compass.
- Player binds the compass to a lodestone.
- Player dies for the first time: compass should remain available in drops; player should be relocated.
- Player dies a second time: compass should not be preserved; player should be relocated.
- Player dies at zero lives: no compass preservation; player should still be relocated.
- Admin sets lives to 5, then the player dies: only the player's first recorded death should preserve the compass.
- Player dies without a lodestone-bound compass: no compass is preserved.
- Relocation succeeds or has a clear, safe fallback when no safe surface is found.
