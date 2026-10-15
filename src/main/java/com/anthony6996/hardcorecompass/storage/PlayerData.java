package com.anthony6996.hardcorecompass.storage;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** UUID-keyed persistent player data, serialized using an atomic replace. */
public final class PlayerData {
    private final File file;
    private final YamlConfiguration data;
    public PlayerData(HardcoreCompassPlugin plugin) { file=new File(plugin.getDataFolder(),"players.yml");if(!file.getParentFile().exists()&&!file.getParentFile().mkdirs())plugin.getLogger().warning("Could not create plugin data directory.");data=YamlConfiguration.loadConfiguration(file); }
    private String path(UUID id){return "players."+id;}
    public int getLives(UUID id,int fallback){return data.getInt(path(id)+".lives",fallback);} public void setLives(UUID id,int lives){data.set(path(id)+".lives",lives);}
    public int getDeaths(UUID id){return data.getInt(path(id)+".deaths",0);} public int incrementDeaths(UUID id){int next=getDeaths(id)+1;data.set(path(id)+".deaths",next);return next;}
    public boolean hasPreservedCompass(UUID id){return data.getBoolean(path(id)+".hasPreservedCompass",false);} public void setHasPreservedCompass(UUID id,boolean value){data.set(path(id)+".hasPreservedCompass",value);}
    public String getLanguage(UUID id){return data.getString(path(id)+".language");} public void setLanguage(UUID id,String language){data.set(path(id)+".language",language);}
    public boolean exists(UUID id){return data.contains(path(id)+".lives");} public void reset(UUID id){data.set(path(id),null);}
    public boolean isSpawnUsed(UUID id,String coordinate){return data.getStringList(path(id)+".usedSpawns").contains(coordinate);}
    public List<String> getUsedSpawns(UUID id){return new ArrayList<>(data.getStringList(path(id)+".usedSpawns"));}
    public void addUsedSpawn(UUID id,String coordinate){List<String> points=getUsedSpawns(id);if(!points.contains(coordinate)){points.add(coordinate);data.set(path(id)+".usedSpawns",points);}}
    public void save(){File temp=new File(file.getParentFile(),"players.yml.tmp");try{data.save(temp);try{Files.move(temp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(IOException unsupported){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){throw new IllegalStateException("Could not atomically save players.yml",e);}}
}
