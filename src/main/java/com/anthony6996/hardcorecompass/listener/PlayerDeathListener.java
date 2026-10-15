package com.anthony6996.hardcorecompass.listener;
import com.anthony6996.hardcorecompass.config.ConfigManager; import com.anthony6996.hardcorecompass.manager.*;
import org.bukkit.event.*; import org.bukkit.event.entity.PlayerDeathEvent; import org.bukkit.entity.Player; import org.bukkit.inventory.ItemStack;
public final class PlayerDeathListener implements Listener {
 private final LivesManager lives; private final CompassManager compass; private final TeleportManager teleport; private final ConfigManager config;
 public PlayerDeathListener(LivesManager l,CompassManager c,TeleportManager t,ConfigManager cfg){lives=l;compass=c;teleport=t;config=cfg;}
 @EventHandler(priority=EventPriority.HIGHEST) public void onDeath(PlayerDeathEvent e){Player p=e.getEntity(); lives.initialize(p.getUniqueId()); ItemStack bound=compass.findBoundCompass(p); e.getDrops().clear(); e.setDroppedExp(0); int remaining=lives.consume(p.getUniqueId()); p.getEnderChest().clear(); p.setExp(0); p.setLevel(0); p.setTotalExperience(0); p.setBedSpawnLocation(null,true); if(remaining>0 && bound!=null) e.getDrops().add(bound); if(remaining>0) e.setDeathMessage(config.message("death-one").replace("{lives}",String.valueOf(remaining))); else e.setDeathMessage(config.message("death-final")); teleport.relocate(p); }
}
