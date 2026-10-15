package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.storage.PlayerData;
import java.util.UUID;

public final class LivesManager {
    private final ConfigManager config; private final PlayerData storage;
    public LivesManager(ConfigManager config, PlayerData storage) { this.config = config; this.storage = storage; }
    public int lives(UUID id) { return storage.getLives(id, config.startingLives()); }
    public boolean isInitialized(UUID id) { return storage.exists(id); }
    public void initialize(UUID id) { if (!storage.exists(id)) { storage.setLives(id, config.startingLives()); storage.save(); } }
    public int consume(UUID id) { initialize(id); int n = Math.max(0, lives(id) - 1); storage.setLives(id, n); storage.save(); return n; }
    public void set(UUID id, int n) { storage.setLives(id, Math.max(0, n)); storage.save(); }
    public void save() { storage.save(); }
}
