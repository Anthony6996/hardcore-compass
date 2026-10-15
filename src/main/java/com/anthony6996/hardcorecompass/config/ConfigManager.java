package com.anthony6996.hardcorecompass.config;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Locale;

public final class ConfigManager {
    private final HardcoreCompassPlugin plugin;
    public ConfigManager(HardcoreCompassPlugin plugin) { this.plugin = plugin; }
    public void reload() { }
    private FileConfiguration c() { return plugin.getConfig(); }
    public int startingLives() { return Math.max(1, c().getInt("lives.starting", 2)); }
    public boolean debugInfo() { return c().getBoolean("lives.debug-info", true); }
    public String worldName() { return c().getString("world", "world"); }
    public int minRadius() { return Math.max(0, c().getInt("teleport.min-radius", 10000)); }
    public int maxRadius() { return Math.max(minRadius(), c().getInt("teleport.max-radius", 30000)); }
    public int attempts() { return Math.max(1, c().getInt("teleport.attempts", 80)); }
    public int searchRadius() { return Math.max(1, c().getInt("teleport.search-radius", 8)); }
    public int borderMargin() { return Math.max(0, c().getInt("teleport.border-margin", 16)); }
    public boolean avoidWater() { return c().getBoolean("teleport.avoid-water", true); }
    public boolean avoidLava() { return c().getBoolean("teleport.avoid-lava", true); }
    public boolean avoidVoid() { return c().getBoolean("teleport.avoid-void", true); }
    public boolean starterEnabled() { return c().getBoolean("starter.enabled", true); }
    public boolean starterLodestone() { return c().getBoolean("starter.lodestone", true); }
    public boolean starterCompass() { return c().getBoolean("starter.compass", true); }
    public String message(String key) {
        String language = c().getString("messages.language", "en").toLowerCase(Locale.ROOT);
        String localized = c().getString("messages." + language + "." + key);
        return ChatColor.translateAlternateColorCodes('&', localized == null ? c().getString("messages." + key, "") : localized);
    }
}
