package com.anthony6996.hardcorecompass.command;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import com.anthony6996.hardcorecompass.manager.LivesManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public final class CompassCommand implements CommandExecutor, TabCompleter {
    private final HardcoreCompassPlugin plugin;
    private final LivesManager lives;
    private final ConfigManager config;
    private final CompassManager compasses;
    public CompassCommand(HardcoreCompassPlugin p, LivesManager l, ConfigManager c, CompassManager m) { plugin=p; lives=l; config=c; compasses=m; }

    private String message(CommandSender sender, String key) {
        return config.localized(sender instanceof Player p ? config.language(p) : config.defaultLanguage(), key);
    }
    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0 || a[0].equalsIgnoreCase("help")) { s.sendMessage(message(s,"help")); return true; }
        String sub=a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload") && s.hasPermission("hardcorecompass.admin")) { plugin.reloadPluginConfig(); s.sendMessage(message(s,"reloaded")); return true; }
        if (sub.equals("lang")) {
            if (a.length==1) { if (!(s instanceof Player p)) { s.sendMessage(message(s,"lang-usage")); return true; } String lang=plugin.getPlayerData().getLanguage(p.getUniqueId()); s.sendMessage(lang==null?message(s,"lang-current-default"):config.getLocalizedMessage(lang,"lang-current")); return true; }
            if (a.length==2 && s instanceof Player p) { String lang=a[1].toLowerCase(Locale.ROOT); if(!lang.equals("es")&&!lang.equals("en")){s.sendMessage(message(s,"lang-usage"));return true;} plugin.getPlayerData().setLanguage(p.getUniqueId(),lang);plugin.getPlayerData().save();s.sendMessage(message(s,"lang-set"));return true; }
            if(s.hasPermission("hardcorecompass.admin")&&a.length==2&&config.setLanguage(a[1])){s.sendMessage(message(s,"lang-set"));return true;}
            s.sendMessage(message(s,"lang-usage"));return true;
        }
        if(sub.equals("give")) {
            if(!s.hasPermission("hardcorecompass.admin")){s.sendMessage(message(s,"no-permission"));return true;}
            if(a.length<2||a.length>3){s.sendMessage(message(s,"give-usage"));return true;}
            Player target=Bukkit.getPlayerExact(a[1]);if(target==null){s.sendMessage(message(s,"player-not-found"));return true;}
            String kind=a.length==3?a[2].toLowerCase(Locale.ROOT):"all";
            if(!kind.equals("compass")&&!kind.equals("lodestone")&&!kind.equals("all")){s.sendMessage(message(s,"give-usage"));return true;}
            if(kind.equals("compass")||kind.equals("all"))giveItem(target,compasses.createCompass(null,target));
            if(kind.equals("lodestone")||kind.equals("all"))giveItem(target,compasses.createLodestone(target));
            String key=kind.equals("all")?"give-success-all":"give-success";
            s.sendMessage(message(s,key).replace("{player}",target.getName()).replace("{item}",kind));return true;
        }
        if(sub.equals("info")) {
            if(!s.hasPermission("hardcorecompass.admin")&&a.length>1){s.sendMessage(message(s,"no-permission"));return true;}
            Player target=a.length>1?Bukkit.getPlayerExact(a[1]):(s instanceof Player p?p:null);if(target==null){s.sendMessage(message(s,"player-not-found"));return true;}
            s.sendMessage(message(s,"info").replace("{player}",target.getName()).replace("{lives}",String.valueOf(lives.lives(target.getUniqueId()))).replace("{deaths}",String.valueOf(lives.deaths(target.getUniqueId()))).replace("{compass}",String.valueOf(lives.hasPreservedCompass(target.getUniqueId()))));return true;
        }
        if(sub.equals("reset")&&a.length==2&&s.hasPermission("hardcorecompass.admin")){Player target=Bukkit.getPlayerExact(a[1]);if(target==null){s.sendMessage(message(s,"player-not-found"));return true;}lives.reset(target.getUniqueId());s.sendMessage(message(s,"reset").replace("{player}",target.getName()));return true;}
        s.sendMessage(message(s,"help"));return true;
    }
    private void giveItem(Player target,ItemStack item){Map<Integer,ItemStack> overflow=target.getInventory().addItem(item);overflow.values().forEach(left->target.getWorld().dropItemNaturally(target.getLocation(),left));}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){if(args.length==1)return filter(List.of("help","info","lang","give","reset","reload"),args[0]);if(args.length==2&&args[0].equalsIgnoreCase("lang"))return filter(List.of("es","en"),args[1]);if(args.length==2&&args[0].equalsIgnoreCase("give"))return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[1]);if(args.length==3&&args[0].equalsIgnoreCase("give"))return filter(List.of("compass","lodestone","all"),args[2]);if(args.length==2&&args[0].equalsIgnoreCase("reset"))return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[1]);return List.of();}
    private List<String> filter(List<String> choices,String prefix){return choices.stream().filter(v->v.regionMatches(true,0,prefix,0,prefix.length())).toList();}
}
