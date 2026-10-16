# Improvement suggestions — expected-behaviour implementation plan

## Priority 1: compass-only preservation

The expected behaviour requires that a first death preserves only one Hardcore Compass, not the player’s entire inventory.

Current risk:

```java
event.getItemsToKeep().add(preservedCompass.clone());
event.setKeepInventory(true);
```

`setKeepInventory(true)` can retain the full inventory depending on Paper’s handling of `PlayerDeathEvent`.

Recommended implementation:

```java
if (config.wipeInventory()) {
    event.getDrops().clear();
}

if (preserve) {
    event.getItemsToKeep().add(preservedCompass.clone());
}
```

Do not call `event.setKeepInventory(true)` unless testing proves it is required and does not retain unrelated items.

If Paper still retains the full inventory, use this alternative:

1. Capture all non-compass items from the player inventory.
2. Remove them from the inventory or drops.
3. Retain only the selected Hardcore Compass.
4. Restore no other items.

Add an integration test:

```text
Inventory: Hardcore Compass, diamond, dirt.
Death: first death, inventory wipe enabled.
Expected: Hardcore Compass retained; diamond and dirt lost.
```

## Priority 2: persistent lodestone registry

Create a dedicated `LodestoneRegistry` rather than relying on block PDC or transient metadata.

Suggested storage file:

```text
plugins/HardcoreCompass/lodestones.yml
```

Suggested structure:

```yaml
lodestones:
  <world-uuid>:
    "x,y,z":
      owner: "<player-uuid>"
      label: "base_1"
      created: 1760000000000
```

Required API:

```java
public boolean isRegistered(Location location);
public Optional<LodestoneRecord> get(Location location);
public void register(Location location, UUID owner, String label);
public boolean unregister(Location location);
public Collection<LodestoneRecord> allInWorld(World world);
```

Implementation rules:

- Key by world UUID and integer block coordinates.
- Register only from a successful, non-cancelled `BlockPlaceEvent`.
- Register only when the placed item has the `hardcore_lodestone` PDC marker.
- Validate on lookup that the block is still `Material.LODESTONE`.
- Clean stale entries when a block has been replaced or removed.
- Save safely using atomic file replacement.
- Keep the registry independent from chunk loading.

## Priority 3: minimum lodestone separation

Add configuration:

```yaml
lodestone:
  min-separation-enabled: true
  min-separation: 750
```

In `BlockPlaceEvent`, before the item is consumed:

```java
if (config.lodestoneMinSeparationEnabled()) {
    double required = config.lodestoneMinSeparation();

    for (LodestoneRecord existing : registry.allInWorld(world)) {
        double distance = horizontalDistance(existing.location(), placedLocation);

        if (distance < required) {
            event.setCancelled(true);
            player.sendMessage(config.getMessage(player, "lodestone-too-close"));
            return;
        }
    }
}
```

Use horizontal distance:

```java
double dx = a.getX() - b.getX();
double dz = a.getZ() - b.getZ();
double distance = Math.sqrt(dx * dx + dz * dz);
```

Only compare lodestones in the same world. Reject distances strictly below the configured threshold; accept exactly 750 blocks or more.

## Priority 4: explosions and destruction

Create one shared method:

```java
public void destroyLodestone(World world, Location location)
```

It should:

1. Check the registry.
2. Remove the registry entry.
3. Suppress the normal block drop.
4. Drop exactly one custom Hardcore Lodestone, preserving PDC and label.
5. Notify online compass holders.
6. Mark matching compasses untracked.
7. Update compass lore and disable glint.
8. Be idempotent.

Handle these events:

```java
@EventHandler
public void onBlockBreak(BlockBreakEvent event)

@EventHandler
public void onEntityExplode(EntityExplodeEvent event)

@EventHandler
public void onBlockExplode(BlockExplodeEvent event)
```

For explosions, inspect the affected block list, remove registered Hardcore Lodestone blocks from the vanilla drop list, and add exactly one custom lodestone item per destroyed lodestone.

Do not send coordinates or breaker identity in the player-facing destruction message.

## Priority 5: anvil labels

Store the custom anvil name on the Hardcore Lodestone item and carry it into the registry when placed.

Recommended PDC keys:

```text
hardcore_lodestone
hardcore_lodestone_label
```

On placement:

```java
String label = item.getItemMeta().getDisplayName();
registry.register(location, player.getUniqueId(), label);
```

On compass binding:

```java
meta.getPersistentDataContainer().set(
    targetLabelKey,
    PersistentDataType.STRING,
    registry.get(location).label()
);
```

Bound compass lore should include:

- Target label, if present.
- Status text.
- Coordinates, if desired.

The target-destroyed message must not include coordinates or breaker identity.

## Priority 6: glint state

Use Paper’s enchantment-glint override rather than adding a real enchantment.

State rules:

| Compass state | Tracked | Glint |
|---|---|---|
| Unbound | false | Off |
| Bound to active Hardcore Lodestone | true | On |
| Target destroyed or unregistered | false | Off |

Refresh glint and lore:

- On binding or rebinding.
- On lodestone destruction.
- On player join.
- After server restart.
- After registry changes.

## Priority 7: commands and admin policy

Add granular reset commands:

```text
/hcompass reset <player> lives
/hcompass reset <player> deaths
/hcompass reset <player> compass
/hcompass reset <player> rtp
/hcompass reset <player> all
```

Add admin policy commands:

```text
/hcompass setpreserve <true|false>
/hcompass setresetoncraft <true|false>
/hcompass setlodestoneprotection <none|owner-only|admin-only>
/hcompass setminseparation <blocks>
```

All administrative commands should:

- Require `hardcorecompass.admin`.
- Work from console where sensible.
- Save changes to `config.yml`.
- Send localised confirmation messages.
- Appear in admin help and tab completion.

## Priority 8: RTP history

The current `spawn-history-limit: 10` is a good starting point. Ensure that:

- Only the most recent 10 spawn records are retained per player.
- Records are compared using horizontal X/Z distance.
- Records are scoped to the same world.
- The history survives restart.
- Invalid or stale world entries are removed safely.

## Testing priorities

1. Compass-only preservation on first death.
2. No preservation on second or later deaths.
3. Persistent lodestone registry across restart and relog.
4. Vanilla lodestone rejection.
5. Shared lodestone support.
6. Break-protection modes.
7. Explosion destruction and duplicate-drop prevention.
8. Anvil label persistence.
9. Glint transitions.
10. Minimum separation enforcement.
11. RTP fallback safety.
12. English/Spanish localisation.
