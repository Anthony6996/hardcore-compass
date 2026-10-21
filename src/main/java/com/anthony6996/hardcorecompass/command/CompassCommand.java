package com.anthony6996.hardcorecompass.command;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import com.anthony6996.hardcorecompass.manager.LivesManager;
import com.anthony6996.hardcorecompass.listener.PlayerJoinListener;
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
    public CompassCommand(HardcoreCompassPlugin plugin, LivesManager lives, ConfigManager config, CompassManager compasses) { this.plugin=plugin; this.lives=lives; this.config=config; this.compasses=compasses; }
    private String msg(CommandSender sender, String key) { return config.localized(sender instanceof Player p ? config.language(p) : config.defaultLanguage(), key); }
    private boolean admin(CommandSender sender) { return sender.hasPermission("hardcorecompass.admin"); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return sendHelp(sender);
        String sub=args[0].toLowerCase(Locale.ROOT);
        if (Set.of("libro","givebook").contains(sub)) {
            if (!admin(sender)) return noPermission(sender);
            if (args.length != 2) { sender.sendMessage("§e/hcompass libro <jugador>"); return true; }
            Player target=Bukkit.getPlayerExact(args[1]);
            if (target==null) { sender.sendMessage(msg(sender,"player-not-found")); return true; }
            new PlayerJoinListener(lives,compasses,config).giveGuideBook(target);
            sender.sendMessage(msg(sender,"book-given").replace("{player}",target.getName())); return true;
        }
        if (Set.of("crafteo","togglecraft").contains(sub)) {
            if (!admin(sender)) return noPermission(sender);
            if (args.length == 1 || args.length == 2 && args[1].equalsIgnoreCase("toggle")) {
                boolean enabled=!config.compassCraftingEnabled(); config.setCompassCraftingEnabled(enabled);
                sender.sendMessage(msg(sender,"craft-toggle").replace("{status}",enabled?"activado":"desactivado")); return true;
            }
            if (args.length == 2) {
                Player target=Bukkit.getPlayerExact(args[1]);
                if (target==null) { sender.sendMessage(msg(sender,"player-not-found")); return true; }
                boolean allowed=target.hasPermission("hardcorecompass.craft");
                sender.sendMessage(msg(sender,"craft-player-status").replace("{player}",target.getName()).replace("{status}",allowed?"permitido":"denegado")); return true;
            }
            sender.sendMessage(msg(sender,"craft-usage")); return true;
        }
        switch(sub) {
            case "help" -> { return sendHelp(sender); }
            case "lang" -> { if (!(sender instanceof Player p)) { sender.sendMessage(msg(sender,"lang-usage")); return true; } if (args.length==1) { String current=plugin.getPlayerData().getLanguage(p.getUniqueId()); sender.sendMessage(msg(p,current==null?"lang-current-default":"lang-current").replace("{language}",current==null?config.defaultLanguage():current)); return true; } if (args.length!=2) { sender.sendMessage(msg(sender,"lang-usage")); return true; } String lang=args[1].toLowerCase(Locale.ROOT); if (!Set.of("en","es").contains(lang)) { sender.sendMessage(msg(sender,"lang-usage")); return true; } plugin.getPlayerData().setLanguage(p.getUniqueId(),lang); plugin.getPlayerData().save(); sender.sendMessage(msg(p,"lang-set").replace("{language}",lang)); return true; }
            case "info" -> { if(args.length>2)return sendHelp(sender); if(args.length==2&&!admin(sender))return noPermission(sender); Player target=args.length==2?Bukkit.getPlayerExact(args[1]):sender instanceof Player p?p:null; if(target==null){sender.sendMessage(msg(sender,"player-not-found"));return true;} sender.sendMessage(msg(sender,"info").replace("{player}",target.getName()).replace("{lives",""+lives.lives(target.getUniqueId()))); return true; }
            case "reload" -> { if(!admin(sender))return noPermission(sender); plugin.reloadPluginConfig(); sender.sendMessage(msg(sender,"reloaded")); return true; }
            case "give" -> { if(!admin(sender))return noPermission(sender); String kind=args.length>1?args[1].toLowerCase(Locale.ROOT):"all"; Player target=args.length>2?Bukkit.getPlayerExact(args[2]):sender instanceof Player p?p:null; if(target==null){sender.sendMessage(msg(sender,"player-not-found"));return true;} if(kind.equals("compass")||kind.equals("all"))give(target,compasses.createCompass(null,target)); if(kind.equals("lodestone")||kind.equals("all"))give(target,compasses.createLodestone(target)); sender.sendMessage(msg(sender,"give-success").replace("{player}",target.getName()).replace("{item}",kind)); return true; }
            default -> { return sendHelp(sender); }
        }
    }
    private boolean noPermission(CommandSender sender){sender.sendMessage(msg(sender,"no-permission"));return true;}
    private boolean sendHelp(CommandSender sender){sender.sendMessage(msg(sender,admin(sender)?"help-admin":"help-player"));return true;}
    private void give(Player player,ItemStack item){Map<Integer,ItemStack> extra=player.getInventory().addItem(item);extra.values().forEach(x->player.getWorld().dropItemNaturally(player.getLocation(),x));}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        boolean isAdmin=admin(sender);
        if(args.length==1){List<String> options=new ArrayList<>(List.of("help","ayuda","lang","idioma","info","informacion"));if(isAdmin)options.addAll(List.of("libro","givebook","crafteo","togglecraft","give","reload"));return filter(options,args[0]);}
        String sub=args[0].toLowerCase(Locale.ROOT);
        if(args.length==2&&Set.of("libro","givebook","info").contains(sub)&&isAdmin)return online(args[1]);
        if(args.length==2&&Set.of("crafteo","togglecraft").contains(sub)&&isAdmin){List<String> result=new ArrayList<>(List.of("toggle"));result.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());return filter(result,args[1]);}
        if(args.length==2&&sub.equals("give")&&isAdmin)return filter(List.of("compass","lodestone","all"),args[1]);
        if(args.length==3&&sub.equals("give")&&isAdmin)return online(args[2]);
        if(args.length==2&&Set.of("lang","idioma").contains(sub)&&sender instanceof Player)return filter(List.of("en","es"),args[1]);
        return List.of();
    }
    private List<String> online(String prefix){return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),prefix);}
    private List<String> filter(List<String> list,String prefix){return list.stream().filter(s->s.regionMatches(true,0,prefix,0,prefix.length())).toList();}
}
