package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.storage.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Chooses safe destinations while preferring separation from previously used player spawns. */
public final class TeleportManager {
    private final HardcoreCompassPlugin plugin;
    private final ConfigManager config;
    private final PlayerData data;
    public TeleportManager(HardcoreCompassPlugin plugin, ConfigManager config, PlayerData data) { this.plugin=plugin; this.config=config; this.data=data; }

    public void relocate(Player player) {
        Location death=player.getLocation().clone(); World world=death.getWorld();
        if(world==null){player.sendMessage(config.message("no-world"));return;}
        Bukkit.getScheduler().runTask(plugin,()->{
            List<String> previous=data.getUsedSpawns(player.getUniqueId());
            int attempts=Math.max(config.attempts(),1); Location best=null; double bestDistance=-1;
            for(int i=0;i<attempts;i++){
                int[] point=randomTarget(world,death.getX(),death.getZ()); Location candidate=findSafe(world,point[0],point[1]);
                if(candidate==null)continue;
                double distance=nearestUsedDistance(candidate,previous);
                if(distance>bestDistance){best=candidate;bestDistance=distance;}
                if(distance>=config.spawnMinDistance())break;
            }
            if(best==null){
                // Last-resort safe landing at/near the death location guarantees a teleport if the world has any safe surface.
                best=findSafe(world,death.getBlockX(),death.getBlockZ());
                if(best==null){int x=world.getSpawnLocation().getBlockX(),z=world.getSpawnLocation().getBlockZ();best=findSafe(world,x,z);}
            }
            if(best==null){plugin.getLogger().severe("No safe surface exists for RTP in world "+world.getName()+"; unable to choose a safe teleport for "+player.getUniqueId());return;}
            data.addUsedSpawn(player.getUniqueId(),key(best)); data.save();
            final Location destination=best;
            if(player.isOnline())player.teleportAsync(destination).thenAccept(ok->{if(!ok)plugin.getLogger().warning("Async RTP teleport failed for "+player.getUniqueId()+" to "+destination);});
        });
    }
    private double nearestUsedDistance(Location candidate,List<String> previous){double nearest=Double.POSITIVE_INFINITY;String prefix=candidate.getWorld().getUID()+":";for(String entry:previous){if(!entry.startsWith(prefix))continue;String[] parts=entry.substring(prefix.length()).split(":");if(parts.length<2)continue;try{double dx=candidate.getX()-(Integer.parseInt(parts[0])+.5),dz=candidate.getZ()-(Integer.parseInt(parts[1])+.5);nearest=Math.min(nearest,Math.hypot(dx,dz));}catch(NumberFormatException ignored){}}return nearest;}
    private String key(Location l){return l.getWorld().getUID()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ();}
    private int[] randomTarget(World world,double cx,double cz){int min=config.minRadius(),max=config.maxRadius();double angle=ThreadLocalRandom.current().nextDouble(0,Math.PI*2);double radius=min==max?min:Math.sqrt(ThreadLocalRandom.current().nextDouble((double)min*min,(double)max*max));int x=(int)Math.round(cx+Math.cos(angle)*radius),z=(int)Math.round(cz+Math.sin(angle)*radius);WorldBorder border=world.getWorldBorder();double half=Math.max(0,border.getSize()/2-config.borderMargin());x=(int)Math.max(border.getCenter().getX()-half,Math.min(border.getCenter().getX()+half,x));z=(int)Math.max(border.getCenter().getZ()-half,Math.min(border.getCenter().getZ()+half,z));return new int[]{x,z};}
    private Location findSafe(World world,int x,int z){int radius=config.searchRadius();for(int d=0;d<=radius;d++)for(int dx=-d;dx<=d;dx++)for(int dz=-d;dz<=d;dz++){if(Math.max(Math.abs(dx),Math.abs(dz))!=d)continue;int bx=x+dx,bz=z+dz,y=world.getHighestBlockYAt(bx,bz,HeightMap.MOTION_BLOCKING_NO_LEAVES);if(y<world.getMinHeight()+1||y>=world.getMaxHeight()-1)continue;Block ground=world.getBlockAt(bx,y-1,bz),feet=world.getBlockAt(bx,y,bz),head=world.getBlockAt(bx,y+1,bz);if(!ground.getType().isSolid()||ground.isLiquid()||!feet.isPassable()||!head.isPassable()||feet.isLiquid()||head.isLiquid())continue;if(config.avoidLava()&&(ground.getType()==Material.LAVA||feet.getType()==Material.LAVA||head.getType()==Material.LAVA))continue;if(config.avoidVoid()&&y<=world.getMinHeight()+1)continue;return new Location(world,bx+.5,y,bz+.5);}return null;}
}
