package com.anthony6996.hardcorecompass.config;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.*;

public final class ConfigManager {
    private final HardcoreCompassPlugin plugin;
    private FileConfiguration values;
    public ConfigManager(HardcoreCompassPlugin p) { plugin=p; reload(); }
    public void reload() { plugin.reloadConfig(); values=plugin.getConfig(); migrate(); validate(); }
    private void migrate() {
        int v=values.getInt("config-version",1);
        if(v<5) {
            if(values.contains("recipe.enabled")&&!values.contains("recipe.compass.enabled")) {
                values.set("recipe.compass.enabled",values.getBoolean("recipe.enabled",true));
                values.set("recipe.compass.shape",values.get("recipe.shape"));
                values.set("recipe.compass.ingredients",values.get("recipe.ingredients"));
            }
            values.set("recipe.lodestone.enabled",values.getBoolean("recipe.lodestone.enabled",true));
            values.set("recipe.lodestone.shape",values.get("recipe.lodestone.shape",List.of("III","ILI","III")));
            values.set("recipe.lodestone.ingredients",values.get("recipe.lodestone.ingredients",Map.of("I","IRON_INGOT","L","LODESTONE")));
        }
        values.set("teleport.spawn-min-distance",values.getInt("teleport.spawn-min-distance",750));
        values.set("teleport.spawn-history-limit",values.getInt("teleport.spawn-history-limit",10));
        values.set("compass.preserve-on-first-death",values.getBoolean("compass.preserve-on-first-death",true));
        values.set("lodestone.break-protection",values.getString("lodestone.break-protection","none"));
        values.set("lodestone.untrack-compasses-on-break",values.getBoolean("lodestone.untrack-compasses-on-break",true));
        values.set("lodestone.require-custom-block",values.getBoolean("lodestone.require-custom-block",false));
        values.set("config-version",5); plugin.saveConfig();
    }
    private void validate() {
        if(values.getInt("teleport.min-radius",10000)<0) values.set("teleport.min-radius",0);
        if(values.getInt("teleport.max-radius",30000)<values.getInt("teleport.min-radius",10000)) values.set("teleport.max-radius",values.getInt("teleport.min-radius",10000));
        if(values.getInt("teleport.spawn-history-limit",10)<1) values.set("teleport.spawn-history-limit",10);
        String protection=values.getString("lodestone.break-protection","none");
        if(protection==null||!Set.of("none","owner-only","admin-only").contains(protection.toLowerCase(Locale.ROOT))) values.set("lodestone.break-protection","none");
        String lang=values.getString("messages.language","en");
        if(lang==null||(!lang.equalsIgnoreCase("en")&&!lang.equalsIgnoreCase("es"))) values.set("messages.language","en");
    }
    private FileConfiguration c(){return values;}
    public boolean recipeEnabled(){return c().getBoolean("recipe.compass.enabled",c().getBoolean("recipe.enabled",true));}
    public boolean lodestoneRecipeEnabled(){return c().getBoolean("recipe.lodestone.enabled",true);}
    public List<String> recipeShape(String type){return new ArrayList<>(c().getStringList("recipe."+type+".shape"));}
    public Material recipeIngredient(String type,char symbol){String m=c().getString("recipe."+type+".ingredients."+symbol);return m==null?null:Material.matchMaterial(m);}
    public boolean preserveOnFirstDeath(){return c().getBoolean("compass.preserve-on-first-death",true);}
    public String lodestoneBreakProtection(){return c().getString("lodestone.break-protection","none").toLowerCase(Locale.ROOT);}
    public boolean untrackOnLodestoneBreak(){return c().getBoolean("lodestone.untrack-compasses-on-break",true);}
    public boolean requireCustomLodestoneBlock(){return c().getBoolean("lodestone.require-custom-block",false);}
    public void setPreserveOnFirstDeath(boolean b){c().set("compass.preserve-on-first-death",b);plugin.saveConfig();}
    public void setRadius(int min,int max){c().set("teleport.min-radius",min);c().set("teleport.max-radius",max);plugin.saveConfig();}
    public int spawnHistoryLimit(){return Math.max(1,c().getInt("teleport.spawn-history-limit",10));}
    public int startingLives(){return Math.max(1,c().getInt("lives.starting",2));}
    public int minRadius(){return Math.max(0,c().getInt("teleport.min-radius",10000));}
    public int maxRadius(){return Math.max(minRadius(),c().getInt("teleport.max-radius",30000));}
    public int attempts(){return Math.max(1,c().getInt("teleport.attempts",80));}
    public int searchRadius(){return Math.max(1,c().getInt("teleport.search-radius",8));}
    public int borderMargin(){return Math.max(0,c().getInt("teleport.border-margin",16));}
    public int spawnMinDistance(){return Math.max(0,c().getInt("teleport.spawn-min-distance",750));}
    public boolean avoidWater(){return c().getBoolean("teleport.avoid-water",true);}
    public boolean avoidLava(){return c().getBoolean("teleport.avoid-lava",true);}
    public boolean avoidVoid(){return c().getBoolean("teleport.avoid-void",true);}
    public boolean starterEnabled(){return c().getBoolean("starter.enabled",true);}
    public boolean starterLodestone(){return c().getBoolean("starter.lodestone",true);}
    public boolean starterCompass(){return c().getBoolean("starter.compass",true);}
    public boolean clearEnderChest(){return c().getBoolean("penalties.clear-ender-chest",true);}
    public boolean clearXp(){return c().getBoolean("penalties.clear-xp",true);}
    public boolean clearBedSpawn(){return c().getBoolean("penalties.clear-bed-spawn",true);}
    public boolean wipeInventory(){return c().getBoolean("penalties.wipe-inventory",true);}
    public boolean enableRtp(){return c().getBoolean("penalties.enable-rtp",true);}
    public String language(Player p){if(p==null)return defaultLanguage();String l=plugin.getPlayerData().getLanguage(p.getUniqueId());return validLanguage(l==null?defaultLanguage():l);}
    public String defaultLanguage(){return validLanguage(c().getString("messages.language","en"));}
    private String validLanguage(String l){return l==null||(!l.equalsIgnoreCase("en")&&!l.equalsIgnoreCase("es"))?"en":l.toLowerCase(Locale.ROOT);}
    public String getMessage(Player p,String k){return localized(language(p),k);}
    public String message(String k){return localized(defaultLanguage(),k);}
    public String localized(String lang,String key){
        String normalized=validLanguage(lang);
        String text=c().getString("messages."+normalized+"."+key);
        if(text==null||text.isBlank()) text=c().getString("messages.en."+key);
        if(text==null||text.isBlank()) text=c().getString("messages."+key);
        if(text==null||text.isBlank()) text=builtinMessage(normalized,key);
        if(text==null||text.isBlank()) { plugin.getLogger().warning("Missing or blank message key: messages."+normalized+"."+key); text="&cMessage unavailable: "+key; }
        return ChatColor.translateAlternateColorCodes('&',text);
    }
    private String builtinMessage(String lang,String key){
        boolean es="es".equalsIgnoreCase(lang);
        return switch(key){
            case "help-player" -> es ? "&6HardcoreCompass &7- Comandos disponibles:\n&e/hcompass help &7- muestra esta ayuda\n&e/hcompass info &7- consulta tus vidas\n&e/hcompass lang [en|es] &7- cambia el idioma" : "&6HardcoreCompass &7- Available commands:\n&e/hcompass help &7- show this help\n&e/hcompass info &7- view your lives\n&e/hcompass lang [en|es] &7- change language";
            case "help-admin" -> es ? "&6HardcoreCompass &7- Comandos disponibles:\n&e/hcompass help &7- muestra esta ayuda\n&e/hcompass info [jugador] &7- consulta vidas\n&e/hcompass lang [en|es] &7- cambia idioma\n&e/hcompass give [compass|lodestone|all] [jugador] &7- entrega objetos\n&e/hcompass reset <jugador> <tipo>\n&e/hcompass wipelinks <jugador>\n&e/hcompass setpreserve <true|false>\n&e/hcompass radius <min> <max>\n&e/hcompass reload" : "&6HardcoreCompass &7- Available commands:\n&e/hcompass help &7- show this help\n&e/hcompass info [player] &7- view lives\n&e/hcompass lang [en|es] &7- change language\n&e/hcompass give [compass|lodestone|all] [player] &7- give items\n&e/hcompass reset <player> <type>\n&e/hcompass wipelinks <player>\n&e/hcompass setpreserve <true|false>\n&e/hcompass radius <min> <max>\n&e/hcompass reload";
            case "lang-usage" -> "&eUsage: /hcompass lang [en|es]";
            case "lang-current-default","lang-current" -> "&7Current language: {language}";
            case "lang-set" -> "&aLanguage set to {language}.";
            case "info" -> "&6{player} &7- lives: {lives}, deaths: {deaths}, preserved compass: {compass}";
            case "give-usage" -> "&eUsage: /hcompass give [compass|lodestone|all] [player]";
            case "give-success" -> "&aGave {item} to {player}.";
            case "give-success-all" -> "&aGave compass and lodestone to {player}.";
            case "reset-usage" -> "&eUsage: /hcompass reset <player> <lives|deaths|compass|rtp|all>";
            case "reset" -> "&aReset {type} for {player}.";
            case "player-not-found" -> "&cPlayer not found or not online.";
            case "wipelinks-usage" -> "&eUsage: /hcompass wipelinks <player>";
            case "wipelinks-done" -> "&aCompass links cleared for {player}.";
            case "preserve-usage" -> "&eUsage: /hcompass setpreserve <true|false>";
            case "preserve-set" -> "&aFirst-death compass preservation set to {value}.";
            case "radius-usage" -> "&eUsage: /hcompass radius <min> <max>";
            case "radius-set" -> "&aRTP radius set to {min}-{max}.";
            case "reloaded" -> "&aHardcoreCompass configuration reloaded.";
            case "no-permission" -> es ? "&cNo tienes permiso." : "&cNo permission.";
            default -> null;
        };
    }
}
