package com.anthony6996.hardcorecompass.config;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Locale;

public final class ConfigManager {
    private final HardcoreCompassPlugin plugin;
    private FileConfiguration values;
    public ConfigManager(HardcoreCompassPlugin plugin) { this.plugin=plugin; reload(); }
    public void reload() { plugin.reloadConfig(); values=plugin.getConfig(); validate(); }
    private void validate() {
        if (values.getInt("teleport.min-radius",10000)<0) { plugin.getLogger().warning("teleport.min-radius below zero; using 0."); values.set("teleport.min-radius",0); }
        if (values.getInt("teleport.max-radius",30000)<values.getInt("teleport.min-radius",10000)) { plugin.getLogger().warning("teleport.max-radius is less than min-radius; correcting it."); values.set("teleport.max-radius",values.getInt("teleport.min-radius",10000)); }
        String language=values.getString("messages.language","en"); if(language==null||(!language.equalsIgnoreCase("en")&&!language.equalsIgnoreCase("es"))) { plugin.getLogger().warning("Unknown message language; using en."); values.set("messages.language","en"); }
    }
    private FileConfiguration c() { return values; }
    public int startingLives() { return Math.max(1,c().getInt("lives.starting",2)); }
    public boolean debugInfo() { return c().getBoolean("lives.debug-info",true); }
    public String worldName() { return c().getString("world","world"); }
    public int minRadius() { return Math.max(0,c().getInt("teleport.min-radius",10000)); }
    public int maxRadius() { return Math.max(minRadius(),c().getInt("teleport.max-radius",30000)); }
    public int attempts() { return Math.max(1,c().getInt("teleport.attempts",80)); }
    public int searchRadius() { return Math.max(1,c().getInt("teleport.search-radius",8)); }
    public int borderMargin() { return Math.max(0,c().getInt("teleport.border-margin",16)); }
    public boolean avoidWater() { return c().getBoolean("teleport.avoid-water",true); }
    public boolean avoidLava() { return c().getBoolean("teleport.avoid-lava",true); }
    public boolean avoidVoid() { return c().getBoolean("teleport.avoid-void",true); }
    public boolean clearEnderChest() { return c().getBoolean("penalties.clear-ender-chest",true); }
    public boolean clearXp() { return c().getBoolean("penalties.clear-xp",true); }
    public boolean clearBedSpawn() { return c().getBoolean("penalties.clear-bed-spawn",true); }
    public boolean wipeInventory() { return c().getBoolean("penalties.wipe-inventory",true); }
    public boolean enableRtp() { return c().getBoolean("penalties.enable-rtp",true); }
    public boolean starterEnabled() { return c().getBoolean("starter.enabled",true); }
    public boolean starterLodestone() { return c().getBoolean("starter.lodestone",true); }
    public boolean starterCompass() { return c().getBoolean("starter.compass",true); }
    public String message(String key) {
        String language=c().getString("messages.language","en").toLowerCase(Locale.ROOT);
        String localized=c().getString("messages."+language+"."+key);
        return ChatColor.translateAlternateColorCodes('&',localized==null?c().getString("messages."+key,""):localized);
    }
}
