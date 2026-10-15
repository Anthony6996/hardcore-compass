package com.anthony6996.hardcorecompass;

import com.anthony6996.hardcorecompass.command.CompassCommand;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.listener.PlayerDeathListener;
import com.anthony6996.hardcorecompass.listener.PlayerJoinListener;
import com.anthony6996.hardcorecompass.listener.PlayerRespawnListener;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import com.anthony6996.hardcorecompass.manager.LivesManager;
import com.anthony6996.hardcorecompass.manager.TeleportManager;
import com.anthony6996.hardcorecompass.storage.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class HardcoreCompassPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private LivesManager livesManager;
    private CompassManager compassManager;
    private TeleportManager teleportManager;
    @Override public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        PlayerData storage = new PlayerData(this);
        livesManager = new LivesManager(configManager, storage);
        compassManager = new CompassManager(this, configManager);
        teleportManager = new TeleportManager(this, configManager);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(livesManager, compassManager, configManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(livesManager, compassManager, teleportManager, configManager), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(configManager), this);
        CompassCommand command = new CompassCommand(this, livesManager, configManager);
        getCommand("hcompass").setExecutor(command);
        getCommand("hcompass").setTabCompleter(command);
        if (configManager.debugInfo()) Bukkit.getOnlinePlayers().forEach(p -> p.setReducedDebugInfo(true));
        getLogger().info("HardcoreCompass enabled for Paper/Purpur 1.21.1.");
    }
    @Override public void onDisable() { if (livesManager != null) livesManager.save(); }
    public void reloadPluginConfig() { reloadConfig(); configManager.reload(); }
}
