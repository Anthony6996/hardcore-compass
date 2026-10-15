package com.anthony6996.hardcorecompass;

import com.anthony6996.hardcorecompass.command.CompassCommand;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.listener.CompassCraftListener;
import com.anthony6996.hardcorecompass.listener.CompassInteractionListener;
import com.anthony6996.hardcorecompass.listener.PlayerDeathListener;
import com.anthony6996.hardcorecompass.listener.PlayerJoinListener;
import com.anthony6996.hardcorecompass.listener.PlayerRespawnListener;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import com.anthony6996.hardcorecompass.manager.LivesManager;
import com.anthony6996.hardcorecompass.manager.TeleportManager;
import com.anthony6996.hardcorecompass.storage.PlayerData;
import org.bukkit.plugin.java.JavaPlugin;

public final class HardcoreCompassPlugin extends JavaPlugin {
    private LivesManager livesManager;
    private CompassManager compassManager;
    private PlayerData playerData;
    @Override public void onEnable() {
        saveDefaultConfig();
        ConfigManager configManager = new ConfigManager(this);
        playerData = new PlayerData(this);
        livesManager = new LivesManager(configManager, playerData);
        compassManager = new CompassManager(this, configManager);
        compassManager.registerRecipe();
        TeleportManager teleportManager = new TeleportManager(this, configManager);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(livesManager, compassManager, configManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(livesManager, compassManager, teleportManager, configManager), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(configManager), this);
        getServer().getPluginManager().registerEvents(new CompassInteractionListener(compassManager, configManager), this);
        getServer().getPluginManager().registerEvents(new CompassCraftListener(compassManager, configManager), this);
        CompassCommand command = new CompassCommand(this, livesManager, configManager, compassManager);
        getCommand("hcompass").setExecutor(command);
        getCommand("hcompass").setTabCompleter(command);
        getLogger().info("HardcoreCompass enabled for Paper/Purpur 1.21.1.");
    }
    @Override public void onDisable(){if(livesManager!=null)livesManager.save();}
    public PlayerData getPlayerData(){return playerData;}
    public void reloadPluginConfig(){reloadConfig();}
}
