package com.anthony6996.hardcorecompass.listener;

import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import com.anthony6996.hardcorecompass.manager.LivesManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import java.util.List;

public final class PlayerJoinListener implements Listener {
    private final LivesManager lives;
    private final CompassManager compass;
    private final ConfigManager config;

    public PlayerJoinListener(LivesManager lives, CompassManager compass, ConfigManager config) {
        this.lives = lives;
        this.compass = compass;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        boolean first = !lives.isInitialized(player.getUniqueId());
        lives.initialize(player.getUniqueId());
        compass.refreshPlayer(player);
        if (first) {
            compass.giveStarter(player);
            player.sendMessage(config.getMessage(player, "first-join").replace("{lives}", String.valueOf(lives.lives(player.getUniqueId()))));
        }
        if (!player.hasPlayedBefore() && config.guideBookEnabled()) {
            giveGuideBook(player);
        }
    }

    public void giveGuideBook(org.bukkit.entity.Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;
        meta.setTitle(config.guideBookTitle());
        meta.setAuthor(config.guideBookAuthor());
        List<String> pages = config.guideBookPages();
        if (!pages.isEmpty()) meta.setPages(pages);
        book.setItemMeta(meta);
        var leftovers = player.getInventory().addItem(book);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }
}
