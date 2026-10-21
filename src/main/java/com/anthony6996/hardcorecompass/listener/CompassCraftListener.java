package com.anthony6996.hardcorecompass.listener;

import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public final class CompassCraftListener implements Listener {
    private final CompassManager compasses;
    private final ConfigManager config;

    public CompassCraftListener(CompassManager compasses, ConfigManager config) {
        this.compasses = compasses;
        this.config = config;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        Player player = event.getView().getPlayer() instanceof Player p ? p : null;
        boolean compass = compasses.isCompassRecipe(event.getRecipe());
        boolean lodestone = compasses.isLodestoneRecipe(event.getRecipe());
        if (!compass && !lodestone) return;
        if (compass && (!config.compassCraftingEnabled() || (player != null && !config.canCraftCompass(player)))) {
            event.getInventory().setResult(new ItemStack(org.bukkit.Material.AIR));
            if (player != null) player.sendMessage(config.getMessage(player, "craft-disabled"));
            return;
        }
        if (player == null) return;
        if (compass) event.getInventory().setResult(compasses.createCompass(null, player));
        else event.getInventory().setResult(compasses.createLodestone(player));
    }
}
