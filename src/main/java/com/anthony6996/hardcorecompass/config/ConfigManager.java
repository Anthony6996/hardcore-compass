package com.anthony6996.hardcorecompass.config;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Locale;

public final class ConfigManager {
    private final HardcoreCompassPlugin plugin;
    private FileConfiguration values;
    public ConfigManager(HardcoreCompassPlugin plugin) { this.plugin=plugin; reload(); }
    public void reload() { plugin.reloadConfig(); values=plugin.getConfig(); validate(); }
    private void validate() {
        if (values.getInt("teleport.min-radius",10000)<0) values.set("teleport.min-radius",0);
        if (values.getInt("teleport.max-radius",30000)<values.getInt("teleport.min-radius",10000)) values.set("teleport.max-radius",values.getInt("teleport.min-radius",10000));
        String language=values.getString("messages.language","en"); if(language==null||(!language.equalsIgnoreCase("en")&&!language.equalsIgnoreCase("es"))) values.set("messages.language","en");
    }
    public boolean setLanguage(String language) { if(language==null||(!language.equalsIgnoreCase("en")&&!language.equalsIgnoreCase("es")))return false;values.set("messages.language",language.toLowerCase(Locale.ROOT));plugin.saveConfig();return true; }
    private FileConfiguration c() { return values; }
    public int startingLives(){return Math.max(1,c().getInt("lives.starting",2));}
    public boolean debugInfo(){return c().getBoolean("lives.debug-info",true);}
    public String worldName(){return c().getString("world","world");}
    public int minRadius(){return Math.max(0,c().getInt("teleport.min-radius",10000));}
    public int maxRadius(){return Math.max(minRadius(),c().getInt("teleport.max-radius",30000));}
    public int attempts(){return Math.max(1,c().getInt("teleport.attempts",80));}
    public int searchRadius(){return Math.max(1,c().getInt("teleport.search-radius",8));}
    public int borderMargin(){return Math.max(0,c().getInt("teleport.border-margin",16));}
    public boolean avoidWater(){return c().getBoolean("teleport.avoid-water",true);}
    public boolean avoidLava(){return c().getBoolean("teleport.avoid-lava",true);}
    public boolean avoidVoid(){return c().getBoolean("teleport.avoid-void",true);}
    public boolean clearEnderChest(){return c().getBoolean("penalties.clear-ender-chest",true);}
    public boolean clearXp(){return c().getBoolean("penalties.clear-xp",true);}
    public boolean clearBedSpawn(){return c().getBoolean("penalties.clear-bed-spawn",true);}
    public boolean wipeInventory(){return c().getBoolean("penalties.wipe-inventory",true);}
    public boolean enableRtp(){return c().getBoolean("penalties.enable-rtp",true);}
    public boolean starterEnabled(){return c().getBoolean("starter.enabled",true);}
    public boolean starterLodestone(){return c().getBoolean("starter.lodestone",true);}
    public boolean starterCompass(){return c().getBoolean("starter.compass",true);}
    public String defaultLanguage(){return validLanguage(c().getString("messages.language","en"));}
    public String language(Player player){if(player==null)return defaultLanguage();String lang=plugin.getPlayerData().getLanguage(player.getUniqueId());return validLanguage(lang==null?defaultLanguage():lang);}
    private String validLanguage(String language){return language==null||(!language.equalsIgnoreCase("en")&&!language.equalsIgnoreCase("es"))?"en":language.toLowerCase(Locale.ROOT);}
    public String getMessage(Player player,String key){return localized(language(player),key);}
    public String message(String key){return localized(defaultLanguage(),key);}
    public String localized(String lang,String key){String text=c().getString("messages."+lang+"."+key);if(text==null)text=c().getString("messages.en."+key);if(text==null)text=c().getString("messages."+key,"");return ChatColor.translateAlternateColorCodes('&',text);}
    public List<String> recipeShape(){return c().getStringList("recipe.shape");}
    public boolean recipeEnabled(){return c().getBoolean("recipe.enabled",true);}
    public List<String> recipeSymbols(){return c().getConfigurationSection("recipe.ingredients")==null?List.of():List.copyOf(c().getConfigurationSection("recipe.ingredients").getKeys(false));}
    public String recipeIngredient(String symbol){return c().getString("recipe.ingredients."+symbol);}
}
