package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import org.bukkit.*; import org.bukkit.block.Block; import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;

/** Safe surface search is bounded and runs on the server thread; the final teleport is async. */
public final class TeleportManager {
 private final HardcoreCompassPlugin plugin; private final ConfigManager config;
 public TeleportManager(HardcoreCompassPlugin plugin,ConfigManager config){this.plugin=plugin;this.config=config;}
 public void relocate(Player player){World world=Bukkit.getWorld(config.worldName());if(world==null){player.sendMessage(config.message("no-world"));plugin.getLogger().warning("RTP skipped; configured world not found: "+config.worldName()+" for "+player.getUniqueId());return;}int attempts=config.attempts();
  Bukkit.getScheduler().runTask(plugin,()->{Location chosen=null;int tx=0,tz=0;for(int i=0;i<attempts&&chosen==null;i++){int[] target=randomTarget(world);tx=target[0];tz=target[1];chosen=findSafe(world,tx,tz);}if(chosen==null){Location spawn=world.getSpawnLocation();chosen=findSafe(world,spawn.getBlockX(),spawn.getBlockZ());if(chosen==null)chosen=spawn.clone().add(.5,0,.5);plugin.getLogger().warning("RTP exhausted "+attempts+" attempts for "+player.getUniqueId()+" near target "+tx+","+tz+"; using world spawn fallback.");}if(player.isOnline())player.teleportAsync(chosen).thenAccept(ok->{if(!ok)plugin.getLogger().warning("Async RTP teleport failed for "+player.getUniqueId()+" to "+chosen);});});
 }
 private int[] randomTarget(World w){int min=config.minRadius(),max=config.maxRadius();double angle=ThreadLocalRandom.current().nextDouble(0,Math.PI*2);double radius=Math.sqrt(ThreadLocalRandom.current().nextDouble((double)min*min,(double)max*max));int x=w.getSpawnLocation().getBlockX()+(int)(Math.cos(angle)*radius),z=w.getSpawnLocation().getBlockZ()+(int)(Math.sin(angle)*radius);WorldBorder b=w.getWorldBorder();double half=Math.max(0,b.getSize()/2-config.borderMargin());x=(int)Math.max(b.getCenter().getX()-half,Math.min(b.getCenter().getX()+half,x));z=(int)Math.max(b.getCenter().getZ()-half,Math.min(b.getCenter().getZ()+half,z));return new int[]{x,z};}
 private Location findSafe(World w,int x,int z){int r=config.searchRadius();for(int d=0;d<=r;d++)for(int dx=-d;dx<=d;dx++)for(int dz=-d;dz<=d;dz++){if(Math.max(Math.abs(dx),Math.abs(dz))!=d)continue;int bx=x+dx,bz=z+dz;int y=w.getHighestBlockYAt(bx,bz,HeightMap.MOTION_BLOCKING_NO_LEAVES);if(y<w.getMinHeight()+1||y>=w.getMaxHeight()-1)continue;Block ground=w.getBlockAt(bx,y-1,bz),feet=w.getBlockAt(bx,y,bz),head=w.getBlockAt(bx,y+1,bz);if(!ground.getType().isSolid()||!feet.isPassable()||!head.isPassable())continue;if(config.avoidWater()&&(ground.isLiquid()||feet.isLiquid()||head.isLiquid()))continue;if(config.avoidLava()&&(ground.getType()==Material.LAVA||feet.getType()==Material.LAVA||head.getType()==Material.LAVA))continue;if(config.avoidVoid()&&y<=w.getMinHeight()+1)continue;return new Location(w,bx+.5,y,bz+.5);}return null;}
}
