package com.anthony6996.hardcorecompass.listener;

import com.anthony6996.hardcorecompass.config.ConfigManager;
import org.bukkit.event.Listener;

public final class PlayerRespawnListener implements Listener {
    private final ConfigManager config;

    public PlayerRespawnListener(ConfigManager config) {
        this.config = config;
    }
}
