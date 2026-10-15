package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Safe surface search is bounded and runs on the server thread; final teleport uses Paper's async API. */
public final class TeleportManager {
    private final HardcoreCompassPlugin plugin;
    private final ConfigManager config;

    public TeleportManager(HardcoreCompassPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void relocate(Player player) {
        Location deathLocation = player.getLocation().clone();
        World world = deathLocation.getWorld();
        if (world == null) {
            player.sendMessage(config.message("no-world"));
            plugin.getLogger().warning("RTP skipped; death world unavailable for " + player.getUniqueId());
            return;
        }
        int attempts = config.attempts();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location chosen = null;
            int tx = deathLocation.getBlockX();
            int tz = deathLocation.getBlockZ();
            for (int i = 0; i < attempts && chosen == null; i++) {
                int[] target = randomTarget(world, deathLocation.getX(), deathLocation.getZ());
                tx = target[0];
                tz = target[1];
                chosen = findSafe(world, tx, tz);
            }
            if (chosen == null) {
                Location fallback = findSafe(world, deathLocation.getBlockX(), deathLocation.getBlockZ());
                if (fallback == null) fallback = deathLocation.clone();
                chosen = fallback;
                plugin.getLogger().warning("RTP exhausted " + attempts + " attempts for " + player.getUniqueId()
                        + " near death-location target " + tx + "," + tz + "; using death-location fallback.");
            }
            if (player.isOnline()) {
                final Location destination = chosen;
                player.teleportAsync(destination).thenAccept(ok -> {
                    if (!ok) plugin.getLogger().warning("Async RTP teleport failed for " + player.getUniqueId() + " to " + destination);
                });
            }
        });
    }

    private int[] randomTarget(World world, double centerX, double centerZ) {
        int min = config.minRadius();
        int max = config.maxRadius();
        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        double radius = min == max ? min : Math.sqrt(ThreadLocalRandom.current().nextDouble((double) min * min, (double) max * max));
        int x = (int) Math.round(centerX + Math.cos(angle) * radius);
        int z = (int) Math.round(centerZ + Math.sin(angle) * radius);
        WorldBorder border = world.getWorldBorder();
        double half = Math.max(0, border.getSize() / 2 - config.borderMargin());
        x = (int) Math.max(border.getCenter().getX() - half, Math.min(border.getCenter().getX() + half, x));
        z = (int) Math.max(border.getCenter().getZ() - half, Math.min(border.getCenter().getZ() + half, z));
        return new int[]{x, z};
    }

    private Location findSafe(World world, int x, int z) {
        int radius = config.searchRadius();
        for (int distance = 0; distance <= radius; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != distance) continue;
                    int blockX = x + dx;
                    int blockZ = z + dz;
                    int y = world.getHighestBlockYAt(blockX, blockZ, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                    if (y < world.getMinHeight() + 1 || y >= world.getMaxHeight() - 1) continue;
                    Block ground = world.getBlockAt(blockX, y - 1, blockZ);
                    Block feet = world.getBlockAt(blockX, y, blockZ);
                    Block head = world.getBlockAt(blockX, y + 1, blockZ);
                    if (!ground.getType().isSolid() || !feet.isPassable() || !head.isPassable()) continue;
                    if (config.avoidWater() && (ground.isLiquid() || feet.isLiquid() || head.isLiquid())) continue;
                    if (config.avoidLava() && (ground.getType() == Material.LAVA || feet.getType() == Material.LAVA || head.getType() == Material.LAVA)) continue;
                    if (config.avoidVoid() && y <= world.getMinHeight() + 1) continue;
                    return new Location(world, blockX + 0.5, y, blockZ + 0.5);
                }
            }
        }
        return null;
    }
}
