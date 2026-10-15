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
    private final HardcoreCompassPlugin plugin; private final LivesManager lives; private final ConfigManager config; private final CompassManager compasses;
    public CompassCommand(HardcoreCompassPlugin p,LivesManager l,ConfigManager c,CompassManager m){plugin=p;lives=l;config=c;compasses=m;}
    private String msg(CommandSender s,String k){return config.localized(s instanceof Player p?config.language(p):config.defaultLanguage(),k);}
    private boolean admin(CommandSender s){return s.hasPermission("hardcorecompass.admin");}
    @Override public boolean onCommand(CommandSender s,Command cmd,String label,String[] a){
        if(a.length==0)return sendHelp(s);
        String sub=a[0].toLowerCase(Locale.ROOT);
        switch(sub){
            case "help" -> {return sendHelp(s);}
            case "lang" -> {if(!(s instanceof Player p)){s.sendMessage(msg(s,"lang-usage"));return true;}if(a.length==1){String current=plugin.getPlayerData().getLanguage(p.getUniqueId());s.sendMessage(msg(p,current==null?"lang-current-default":"lang-current").replace("{language}",current==null?config.defaultLanguage():current));return true;}if(a.length!=2){s.sendMessage(msg(s,"lang-usage"));return true;}String lang=a[1].toLowerCase(Locale.ROOT);if(!Set.of("en","es").contains(lang)){s.sendMessage(msg(s,"lang-usage"));return true;}plugin.getPlayerData().setLanguage(p.getUniqueId(),lang);plugin.getPlayerData().save();s.sendMessage(msg(p,"lang-set").replace("{language}",lang));return true;}
            case "info" -> {if(a.length>2)return sendHelp(s);if(a.length==2&&!admin(s))return noPermission(s);Player target=a.length==2?Bukkit.getPlayerExact(a[1]):s instanceof Player p?p:null;if(target==null){s.sendMessage(msg(s,"player-not-found"));return true;}s.sendMessage(msg(s,"info").replace("{player}",target.getName()).replace("{lives}",""+lives.lives(target.getUniqueId())).replace("{deaths}",""+lives.deaths(target.getUniqueId())).replace("{compass}",""+lives.hasPreservedCompass(target.getUniqueId())));return true;}
            case "give" -> {if(!admin(s))return noPermission(s);String kind="all";Player target=null;if(a.length==1){if(s instanceof Player p)target=p;else{s.sendMessage(msg(s,"give-usage"));return true;}}else if(a.length==2){if(isKind(a[1])){kind=a[1].toLowerCase(Locale.ROOT);if(s instanceof Player p)target=p;else{s.sendMessage(msg(s,"give-usage"));return true;}}else{target=Bukkit.getPlayerExact(a[1]);if(target==null){s.sendMessage(msg(s,"player-not-found"));return true;}}}else if(a.length==3){kind=a[1].toLowerCase(Locale.ROOT);if(!isKind(kind)){s.sendMessage(msg(s,"give-usage"));return true;}target=Bukkit.getPlayerExact(a[2]);if(target==null){s.sendMessage(msg(s,"player-not-found"));return true;}}else{return sendHelp(s);}giveItems(target,kind);s.sendMessage(msg(s,kind.equals("all")?"give-success-all":"give-success").replace("{player}",target.getName()).replace("{item}",kind));return true;}
            case "givelodestone" -> {if(!admin(s))return noPermission(s);if(a.length>2)return sendHelp(s);Player t=a.length==2?Bukkit.getPlayerExact(a[1]):s instanceof Player p?p:null;if(t==null){s.sendMessage(msg(s,"player-not-found"));return true;}give(t,compasses.createLodestone(t));s.sendMessage(msg(s,"give-success").replace("{player}",t.getName()).replace("{item}","lodestone"));return true;}
            case "reset" -> {if(!admin(s))return noPermission(s);if(a.length!=3||!Set.of("lives","deaths","compass","rtp","all").contains(a[2].toLowerCase(Locale.ROOT))){s.sendMessage(msg(s,"reset-usage"));return true;}Player t=Bukkit.getPlayerExact(a[1]);if(t==null){s.sendMessage(msg(s,"player-not-found"));return true;}String what=a[2].toLowerCase(Locale.ROOT);switch(what){case "deaths"->lives.resetDeaths(t.getUniqueId());case "all"->lives.reset(t.getUniqueId());case "lives"->lives.set(t.getUniqueId(),config.startingLives());case "compass"->compasses.wipeLinks(t);case "rtp"->plugin.getPlayerData().clearUsedSpawns(t.getUniqueId());}s.sendMessage(msg(s,"reset").replace("{player}",t.getName()).replace("{type}",what));return true;}
            case "wipelinks" -> {if(!admin(s))return noPermission(s);if(a.length!=2){s.sendMessage(msg(s,"wipelinks-usage"));return true;}Player t=Bukkit.getPlayerExact(a[1]);if(t==null){s.sendMessage(msg(s,"player-not-found"));return true;}compasses.wipeLinks(t);s.sendMessage(msg(s,"wipelinks-done").replace("{player}",t.getName()));return true;}
            case "setpreserve" -> {if(!admin(s))return noPermission(s);if(a.length!=2||!Set.of("true","false").contains(a[1].toLowerCase(Locale.ROOT))){s.sendMessage(msg(s,"preserve-usage"));return true;}config.setPreserveOnFirstDeath(Boolean.parseBoolean(a[1]));s.sendMessage(msg(s,"preserve-set").replace("{value}",a[1]));return true;}
            case "radius" -> {if(!admin(s))return noPermission(s);if(a.length!=3){s.sendMessage(msg(s,"radius-usage"));return true;}try{int min=Integer.parseInt(a[1]),max=Integer.parseInt(a[2]);if(min<0||max<min)throw new NumberFormatException();config.setRadius(min,max);s.sendMessage(msg(s,"radius-set").replace("{min}",""+min).replace("{max}",""+max));}catch(NumberFormatException ex){s.sendMessage(msg(s,"radius-usage"));}return true;}
            case "reload" -> {if(!admin(s))return noPermission(s);if(a.length!=1)return sendHelp(s);plugin.reloadPluginConfig();s.sendMessage(msg(s,"reloaded"));return true;}
            default -> {return sendHelp(s);}
        }
    }
    private boolean isKind(String x){return Set.of("compass","lodestone","all").contains(x.toLowerCase(Locale.ROOT));}
    private void giveItems(Player p,String kind){if(kind.equals("compass")||kind.equals("all"))give(p,compasses.createCompass(null,p));if(kind.equals("lodestone")||kind.equals("all"))give(p,compasses.createLodestone(p));}
    private boolean noPermission(CommandSender s){s.sendMessage(msg(s,"no-permission"));return true;}
    private boolean sendHelp(CommandSender s){s.sendMessage(msg(s,admin(s)?"help-admin":"help-player"));return true;}
    private void give(Player p,ItemStack i){Map<Integer,ItemStack> extra=p.getInventory().addItem(i);extra.values().forEach(x->p.getWorld().dropItemNaturally(p.getLocation(),x));}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] args){boolean isAdmin=admin(s);if(args.length==1){List<String> options=new ArrayList<>(List.of("help","lang","info"));if(isAdmin)options.addAll(List.of("give","givelodestone","reset","wipelinks","setpreserve","radius","reload"));return filter(options,args[0]);}String sub=args[0].toLowerCase(Locale.ROOT);if(sub.equals("give")&&isAdmin){if(args.length==2)return filter(new ArrayList<>(List.of("compass","lodestone","all")),args[1]);if(args.length==3)return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[2]);}if(args.length==2&&Set.of("reset","wipelinks","givelodestone").contains(sub)&&isAdmin)return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[1]);if(args.length==2&&sub.equals("lang")&&s instanceof Player)return filter(List.of("en","es"),args[1]);if(args.length==3&&sub.equals("reset")&&isAdmin)return filter(List.of("lives","deaths","compass","rtp","all"),args[2]);if(args.length==2&&sub.equals("setpreserve")&&isAdmin)return filter(List.of("true","false"),args[1]);return List.of();}
    private List<String> filter(List<String> src,String prefix){return src.stream().filter(x->x.regionMatches(true,0,prefix,0,prefix.length())).toList();}
}
