package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class CompassManager {
    private final NamespacedKey key;
    private final ConfigManager config;

    public CompassManager(HardcoreCompassPlugin plugin, ConfigManager config) {
        key = new NamespacedKey(plugin, "hardcore_compass");
        this.config = config;
    }

    public ItemStack createLodestone() {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Hardcore Lodestone");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createCompass(Block lodestone) {
        return createCompass(lodestone, null);
    }

    public ItemStack createCompass(Block lodestone, Player recipient) {
        ItemStack item = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (lodestone != null) {
            meta.setLodestone(lodestone.getLocation());
            meta.setLodestoneTracked(true);
        }
        meta.setDisplayName("§6Hardcore Compass");
        meta.setLore(List.of(config.getMessage(recipient, "compass-lore")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTagged(ItemStack item) {
        return item != null && item.getType() == Material.COMPASS && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public boolean isBoundTaggedCompass(ItemStack item) {
        return isTagged(item) && item.getItemMeta() instanceof CompassMeta meta && meta.hasLodestone();
    }

    public void giveStarter(Player player) {
        if (!config.starterEnabled()) return;
        if (config.starterLodestone()) player.getInventory().addItem(createLodestone());
        if (config.starterCompass()) player.getInventory().addItem(createCompass(null, player));
    }

    public ItemStack findTaggedCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTagged(item)) return item.clone();
        }
        return null;
    }

    public ItemStack findBoundCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isBoundTaggedCompass(item)) return item.clone();
        }
        return null;
    }
}
