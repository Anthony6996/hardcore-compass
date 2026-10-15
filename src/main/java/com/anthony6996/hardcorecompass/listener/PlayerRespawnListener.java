package com.anthony6996.hardcorecompass.listener;
import com.anthony6996.hardcorecompass.config.ConfigManager; import org.bukkit.event.*; import org.bukkit.event.player.PlayerRespawnEvent;
public final class PlayerRespawnListener implements Listener {
 private final ConfigManager config; public PlayerRespawnListener(ConfigManager c){config=c;}
 @EventHandler public void onRespawn(PlayerRespawnEvent e){if(config.debugInfo()) e.getPlayer().setReducedDebugInfo(true);}
}
