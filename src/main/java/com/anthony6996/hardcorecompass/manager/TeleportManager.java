package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.storage.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;

/** Bounded safe-surface search; successful destinations are persistently unique per player. */
public final class TeleportManager {
    private final HardcoreCompassPlugin plugin; private final ConfigManager config; private final PlayerData data;
    public TeleportManager(HardcoreCompassPlugin plugin,ConfigManager config,PlayerData data){this.plugin=plugin;this.config=config;this.data=data;}
    public void relocate(Player player){Location death=player.getLocation().clone();World world=death.getWorld();if(world==null){player.sendMessage(config.message("no-world"));return;}int attempts=config.attempts();Bukkit.getScheduler().runTask(plugin,()->{Location chosen=null;for(int i=0;i<attempts&&chosen==null;i++){int[] p=randomTarget(world,death.getX(),death.getZ());chosen=findSafe(world,p[0],p[1]);if(chosen!=null&&data.isSpawnUsed(player.getUniqueId(),key(chosen)))chosen=null;}if(chosen==null){player.sendMessage(config.message("rtp-no-unique"));plugin.getLogger().warning("No unique safe RTP point found for "+player.getUniqueId());return;}String locationKey=key(chosen);data.addUsedSpawn(player.getUniqueId(),locationKey);data.save();if(player.isOnline())player.teleportAsync(chosen).thenAccept(ok->{if(!ok)plugin.getLogger().warning("RTP teleport failed for "+player.getUniqueId()+" to "+chosen);});});}
    private String key(Location l){return l.getWorld().getUID()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ();}
    private int[] randomTarget(World w,double cx,double cz){int min=config.minRadius(),max=config.maxRadius();double a=ThreadLocalRandom.current().nextDouble(0,Math.PI*2);double r=min==max?min:Math.sqrt(ThreadLocalRandom.current().nextDouble((double)min*min,(double)max*max));int x=(int)Math.round(cx+Math.cos(a)*r),z=(int)Math.round(cz+Math.sin(a)*r);WorldBorder b=w.getWorldBorder();double half=Math.max(0,b.getSize()/2-config.borderMargin());x=(int)Math.max(b.getCenter().getX()-half,Math.min(b.getCenter().getX()+half,x));z=(int)Math.max(b.getCenter().getZ()-half,Math.min(b.getCenter().getZ()+half,z));return new int[]{x,z};}
    private Location findSafe(World w,int x,int z){int radius=config.searchRadius();for(int d=0;d<=radius;d++)for(int dx=-d;dx<=d;dx++)for(int dz=-d;dz<=d;dz++){if(Math.max(Math.abs(dx),Math.abs(dz))!=d)continue;int bx=x+dx,bz=z+dz,y=w.getHighestBlockYAt(bx,bz,HeightMap.MOTION_BLOCKING_NO_LEAVES);if(y<w.getMinHeight()+1||y>=w.getMaxHeight()-1)continue;Block ground=w.getBlockAt(bx,y-1,bz),feet=w.getBlockAt(bx,y,bz),head=w.getBlockAt(bx,y+1,bz);if(!ground.getType().isSolid()||!feet.isPassable()||!head.isPassable()||feet.isLiquid()||head.isLiquid())continue;if(config.avoidWater()&&(ground.isLiquid()||feet.isLiquid()||head.isLiquid()))continue;if(config.avoidLava()&&(ground.getType()==Material.LAVA||feet.getType()==Material.LAVA||head.getType()==Material.LAVA))continue;if(config.avoidVoid()&&y<=w.getMinHeight()+1)continue;return new Location(w,bx+.5,y,bz+.5);}return null;}
}
