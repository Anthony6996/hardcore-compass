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
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class CompassManager {
    private final NamespacedKey key;
    private final ConfigManager config;
    private final HardcoreCompassPlugin plugin;
    private final NamespacedKey recipeKey;

    public CompassManager(HardcoreCompassPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        key = new NamespacedKey(plugin, "hardcore_compass");
        recipeKey = new NamespacedKey(plugin, "hardcore_compass");
    }

    public void registerRecipe() {
        if (!config.recipeEnabled()) return;
        Bukkit.removeRecipe(recipeKey);
        ItemStack result = createCompass(null, null);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        List<String> shape = config.recipeShape();
        recipe.shape(shape.toArray(new String[0]));
        for (String symbol : config.recipeSymbols()) {
            String materialName = config.recipeIngredient(symbol);
            if (materialName == null || materialName.isBlank()) continue;
            Material material = Material.matchMaterial(materialName);
            if (material == null) throw new IllegalArgumentException("Invalid recipe material: " + materialName);
            recipe.setIngredient(symbol.charAt(0), material);
        }
        Bukkit.addRecipe(recipe);
    }

    public boolean isRecipe(org.bukkit.inventory.Recipe recipe) {
        return recipe instanceof ShapedRecipe shaped && shaped.getKey().equals(recipeKey);
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
        if (lodestone != null) {
            meta.setLodestone(lodestone.getLocation());
            meta.setLodestoneTracked(true);
        }
        String lang = config.language(recipient);
        meta.setDisplayName(config.localized(lang, "compass-name"));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        updateLore(meta, lang, lodestone == null ? null : lodestone.getLocation());
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
            for (ItemStack item : player.getInventory().getContents()) markIfMatches(item, world, target);
            for (ItemStack item : player.getEnderChest().getContents()) markIfMatches(item, world, target);
        }
    }

    private void markIfMatches(ItemStack item, World world, Location target) {
        if (!isTagged(item) || !(item.getItemMeta() instanceof CompassMeta meta) || !meta.hasLodestone()) return;
        Location bound = meta.getLodestone();
        if (bound == null || bound.getWorld() == null || !bound.getWorld().equals(world)
                || bound.getBlockX() != target.getBlockX() || bound.getBlockY() != target.getBlockY()
                || bound.getBlockZ() != target.getBlockZ()) return;
        String lang = config.defaultLanguage();
        meta.setLodestoneTracked(false);
        meta.setLore(List.of(config.localized(lang, "compass-unbound")));
        item.setItemMeta(meta);
    }

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
        if (target != null) text = text.replace("{x}", String.valueOf(target.getBlockX()))
                .replace("{y}", String.valueOf(target.getBlockY())).replace("{z}", String.valueOf(target.getBlockZ()));
        else text = text.replace("{lodestone_name}", config.localized(lang, "lodestone-name"));
        meta.setLore(List.of(text));
    }
}
