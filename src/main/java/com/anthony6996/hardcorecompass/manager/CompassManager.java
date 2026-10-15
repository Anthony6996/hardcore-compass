package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class CompassManager {
    private final NamespacedKey key; private final ConfigManager config;
    public CompassManager(HardcoreCompassPlugin plugin,ConfigManager config){key=new NamespacedKey(plugin,"hardcore_compass");this.config=config;}
    public ItemStack createLodestone(){ItemStack item=new ItemStack(Material.LODESTONE);ItemMeta m=item.getItemMeta();m.setDisplayName("§6Hardcore Lodestone");m.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)1);item.setItemMeta(m);return item;}
    public ItemStack createCompass(Block lodestone){ItemStack item=new ItemStack(Material.COMPASS);CompassMeta meta=(CompassMeta)item.getItemMeta();if(lodestone!=null){meta.setLodestone(lodestone.getLocation());meta.setLodestoneTracked(true);}meta.setDisplayName("§6Hardcore Compass");meta.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);return item;}
    public boolean isTagged(ItemStack item){return item!=null&&item.getType()==Material.COMPASS&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(key,PersistentDataType.BYTE);}
    public boolean isBoundTaggedCompass(ItemStack item){return isTagged(item)&&item.getItemMeta() instanceof CompassMeta m&&m.hasLodestone();}
    public void giveStarter(org.bukkit.entity.Player p){if(!config.starterEnabled())return;if(config.starterLodestone())p.getInventory().addItem(createLodestone());if(config.starterCompass())p.getInventory().addItem(createCompass(null));}
    public ItemStack findBoundCompass(org.bukkit.entity.Player p){for(ItemStack item:p.getInventory().getContents())if(isBoundTaggedCompass(item))return item.clone();return null;}
}
