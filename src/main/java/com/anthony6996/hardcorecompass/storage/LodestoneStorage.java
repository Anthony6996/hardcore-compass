package com.anthony6996.hardcorecompass.storage;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Persistent coordinate registry for placed Hardcore Lodestones. */
public final class LodestoneStorage {
    public record Entry(UUID world, int x, int y, int z, String owner, String label) {}
    private final File file;
    private final YamlConfiguration data;
    public LodestoneStorage(JavaPlugin plugin) {
        file=new File(plugin.getDataFolder(),"lodestones.yml");
        if(!file.getParentFile().exists()&&!file.getParentFile().mkdirs()) throw new IllegalStateException("Cannot create plugin data directory");
        data=YamlConfiguration.loadConfiguration(file);
    }
    private String key(Location l){return l.getWorld().getUID()+"_"+l.getBlockX()+"_"+l.getBlockY()+"_"+l.getBlockZ();}
    public synchronized boolean contains(Location l){return data.contains("lodestones."+key(l));}
    public synchronized String label(Location l){return data.getString("lodestones."+key(l)+".label");}
    public synchronized String owner(Location l){return data.getString("lodestones."+key(l)+".owner");}
    public synchronized void register(Location l,UUID owner,String label){String p="lodestones."+key(l);data.set(p+".world",l.getWorld().getUID().toString());data.set(p+".x",l.getBlockX());data.set(p+".y",l.getBlockY());data.set(p+".z",l.getBlockZ());data.set(p+".owner",owner==null?null:owner.toString());data.set(p+".label",label==null||label.isBlank()?null:label);save();}
    public synchronized boolean unregister(Location l){String p="lodestones."+key(l);if(!data.contains(p))return false;data.set(p,null);save();return true;}
    public synchronized List<Entry> inWorld(World world){List<Entry> out=new ArrayList<>();var root=data.getConfigurationSection("lodestones");if(root==null)return out;for(String k:root.getKeys(false)){String p="lodestones."+k;String wid=data.getString(p+".world");if(wid==null||!wid.equals(world.getUID().toString()))continue;try{out.add(new Entry(UUID.fromString(wid),data.getInt(p+".x"),data.getInt(p+".y"),data.getInt(p+".z"),data.getString(p+".owner"),data.getString(p+".label")));}catch(IllegalArgumentException ignored){}}return out;}
    private void save(){File temp=new File(file.getParentFile(),"lodestones.yml.tmp");try{data.save(temp);try{Files.move(temp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(IOException ex){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}catch(IOException ex){throw new IllegalStateException("Could not save lodestones.yml",ex);}}
}
