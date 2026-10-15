package com.anthony6996.hardcorecompass.listener;

import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public final class PlayerRespawnListener implements Listener {
    private final ConfigManager config;
    private final CompassManager compass;

    public PlayerRespawnListener(ConfigManager config, CompassManager compass) {
        this.config = config;
        this.compass = compass;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!config.enableRtp() || !config.wipeInventory()) return;
        ItemStack preserved = compass.findTaggedCompassInDeathKeep(event.getPlayer());
        if (preserved == null) return;
        Bukkit.getScheduler().runTask(pluginOf(player), () -> {
            if (!player.isOnline()) return;
            player.getInventory().addItem(preserved).values().forEach(left ->
                    player.getWorld().dropItemNaturally(player.getLocation(), left));
        });
    }

    private org.bukkit.plugin.Plugin pluginOf(Player player) {
        return player.getServer().getPluginManager().getPlugin("HardcoreCompass");
    }
}
