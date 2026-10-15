package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

public final class CompassManager {
    private final NamespacedKey key;
    private final ConfigManager config;
    private final NamespacedKey recipeKey;

    public CompassManager(HardcoreCompassPlugin plugin, ConfigManager config) {
        this.config = config;
        key = new NamespacedKey(plugin, "hardcore_compass");
        recipeKey = new NamespacedKey(plugin, "hardcore_compass");
    }

    public void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);
        if (!config.recipeEnabled()) return;
        List<String> shape = config.recipeShape();
        if (shape.size() < 1 || shape.size() > 3 || shape.stream().anyMatch(row -> row.isEmpty() || row.length() > 3))
            throw new IllegalArgumentException("recipe.shape must have 1-3 rows, each 1-3 characters");
        java.util.Set<Character> used = new java.util.HashSet<>();
        shape.forEach(row -> row.chars().filter(ch -> ch != ' ').forEach(ch -> used.add((char) ch)));
        if (used.isEmpty()) throw new IllegalArgumentException("recipe.shape must use at least one ingredient symbol");
        for (char symbol : used) {
            Material material = config.recipeIngredient(symbol);
            if (material == null) throw new IllegalArgumentException("No valid recipe.ingredients entry for symbol " + symbol);
        }
        for (String symbol : config.recipeSymbols()) {
            if (symbol.length() != 1 || (!used.contains(symbol.charAt(0)) && !symbol.equals(" ")))
                throw new IllegalArgumentException("Recipe ingredient key must be a used single character: " + symbol);
        }
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createCompass(null, null));
        recipe.shape(shape.toArray(String[]::new));
        for (char symbol : used) recipe.setIngredient(symbol, config.recipeIngredient(symbol));
        if (!Bukkit.addRecipe(recipe)) throw new IllegalStateException("Could not register Hardcore Compass recipe");
    }

    public boolean isRecipe(Recipe recipe) {
        return recipe instanceof ShapedRecipe shaped && recipeKey.equals(shaped.getKey());
    }

    public ItemStack createLodestone() { return createLodestone(null); }
    public ItemStack createLodestone(Player recipient) {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        String lang = config.language(recipient);
        meta.setDisplayName(config.localized(lang, "compass-name-lodestone"));
        meta.setLore(List.of(config.localized(lang, "lodestone-lore")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createCompass(Block lodestone) { return createCompass(lodestone, null); }
    public ItemStack createCompass(Block lodestone, Player recipient) {
        ItemStack item = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        Location target = lodestone == null ? null : lodestone.getLocation();
        if (target != null) { meta.setLodestone(target); meta.setLodestoneTracked(true); }
        String lang = config.language(recipient);
        meta.setDisplayName(config.localized(lang, "compass-name"));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        updateLore(meta, lang, target);
        item.setItemMeta(meta);
        return item;
    }

    public void bind(ItemStack item, Player player, Location target) {
        if (!isTagged(item) || !(item.getItemMeta() instanceof CompassMeta meta)) return;
        String lang = config.language(player);
        meta.setLodestone(target);
        meta.setLodestoneTracked(true);
        updateLore(meta, lang, target);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public void markTargetLost(World world, Location target) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            markContents(player.getInventory().getContents(), world, target);
            markContents(player.getEnderChest().getContents(), world, target);
        }
    }
    private void markContents(ItemStack[] items, World world, Location target) {
        for (ItemStack item : items) {
            if (!isTagged(item) || !(item.getItemMeta() instanceof CompassMeta meta) || !meta.hasLodestone()) continue;
            Location bound = meta.getLodestone();
            if (bound == null || bound.getWorld() == null || !bound.getWorld().equals(world)
                    || bound.getBlockX() != target.getBlockX() || bound.getBlockY() != target.getBlockY()
                    || bound.getBlockZ() != target.getBlockZ()) continue;
            meta.setLodestoneTracked(false);
            updateLore(meta, config.language(playerForItem(items)), null);
            item.setItemMeta(meta);
        }
    }
    // Language lookup for loss notices is not tied to an inventory array; lore is refreshed on the next bind/use.
    private Player playerForItem(ItemStack[] ignored) { return null; }

    public boolean isTagged(ItemStack item) {
        return item != null && item.getType() == Material.COMPASS && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
    public void giveStarter(Player player) {
        if (!config.starterEnabled()) return;
        if (config.starterLodestone()) player.getInventory().addItem(createLodestone(player));
        if (config.starterCompass()) player.getInventory().addItem(createCompass(null, player));
    }
    public ItemStack findTaggedCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) if (isTagged(item)) return item.clone();
        return null;
    }
    private void updateLore(CompassMeta meta, String lang, Location target) {
        String text = config.localized(lang, target == null ? "compass-unbound" : "compass-bound-lore");
        if (target != null) text = text.replace("{x}", String.valueOf(target.getBlockX())).replace("{y}", String.valueOf(target.getBlockY())).replace("{z}", String.valueOf(target.getBlockZ()));
        else text = text.replace("{lodestone_name}", config.localized(lang, "lodestone-name"));
        meta.setLore(List.of(text));
    }
}
