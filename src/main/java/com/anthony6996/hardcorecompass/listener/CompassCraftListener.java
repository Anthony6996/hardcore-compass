package com.anthony6996.hardcorecompass.listener;

import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.entity.Player;

public final class CompassCraftListener implements Listener {
    private final CompassManager compasses;
    private final ConfigManager config;

    public CompassCraftListener(CompassManager compasses, ConfigManager config) {
        this.compasses = compasses;
        this.config = config;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null || !compasses.isRecipe(event.getRecipe())) return;
        CraftingInventory inventory = event.getInventory();
        Player player = event.getView().getPlayer() instanceof Player p ? p : null;
        inventory.setResult(compasses.createCompass(null, player));
    }
}
