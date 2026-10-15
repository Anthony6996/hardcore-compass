package com.anthony6996.hardcorecompass.listener;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import com.anthony6996.hardcorecompass.manager.LivesManager;
import org.bukkit.event.*; import org.bukkit.event.player.PlayerJoinEvent; import org.bukkit.entity.Player;
public final class PlayerJoinListener implements Listener {
 private final LivesManager lives; private final CompassManager compass; private final ConfigManager config;
 public PlayerJoinListener(LivesManager l,CompassManager c,ConfigManager cfg){lives=l;compass=c;config=cfg;}
 @EventHandler public void onJoin(PlayerJoinEvent e){Player p=e.getPlayer(); boolean first=!lives.isInitialized(p.getUniqueId()); lives.initialize(p.getUniqueId()); if(first){compass.giveStarter(p); p.sendMessage(config.getMessage(p,"first-join").replace("{lives}",String.valueOf(lives.lives(p.getUniqueId()))));}}
}
