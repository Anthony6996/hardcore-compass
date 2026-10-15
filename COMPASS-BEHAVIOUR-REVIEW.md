# Compass behaviour review — custom items and lodestone policy

Reviewed against the current `main` source, including `CompassManager`, `CompassInteractionListener`, and `PlayerDeathListener`.

## Current implementation

The plugin already uses a `NamespacedKey` and `PersistentDataContainer` BYTE marker to identify plugin compasses:

```java
key = new NamespacedKey(plugin, "hardcore_compass");
meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
```

`CompassManager.isTagged(ItemStack)` currently accepts only a `Material.COMPASS` with that marker. This correctly prevents ordinary compasses from being treated as Hardcore Compasses.

However, the same key is used conceptually for both item types, and `createLodestone()` tags a lodestone item with the same key. The interaction listener currently binds a tagged compass when right-clicking **any lodestone block**, including a normal vanilla lodestone:

```java
if (event.getClickedBlock().getType() != Material.LODESTONE) return;
...
compasses.bind(held, player, block.getLocation());
```

That means a vanilla lodestone can currently become the target of a Hardcore Compass. If the intended design is that only crafted or admin-given Hardcore Lodestones may be used, this needs a block-level ownership/type check.

## Required behaviour

1. Hardcore Compasses and Hardcore Lodestones must be clearly distinguishable from vanilla items.
2. Both should have configurable crafting recipes, names, and descriptions.
3. The crafting-table result preview should show the custom name and lore.
4. Only Hardcore Lodestone blocks should be valid binding targets, if that is the intended server rule.
5. Multiple Hardcore Compasses should be able to bind to the same Hardcore Lodestone.
6. One player binding a compass must not overwrite or interfere with another player’s compass.
7. Breaking a Hardcore Lodestone should untrack every compass bound to that exact block.
8. Crafting a new Hardcore Compass must not reset a player’s death count.
9. First-death compass preservation should remain a server-wide admin setting.

## Recommended data model

Use two separate item-type markers rather than one shared BYTE value:

```java
private final NamespacedKey compassKey;
private final NamespacedKey lodestoneKey;

compassKey = new NamespacedKey(plugin, "hardcore_compass");
lodestoneKey = new NamespacedKey(plugin, "hardcore_lodestone");
```

Item creation:

```java
meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
```

```java
meta.getPersistentDataContainer().set(lodestoneKey, PersistentDataType.BYTE, (byte) 1);
```

Then add separate checks:

```java
public boolean isHardcoreCompass(ItemStack item) {
    return item != null
            && item.getType() == Material.COMPASS
            && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer()
                .has(compassKey, PersistentDataType.BYTE);
}

public boolean isHardcoreLodestone(ItemStack item) {
    return item != null
            && item.getType() == Material.LODESTONE
            && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer()
                .has(lodestoneKey, PersistentDataType.BYTE);
}
```

Replace every current call to `isTagged(item)` with the appropriate type-specific check.

## Marking placed lodestones

An ItemStack’s PersistentDataContainer is lost when the item is placed as a block. To preserve Hardcore Lodestone identity after placement, store a marker on the block’s `TileState`:

```java
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
public void onPlace(BlockPlaceEvent event) {
    ItemStack item = event.getItemInHand();
    if (!compasses.isHardcoreLodestone(item)) return;

    Block block = event.getBlockPlaced();
    if (!(block.getState() instanceof TileState state)) return;

    PersistentDataContainer pdc = state.getPersistentDataContainer();
    pdc.set(compasses.lodestoneBlockKey(), PersistentDataType.BYTE, (byte) 1);
    state.update();
}
```

Expose the block marker key from `CompassManager`:

```java
public NamespacedKey lodestoneBlockKey() {
    return lodestoneBlockKey;
}
```

Then change the interaction listener to reject ordinary lodestone blocks:

```java
if (!(block.getState() instanceof TileState state)) return;

boolean hardcore = state.getPersistentDataContainer()
        .has(compasses.lodestoneBlockKey(), PersistentDataType.BYTE);

if (!hardcore) return;
```

This ensures that only a placed Hardcore Lodestone can be bound to a Hardcore Compass.

## Crafting recipes

The current `registerRecipe()` only registers a Hardcore Compass recipe. Add a second recipe for the Hardcore Lodestone.

Recommended configuration:

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

Use separate recipe keys:

```java
private final NamespacedKey compassRecipeKey;
private final NamespacedKey lodestoneRecipeKey;

compassRecipeKey = new NamespacedKey(plugin, "hardcore_compass_recipe");
lodestoneRecipeKey = new NamespacedKey(plugin, "hardcore_lodestone_recipe");
```

Register the compass recipe with `createCompass(null, null)` and the lodestone recipe with `createLodestone(null)`.

In `CompassCraftListener`, identify which recipe produced the preview result and replace it with the corresponding tagged item:

```java
if (compasses.isCompassRecipe(event.getRecipe())) {
    event.getInventory().setResult(compasses.createCompass(null, player));
} else if (compasses.isLodestoneRecipe(event.getRecipe())) {
    event.getInventory().setResult(compasses.createLodestone(player));
}
```

Because the crafting preview has a player context through `PrepareItemCraftEvent`, the custom name and lore should be visible before the player takes the item. If Paper does not reliably provide the crafting player in every event path, fall back to the server default language rather than showing vanilla item text.

## Shared lodestones

Do **not** store a single owner or target on the lodestone itself. The compass already stores its own target in `CompassMeta`, so multiple compasses can point to the same lodestone without conflicting.

The current `markTargetLost(World, Location)` approach is correct in principle: it scans compasses and untracks every one whose stored world and block coordinates match the broken lodestone.

Keep this model:

```text
Hardcore Lodestone block
        ↑
        ├── Player A compass
        ├── Player A second compass
        └── Player B compass
```

Breaking the lodestone should untrack all three. It should not delete the compasses or change their ownership.

## Lodestone break protection

Add a configurable policy:

```yaml
lodestone:
  break-protection: none # none | owner-only | admin-only
  untrack-compasses-on-break: true
```

Implementation:

- `none`: anyone may break the lodestone; all bound compasses become untracked.
- `owner-only`: store the placer UUID in the lodestone block’s `TileState` PDC and cancel `BlockBreakEvent` for other non-admin players.
- `admin-only`: cancel the break event unless the player has `hardcorecompass.admin`.

For the intended hardcore experience, `none` is the recommended default. A player’s base lodestone remains vulnerable, which preserves risk and makes compass recovery meaningful.

## Offline compass handling

The current `markTargetLost()` only scans online players’ inventories and ender chests. That means an offline player’s compass will remain visually tracked until they next join.

Two acceptable approaches:

1. **Simple and recommended initially:** leave offline compasses untouched, but refresh their tracked state and lore on join by comparing their stored lodestone target with a registry of broken/missing lodestone locations.
2. **More robust:** maintain a persistent registry of lodestone locations and broken state, then resolve compass state on join, death, or compass use.

At minimum, document that offline compasses may briefly continue displaying as tracked until the player rejoins.

## Death count and preservation

Crafting or obtaining a new Hardcore Compass must **not** reset the player’s death count.

The current death logic is broadly correct:

```java
int deathCount = lives.deaths(player.getUniqueId()) + 1;
boolean preserve = deathCount == 1
        && preservedCompass != null
        && lives.markCompassPreserved(player.getUniqueId());
```

Keep death count attached to the player UUID, not to an individual compass. A newly crafted compass is only a replacement navigation item; it must not restore first-death preservation.

Make the policy explicit and admin-controlled:

```yaml
compass:
  preserve-on-first-death: true
  reset-death-count-on-new-compass: false
```

Commands:

```text
/hcompass setpreserve <true|false>
/hcompass setresetoncraft <true|false>
```

Both should require `hardcorecompass.admin`, save to `config.yml`, and apply immediately after a config reload or direct `ConfigManager` update.

## Testing checklist

- Craft a Hardcore Compass and confirm its custom name and lore appear in the crafting result preview.
- Craft a Hardcore Lodestone and confirm its custom name and lore appear in the crafting result preview.
- Confirm a vanilla compass cannot bind to a Hardcore Lodestone.
- Confirm a Hardcore Compass cannot bind to a vanilla lodestone.
- Bind two players’ compasses to the same Hardcore Lodestone and confirm both remain independently tracked.
- Break the shared lodestone and confirm both compasses become untracked.
- Confirm crafting a new compass does not reset death count or restore first-death preservation.
- Test `break-protection: none`, `owner-only`, and `admin-only`.
- Test offline players with compasses bound to a broken lodestone, then verify their compass state after reconnecting.
- Test `/hcompass setpreserve false`, then confirm no compass is preserved on first death.
