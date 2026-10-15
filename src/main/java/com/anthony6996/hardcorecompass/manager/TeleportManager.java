package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;

/** Chooses a random horizontal target, then performs block inspection on the main thread. */
public final class TeleportManager {
    private final HardcoreCompassPlugin plugin; private final ConfigManager config;
    public TeleportManager(HardcoreCompassPlugin plugin, ConfigManager config) { this.plugin=plugin; this.config=config; }
    public void relocate(Player player) {
        World world = Bukkit.getWorld(config.worldName());
        if (world == null) { player.sendMessage(config.message("no-world")); return; }
        int min=config.minRadius(), max=config.maxRadius(); double angle=ThreadLocalRandom.current().nextDouble(0, Math.PI*2); double radius=Math.sqrt(ThreadLocalRandom.current().nextDouble((double)min*min,(double)max*max));
        int x=world.getSpawnLocation().getBlockX()+(int)(Math.cos(angle)*radius), z=world.getSpawnLocation().getBlockZ()+(int)(Math.sin(angle)*radius);
        WorldBorder border=world.getWorldBorder(); double half=border.getSize()/2.0-config.borderMargin(); x=(int)Math.max(border.getCenter().getX()-half,Math.min(border.getCenter().getX()+half,x)); z=(int)Math.max(border.getCenter().getZ()-half,Math.min(border.getCenter().getZ()+half,z));
        final int tx=x,tz=z;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location safe=findSafe(world,tx,tz);
            if (safe != null && player.isOnline()) player.teleportAsync(safe);
        });
    }
    private Location findSafe(World w,int x,int z) {
        int r=config.searchRadius();
        for(int d=0;d<=r;d++) for(int dx=-d;dx<=d;dx++) for(int dz=-d;dz<=d;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=d) continue;
            int bx=x+dx,bz=z+dz; int y=w.getHighestBlockYAt(bx,bz,HeightMap.MOTION_BLOCKING_NO_LEAVES); if(y<w.getMinHeight()+1) continue;
            Block ground=w.getBlockAt(bx,y-1,bz), feet=w.getBlockAt(bx,y,bz), head=w.getBlockAt(bx,y+1,bz);
            if(!ground.getType().isSolid()||!feet.isPassable()||!head.isPassable()) continue;
            if(config.avoidWater()&&(ground.isLiquid()||feet.isLiquid()||head.isLiquid())) continue;
            if(config.avoidLava()&&(ground.getType()==Material.LAVA||feet.getType()==Material.LAVA||head.getType()==Material.LAVA)) continue;
            if(config.avoidVoid()&&y<=w.getMinHeight()+1) continue;
            return new Location(w,bx+0.5,y,bz+0.5);
        }
        return null;
    }
}
