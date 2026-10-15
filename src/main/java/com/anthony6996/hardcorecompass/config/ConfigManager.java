package com.anthony6996.hardcorecompass.config;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.ArrayList;
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
        if (!values.contains("recipe.enabled")) values.set("recipe.enabled",true);
        if (!values.isList("recipe.shape") || values.getStringList("recipe.shape").isEmpty()) values.set("recipe.shape",List.of(" R ","RCR"," R "));
        if (!values.isConfigurationSection("recipe.ingredients")) values.createSection("recipe.ingredients");
        if (!values.contains("recipe.ingredients.R")) values.set("recipe.ingredients.R","REDSTONE"); if (!values.contains("recipe.ingredients.C")) values.set("recipe.ingredients.C","COMPASS");
        List<String> shape=values.getStringList("recipe.shape"); java.util.Set<Character> used=new java.util.HashSet<>(); shape.forEach(row->row.chars().filter(ch->ch!=' ').forEach(ch->used.add((char)ch)));
        for(char symbol:used){String name=values.getString("recipe.ingredients."+symbol);if(name==null||Material.matchMaterial(name)==null){plugin.getLogger().warning("Invalid/missing recipe material for symbol "+symbol+"; using safe defaults.");values.set("recipe.ingredients."+symbol,symbol=='C'?"COMPASS":"REDSTONE");}}
    }
    public boolean setLanguage(String language) { if(language==null||(!language.equalsIgnoreCase("en")&&!language.equalsIgnoreCase("es")))return false;values.set("messages.language",language.toLowerCase(Locale.ROOT));plugin.saveConfig();return true; }
    public void setRadius(int min,int max){values.set("teleport.min-radius",min);values.set("teleport.max-radius",max);plugin.saveConfig();}
    private FileConfiguration c() { return values; }
    public int startingLives(){return Math.max(1,c().getInt("lives.starting",2));} public boolean debugInfo(){return c().getBoolean("lives.debug-info",true);} public String worldName(){return c().getString("world","world");}
    public int minRadius(){return Math.max(0,c().getInt("teleport.min-radius",10000));} public int maxRadius(){return Math.max(minRadius(),c().getInt("teleport.max-radius",30000));}
    public int attempts(){return Math.max(1,c().getInt("teleport.attempts",80));} public int searchRadius(){return Math.max(1,c().getInt("teleport.search-radius",8));} public int borderMargin(){return Math.max(0,c().getInt("teleport.border-margin",16));}
    public boolean avoidWater(){return c().getBoolean("teleport.avoid-water",true);} public boolean avoidLava(){return c().getBoolean("teleport.avoid-lava",true);} public boolean avoidVoid(){return c().getBoolean("teleport.avoid-void",true);}
    public boolean clearEnderChest(){return c().getBoolean("penalties.clear-ender-chest",true);} public boolean clearXp(){return c().getBoolean("penalties.clear-xp",true);} public boolean clearBedSpawn(){return c().getBoolean("penalties.clear-bed-spawn",true);} public boolean wipeInventory(){return c().getBoolean("penalties.wipe-inventory",true);} public boolean enableRtp(){return c().getBoolean("penalties.enable-rtp",true);}
    public boolean starterEnabled(){return c().getBoolean("starter.enabled",true);} public boolean starterLodestone(){return c().getBoolean("starter.lodestone",true);} public boolean starterCompass(){return c().getBoolean("starter.compass",true);}
    public boolean recipeEnabled(){return c().getBoolean("recipe.enabled",true);} public List<String> recipeShape(){return new ArrayList<>(c().getStringList("recipe.shape"));} public List<String> recipeSymbols(){return c().getConfigurationSection("recipe.ingredients")==null?List.of():List.copyOf(c().getConfigurationSection("recipe.ingredients").getKeys(false));} public Material recipeIngredient(char ch){String n=c().getString("recipe.ingredients."+ch);return n==null?null:Material.matchMaterial(n);}
    public String defaultLanguage(){return validLanguage(c().getString("messages.language","en"));} public String language(Player p){if(p==null)return defaultLanguage();String lang=plugin.getPlayerData().getLanguage(p.getUniqueId());return validLanguage(lang==null?defaultLanguage():lang);} private String validLanguage(String l){return l==null||(!l.equalsIgnoreCase("en")&&!l.equalsIgnoreCase("es"))?"en":l.toLowerCase(Locale.ROOT);}
    public String getMessage(Player p,String k){return localized(language(p),k);} public String message(String k){return localized(defaultLanguage(),k);} public String localized(String l,String k){String s=c().getString("messages."+l+"."+k);if(s==null)s=c().getString("messages.en."+k);if(s==null)s=c().getString("messages."+k,"");return ChatColor.translateAlternateColorCodes('&',s);}
}
