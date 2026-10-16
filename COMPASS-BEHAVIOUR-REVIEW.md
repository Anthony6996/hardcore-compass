# HardcoreCompass — review against EXPECTED_BEHAVIOUR.txt

Reviewed against `EXPECTED_BEHAVIOUR.txt` and the latest `main` implementation.

## Status summary

| Area | Status | Notes |
|---|---|---|
| Separate Hardcore Compass / Lodestone identity | Implemented | Separate PDC keys and type checks exist. |
| Vanilla compass rejection | Implemented | `isHardcoreCompass` gates binding. |
| Hardcore-only lodestone binding | Partially implemented | Placement marking exists; persistent registry lifecycle needs verification. |
| Custom item names and lore | Implemented | Localised names and status lore are present. |
| Configurable separate recipes | Implemented | Compass and lodestone recipes use separate keys and configuration sections. |
| Safe recipe validation | Implemented | Invalid recipes log an error and allow startup to continue. |
| Shared lodestones | Implemented | Each compass stores its own target; one binding does not overwrite another. |
| Break protection modes | Implemented | `none`, `owner-only`, and `admin-only` are supported. |
| First-death-only compass preservation | Implemented, needs semantic testing | Death count gates preservation; inventory-retention semantics require testing. |
| Persistent lodestone registry | Partially implemented | Must be verified across restart, chunk unload, world reload, and block replacement. |
| Minimum lodestone separation | Not implemented | `EXPECTED_BEHAVIOUR.txt` explicitly identifies this as outstanding. |
| Explosion handling | Not fully implemented | Creeper, TNT, bed, and respawn-anchor destruction paths need explicit handling and tests. |
| Anvil labels | Partially implemented | Recent commits add label support; end-to-end persistence and compass lore need testing. |
| Compass glint override | Partially implemented | Recent commit adds glint override; active/lost/unbound transitions need testing. |
| Offline compass refresh | Implemented | Join refresh checks whether the compass target is still a registered Hardcore Lodestone. |
| RTP and fallbacks | Implemented | Safe-surface search, retries, border clamping, and spawn fallback exist. |

## Core gameplay loop

The intended loop is now correctly represented:

1. Player receives or crafts a Hardcore Compass and Hardcore Lodestone.
2. Player places the Hardcore Lodestone and binds the compass to it.
3. On the first death, one carried Hardcore Compass is preserved.
4. On the second and later deaths, no Hardcore Compass is preserved.
5. Every death can trigger RTP within the configured radius.
6. Breaking or destroying the target lodestone untracks affected compasses.

The default configuration matches this intent:

```yaml
lives:
  starting: 2

compass:
  preserve-on-first-death: true
  reset-death-count-on-new-compass: false

penalties:
  wipe-inventory: true
  clear-ender-chest: true
  clear-xp: true
  clear-bed-spawn: true
  enable-rtp: true
```

## High-priority gap: compass-only preservation

The current first-death logic uses:

```java
event.getItemsToKeep().add(preservedCompass.clone());
event.setKeepInventory(true);
```

`EXPECTED_BEHAVIOUR.txt` requires that only the selected Hardcore Compass is retained, not the player’s whole inventory. `setKeepInventory(true)` may retain all inventory depending on Paper’s `PlayerDeathEvent` semantics.

Recommended approach:

1. Do not enable global keep-inventory.
2. Clear `event.getDrops()` when inventory wiping is enabled.
3. Add exactly one cloned Hardcore Compass to `event.getItemsToKeep()`.
4. Test on the target Paper/Purpur build that no other inventory item is retained.
5. If Paper retains the full inventory, remove non-compass items from drops or use a respawn-stage restoration approach instead.

The acceptance test is:

```text
Carry: Hardcore Compass + diamond + dirt.
Die with inventory wipe enabled.
Expected: only the Hardcore Compass is retained; diamond and dirt are lost.
```

## High-priority gap: persistent lodestone registry

A placed lodestone cannot rely on item metadata or transient block metadata. The registry should be a plugin-owned persistent store keyed by:

```text
world UUID + block X + block Y + block Z
```

Each entry should contain:

```yaml
<world-uuid>:
  "x,y,z":
    owner: <player-uuid>
    label: "base_1"
    created: <epoch-millis>
```

Requirements:

- Register only after a successful, non-cancelled `BlockPlaceEvent`.
- Register only when the placed item has the `hardcore_lodestone` PDC marker.
- Validate on lookup that the block is still `Material.LODESTONE`.
- Remove or ignore stale entries when the block has been replaced or removed.
- Persist across restart, chunk unload, and world reload.
- Use world UUID, not world name alone, to distinguish dimensions.

## High-priority gap: minimum separation

Add configurable separation enforcement:

```yaml
lodestone:
  min-separation-enabled: true
  min-separation: 750
```

Recommended rules:

- Use horizontal X/Z Euclidean distance.
- Compare only against lodestones in the same world.
- Reject distances strictly below 750 blocks.
- Accept exactly 750 blocks or greater.
- Cancel `BlockPlaceEvent` before the item is consumed.
- Send a localised rejection message including the required distance.

## High-priority gap: explosions

Handle destruction consistently across:

- `BlockBreakEvent`
- `EntityExplodeEvent`
- `BlockExplodeEvent`
- Bed and respawn-anchor explosions where Paper exposes them

For every registered Hardcore Lodestone in the affected block list:

1. Remove it from the registry.
2. Suppress the normal lodestone drop.
3. Drop exactly one tagged Hardcore Lodestone, preserving its label and PDC marker.
4. Notify online compass holders whose target matches the destroyed location.
5. Set affected compasses to `tracked=false`, apply lost lore, and disable glint.
6. Ensure idempotency so duplicate event paths do not create duplicate drops or notifications.

Destruction notices must not include coordinates or the breaker’s identity.

## Anvil labels and compass glint

Anvil renaming should preserve the `hardcore_lodestone` PDC marker. When placed, the custom name should become the registered location label.

The compass should store the target label, recommended PDC key:

```text
target_label
```

Compass glint should use Paper’s enchantment-glint override:

- Enabled while tracking an active registered Hardcore Lodestone.
- Disabled when unbound.
- Disabled when the target is destroyed, replaced, or unregistered.

Do not rely on an actual enchantment to create the glint.

## Acceptance tests

1. Give both custom items using `/hcompass give`, place the lodestone, restart, and confirm the exact world UUID and coordinates remain registered.
2. Place a vanilla lodestone, including one renamed to look custom; confirm a Hardcore Compass refuses it and remains unchanged.
3. Bind a Hardcore Compass, then relog and restart; confirm target, PDC, label, lore, tracked state, and glint persist.
4. Test main-hand and off-hand interaction; confirm no duplicate processing or off-hand mutation.
5. Rename a lodestone in an anvil, place it, bind to it, break it, pick it up, replace it, and confirm the label survives and appears in compass lore.
6. Test lodestone placement inside and outside the 750-block separation boundary; confirm no item loss on rejection.
7. Test break protection as owner, non-owner, and admin under all three modes.
8. Test creeper, TNT, bed, and respawn-anchor destruction; confirm exactly one custom drop per affected registered lodestone.
9. Confirm all online compass holders receive the target-destroyed notice, lose glint, and show lost lore.
10. Take a Hardcore Compass offline, destroy its lodestone, rejoin, and confirm the compass refreshes to lost state.
11. Die once with a Hardcore Compass and other items; confirm only the compass is preserved.
12. Die a second time; confirm no compass preservation.
13. Test RTP in normal, ocean, lava, void-edge, border, and no-safe-surface scenarios.
14. Test English and Spanish messages, item lore, and fallback behaviour.
15. Enable/disable each recipe independently and submit malformed recipe configurations; confirm useful logs and continued startup.
