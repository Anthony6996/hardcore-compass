package com.anthony6996.hardcorecompass.storage;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Small UUID-keyed YAML persistence store; all methods are intended for the server thread. */
public final class PlayerData {
    private final File file;
    private final YamlConfiguration data;
    public PlayerData(HardcoreCompassPlugin plugin) {
        file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        data = YamlConfiguration.loadConfiguration(file);
    }
    public int getLives(UUID id, int fallback) { return data.getInt(id.toString() + ".lives", fallback); }
    public void setLives(UUID id, int lives) { data.set(id.toString() + ".lives", lives); }
    public boolean exists(UUID id) { return data.contains(id.toString() + ".lives"); }
    public void save() { try { data.save(file); } catch (IOException e) { throw new IllegalStateException("Could not save players.yml", e); } }
}
