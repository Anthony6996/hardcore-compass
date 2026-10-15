package com.anthony6996.hardcorecompass.command;
import com.anthony6996.hardcorecompass.HardcoreCompassPlugin; import com.anthony6996.hardcorecompass.config.ConfigManager; import com.anthony6996.hardcorecompass.manager.LivesManager;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public final class CompassCommand implements CommandExecutor {
 private final HardcoreCompassPlugin plugin; private final LivesManager lives; private final ConfigManager config;
 public CompassCommand(HardcoreCompassPlugin p,LivesManager l,ConfigManager c){plugin=p;lives=l;config=c;}
 @Override public boolean onCommand(CommandSender s,Command cmd,String label,String[] a){if(a.length==0||a[0].equalsIgnoreCase("lives")){if(a.length==1&&s instanceof Player p){s.sendMessage(config.message("lives-self").replace("{lives}",String.valueOf(lives.lives(p.getUniqueId()))));return true;} if(a.length==2&&s.hasPermission("hardcorecompass.admin")){Player p=Bukkit.getPlayerExact(a[1]);if(p==null){s.sendMessage("Player must be online.");return true;}s.sendMessage(config.message("lives-other").replace("{player}",p.getName()).replace("{lives}",String.valueOf(lives.lives(p.getUniqueId()))));return true;}}
 if(a.length==3&&a[0].equalsIgnoreCase("setlives")&&s.hasPermission("hardcorecompass.admin")){Player p=Bukkit.getPlayerExact(a[1]);try{int n=Integer.parseInt(a[2]);if(p==null||n<0)throw new IllegalArgumentException();lives.set(p.getUniqueId(),n);s.sendMessage(config.message("set-lives").replace("{player}",p.getName()).replace("{lives}",String.valueOf(n)));}catch(Exception ex){s.sendMessage(config.message("usage"));}return true;}
 if(a.length==1&&a[0].equalsIgnoreCase("reload")&&s.hasPermission("hardcorecompass.admin")){plugin.reloadPluginConfig();s.sendMessage(config.message("reloaded"));return true;}s.sendMessage(config.message("usage"));return true;}
}
