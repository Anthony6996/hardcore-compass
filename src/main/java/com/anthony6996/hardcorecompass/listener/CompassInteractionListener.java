package com.anthony6996.hardcorecompass.listener;

import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class CompassInteractionListener implements Listener {
    private final CompassManager compasses;
    private final ConfigManager config;
    public CompassInteractionListener(CompassManager compasses, ConfigManager config) { this.compasses=compasses; this.config=config; }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent event) {
        if(event.getHand()!=EquipmentSlot.HAND||event.getClickedBlock()==null||event.getClickedBlock().getType()!=org.bukkit.Material.LODESTONE||!event.getAction().isRightClick())return;
        Player player=event.getPlayer(); ItemStack held=player.getInventory().getItemInMainHand(); if(!compasses.isTagged(held))return;
        Block block=event.getClickedBlock(); compasses.bind(held,player,block.getLocation()); player.getInventory().setItemInMainHand(held);
        player.playSound(block.getLocation(),Sound.ITEM_LODESTONE_COMPASS_LOCK,1f,1f); player.sendMessage(config.getMessage(player,"compass-bound"));
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) { if(event.getBlock().getType()==org.bukkit.Material.LODESTONE)compasses.markTargetLost(event.getBlock().getWorld(),event.getBlock().getLocation()); }
}
