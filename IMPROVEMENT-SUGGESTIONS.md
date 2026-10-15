# Improvement suggestions — custom items, lodestones, and admin policy

## Custom item identity

Use separate PersistentDataContainer keys for Hardcore Compasses and Hardcore Lodestones:

```java
hardcore_compass
hardcore_lodestone
```

This prevents ambiguity between item types and makes validation explicit. The current single `hardcore_compass` key should be split into type-specific keys, while retaining a migration check for existing items where practical.

Recommended item metadata:

- Hardcore Compass: custom name, bound/unbound lore, plugin compass PDC tag.
- Hardcore Lodestone: custom name, description lore, plugin lodestone PDC tag.
- Placed Hardcore Lodestone: block `TileState` PDC marker plus optional owner UUID.

Vanilla compasses and vanilla lodestones should never be accepted by binding, preservation, or recipe-result logic.

## Crafting

Add independent configurable recipes for both custom items:

```yaml
recipe:
  compass:
    enabled: true
    shape:
      - " R "
      - "RCR"
      - " R "
    ingredients:
      R: REDSTONE
      C: COMPASS
  lodestone:
    enabled: true
    shape:
      - "III"
      - "ILI"
      - "III"
    ingredients:
      I: IRON_INGOT
      L: LODESTONE
```

Implementation notes:

- Use separate `NamespacedKey` values for each recipe.
- Register the compass recipe with a tagged compass result.
- Register the lodestone recipe with a tagged lodestone result.
- In `PrepareItemCraftEvent`, replace the preview result with the correctly tagged and localised item.
- Validate shape dimensions, used symbols, and material mappings before calling `Bukkit.addRecipe`.
- On invalid configuration, log a clear error and skip registration rather than throwing an exception during plugin enable.
- Re-register both recipes on `/hcompass reload`.

The crafting preview should display the custom item name and lore so players can distinguish Hardcore items from vanilla items before taking them.

## Lodestone ownership and sharing

Do not make a Hardcore Lodestone exclusive to one compass or one player. Multiple Hardcore Compasses should be able to bind to the same lodestone.

The compass’s own `CompassMeta` target is sufficient for shared use. Breaking a lodestone should untrack every compass whose stored world and block coordinates match that lodestone.

Do not allow one player’s binding action to overwrite another player’s compass target. Binding affects only the compass in the binder’s main hand.

## Lodestone break protection

Add a configurable protection mode:

```yaml
lodestone:
  break-protection: none # none | owner-only | admin-only
  untrack-compasses-on-break: true
```

- `none`: anyone can break the lodestone; recommended hardcore default.
- `owner-only`: only the placer or an admin can break it.
- `admin-only`: only administrators can break it.

Store the placer UUID in the lodestone block’s `TileState` PersistentDataContainer when the lodestone is placed. Check that UUID in `BlockBreakEvent` when `owner-only` is enabled.

When a protected break is cancelled, send a localised message and optionally play a denial sound.

## Offline compass handling

The current implementation updates only online players’ inventories and ender chests when a lodestone breaks. Offline players’ compasses cannot be updated immediately.

Recommended approach:

1. Maintain a persistent registry of known Hardcore Lodestone locations and their broken state.
2. On player join, inspect tagged compasses in that player’s inventory and ender chest.
3. If a compass points to a registered broken lodestone, set `lodestoneTracked(false)` and refresh lore in that player’s language.
4. Remove obsolete registry entries after a configurable period or when no compass references them.

This avoids storing every player’s compass state centrally while still correcting offline items on their next session.

## Death count and compass preservation

A newly crafted Hardcore Compass must not reset the player’s death count. Death count should remain a per-player progression value.

Recommended config:

```yaml
compass:
  preserve-on-first-death: true
  reset-death-count-on-new-compass: false
```

Recommended commands:

```text
/hcompass setpreserve <true|false>
/hcompass setresetoncraft <true|false>
/hcompass reset <player> lives|deaths|compass|rtp|all
```

- `preserve-on-first-death` controls whether the first death preserves a tagged Hardcore Compass.
- `reset-death-count-on-new-compass` should default to `false` to prevent players regaining first-death protection by crafting another compass.
- `reset` should be granular so admins can reset lives without deleting language preferences or RTP history.

## RTP history

Add a cap or pruning policy for stored previous spawn locations:

```yaml
teleport:
  spawn-history-limit: 10
```

Keep only the most recent entries per player. This prevents `players.yml` from growing indefinitely on long-lived servers while still allowing meaningful minimum-distance selection.

## Recipe and configuration safety

- Validate recipe shape rows, symbols, and materials before registration.
- Log invalid configuration with the affected section and expected format.
- Disable only the invalid recipe rather than preventing the entire plugin from enabling.
- Add configuration versioning and safe migration for future changes.

## Testing priorities

1. Custom compass and lodestone crafting previews show custom names and lore.
2. Vanilla items cannot participate in the Hardcore system.
3. Multiple compasses can share one Hardcore Lodestone.
4. Breaking a shared lodestone untracks all matching compasses.
5. Crafting a new compass does not reset death count or first-death preservation.
6. Lodestone protection modes behave correctly.
7. Offline compasses are corrected after the player rejoins.
8. Invalid recipe configuration does not crash plugin startup.
