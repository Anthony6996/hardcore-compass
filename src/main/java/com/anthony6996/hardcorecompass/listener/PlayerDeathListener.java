package com.anthony6996.hardcorecompass.listener;

import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import com.anthony6996.hardcorecompass.manager.LivesManager;
import com.anthony6996.hardcorecompass.manager.TeleportManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public final class PlayerDeathListener implements Listener {
    private final LivesManager lives;
    private final CompassManager compass;
    private final TeleportManager teleport;
    private final ConfigManager config;

    public PlayerDeathListener(LivesManager lives, CompassManager compass, TeleportManager teleport, ConfigManager config) {
        this.lives = lives;
        this.compass = compass;
        this.teleport = teleport;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        lives.initialize(player.getUniqueId());
        ItemStack preservedCompass = compass.findTaggedCompass(player);
        int deathCount = lives.deaths(player.getUniqueId()) + 1;
        boolean preserve = deathCount == 1 && preservedCompass != null
                && lives.markCompassPreserved(player.getUniqueId());

        int remaining = lives.recordDeath(player.getUniqueId());
        if (config.wipeInventory()) event.getDrops().clear();
        if (preserve) {
            event.getItemsToKeep().add(preservedCompass.clone());
            event.setKeepInventory(true);
        }
        event.setDroppedExp(config.clearXp() ? 0 : event.getDroppedExp());
        if (config.clearEnderChest()) player.getEnderChest().clear();
        if (config.clearXp()) {
            player.setExp(0);
            player.setLevel(0);
            player.setTotalExperience(0);
        }
        if (config.clearBedSpawn()) player.setBedSpawnLocation(null, true);

        if (remaining > 0) {
            event.setDeathMessage(config.getMessage(player, "death-one").replace("{lives}", String.valueOf(remaining)));
        } else {
            event.setDeathMessage(config.getMessage(player, "death-final"));
        }
        if (config.enableRtp()) teleport.relocate(player);
    }
}
