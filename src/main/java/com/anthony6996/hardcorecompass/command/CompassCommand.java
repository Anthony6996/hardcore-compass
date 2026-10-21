package com.anthony6996.hardcorecompass.command;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import com.anthony6996.hardcorecompass.listener.PlayerJoinListener;
import com.anthony6996.hardcorecompass.manager.CompassManager;
import com.anthony6996.hardcorecompass.manager.LivesManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public final class CompassCommand implements CommandExecutor, TabCompleter {
    private final HardcoreCompassPlugin plugin;
    private final LivesManager lives;
    private final ConfigManager config;
    private final CompassManager compasses;

    public CompassCommand(HardcoreCompassPlugin plugin, LivesManager lives, ConfigManager config, CompassManager compasses) {
        this.plugin = plugin;
        this.lives = lives;
        this.config = config;
        this.compasses = compasses;
    }

    private String msg(CommandSender sender, String key) {
        return config.localized(sender instanceof Player player ? config.language(player) : config.defaultLanguage(), key);
    }

    private boolean admin(CommandSender sender) { return sender.hasPermission("hardcorecompass.admin"); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return sendHelp(sender);
        String sub = canonical(args[0]);
        switch (sub) {
            case "help" -> { return sendHelp(sender); }
            case "lang" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(msg(sender, "lang-usage")); return true; }
                if (args.length == 1) {
                    String current = plugin.getPlayerData().getLanguage(player.getUniqueId());
                    sender.sendMessage(msg(player, current == null ? "lang-current-default" : "lang-current").replace("{language}", current == null ? config.defaultLanguage() : current));
                    return true;
                }
                if (args.length != 2) { sender.sendMessage(msg(sender, "lang-usage")); return true; }
                String language = args[1].toLowerCase(Locale.ROOT);
                if (!Set.of("en", "es").contains(language)) { sender.sendMessage(msg(sender, "lang-usage")); return true; }
                plugin.getPlayerData().setLanguage(player.getUniqueId(), language);
                plugin.getPlayerData().save();
                sender.sendMessage(msg(player, "lang-set").replace("{language}", language));
                return true;
            }
            case "info" -> {
                if (args.length > 2) return sendHelp(sender);
                if (args.length == 2 && !admin(sender)) return noPermission(sender);
                Player target = args.length == 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player player ? player : null;
                if (target == null) { sender.sendMessage(msg(sender, "player-not-found")); return true; }
                sender.sendMessage(msg(sender, "info").replace("{player}", target.getName())
                    .replace("{lives}", String.valueOf(lives.lives(target.getUniqueId())))
                    .replace("{deaths}", String.valueOf(lives.deaths(target.getUniqueId())))
                    .replace("{compass}", String.valueOf(lives.hasPreservedCompass(target.getUniqueId()))));
                return true;
            }
            case "give" -> {
                if (!admin(sender)) return noPermission(sender);
                String kind = "all";
                Player target;
                if (args.length == 1) {
                    if (!(sender instanceof Player player)) { sender.sendMessage(msg(sender, "give-usage")); return true; }
                    target = player;
                } else if (args.length == 2) {
                    if (isKind(args[1])) {
                        kind = args[1].toLowerCase(Locale.ROOT);
                        if (!(sender instanceof Player player)) { sender.sendMessage(msg(sender, "give-usage")); return true; }
                        target = player;
                    } else {
                        target = Bukkit.getPlayerExact(args[1]);
                        if (target == null) { sender.sendMessage(msg(sender, "player-not-found")); return true; }
                    }
                } else if (args.length == 3) {
                    kind = args[1].toLowerCase(Locale.ROOT);
                    if (!isKind(kind)) { sender.sendMessage(msg(sender, "give-usage")); return true; }
                    target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) { sender.sendMessage(msg(sender, "player-not-found")); return true; }
                } else return sendHelp(sender);
                giveItems(target, kind);
                sender.sendMessage(msg(sender, kind.equals("all") ? "give-success-all" : "give-success").replace("{player}", target.getName()).replace("{item}", kind));
                return true;
            }
            case "givelodestone" -> {
                if (!admin(sender)) return noPermission(sender);
                if (args.length > 2) return sendHelp(sender);
                Player target = args.length == 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player player ? player : null;
                if (target == null) { sender.sendMessage(msg(sender, "player-not-found")); return true; }
                give(target, compasses.createLodestone(target));
                sender.sendMessage(msg(sender, "give-success").replace("{player}", target.getName()).replace("{item}", "lodestone"));
                return true;
            }
            case "reset" -> {
                if (!admin(sender)) return noPermission(sender);
                if (args.length != 3 || !Set.of("lives", "deaths", "compass", "rtp", "all").contains(args[2].toLowerCase(Locale.ROOT))) { sender.sendMessage(msg(sender, "reset-usage")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(msg(sender, "player-not-found")); return true; }
                String type = args[2].toLowerCase(Locale.ROOT);
                switch (type) {
                    case "deaths" -> lives.resetDeaths(target.getUniqueId());
                    case "all" -> lives.reset(target.getUniqueId());
                    case "lives" -> lives.set(target.getUniqueId(), config.startingLives());
                    case "compass" -> compasses.wipeLinks(target);
                    case "rtp" -> plugin.getPlayerData().clearUsedSpawns(target.getUniqueId());
                }
                sender.sendMessage(msg(sender, "reset").replace("{player}", target.getName()).replace("{type}", type));
                return true;
            }
            case "wipelinks" -> {
                if (!admin(sender)) return noPermission(sender);
                if (args.length != 2) { sender.sendMessage(msg(sender, "wipelinks-usage")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(msg(sender, "player-not-found")); return true; }
                compasses.wipeLinks(target);
                sender.sendMessage(msg(sender, "wipelinks-done").replace("{player}", target.getName()));
                return true;
            }
            case "setpreserve" -> {
                if (!admin(sender)) return noPermission(sender);
                if (args.length != 2 || !Set.of("true", "false").contains(args[1].toLowerCase(Locale.ROOT))) { sender.sendMessage(msg(sender, "preserve-usage")); return true; }
                config.setPreserveOnFirstDeath(Boolean.parseBoolean(args[1]));
                sender.sendMessage(msg(sender, "preserve-set").replace("{value}", args[1]));
                return true;
            }
            case "radius" -> {
                if (!admin(sender)) return noPermission(sender);
                if (args.length != 3) { sender.sendMessage(msg(sender, "radius-usage")); return true; }
                try {
                    int min = Integer.parseInt(args[1]), max = Integer.parseInt(args[2]);
                    if (min < 0 || max < min) throw new NumberFormatException();
                    config.setRadius(min, max);
                    sender.sendMessage(msg(sender, "radius-set").replace("{min}", String.valueOf(min)).replace("{max}", String.valueOf(max)));
                } catch (NumberFormatException exception) { sender.sendMessage(msg(sender, "radius-usage")); }
                return true;
            }
            case "reload" -> {
                if (!admin(sender)) return noPermission(sender);
                if (args.length != 1) return sendHelp(sender);
                plugin.reloadPluginConfig();
                sender.sendMessage(msg(sender, "reloaded"));
                return true;
            }
            case "libro" -> {
                if (!admin(sender)) return noPermission(sender);
                if (args.length != 2) { sender.sendMessage("§e/hc libro <jugador>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(msg(sender, "player-not-found")); return true; }
                new PlayerJoinListener(lives, compasses, config).giveGuideBook(target);
                sender.sendMessage(msg(sender, "book-given").replace("{player}", target.getName()));
                return true;
            }
            case "crafteo" -> {
                if (!admin(sender)) return noPermission(sender);
                if (args.length == 1 || args.length == 2 && args[1].equalsIgnoreCase("toggle")) {
                    boolean enabled = !config.compassCraftingEnabled();
                    config.setCompassCraftingEnabled(enabled);
                    sender.sendMessage(msg(sender, "craft-toggle").replace("{status}", enabled ? "activado" : "desactivado"));
                    return true;
                }
                if (args.length == 2) {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) { sender.sendMessage(msg(sender, "player-not-found")); return true; }
                    boolean allowed = target.hasPermission("hardcorecompass.craft");
                    sender.sendMessage(msg(sender, "craft-player-status").replace("{player}", target.getName()).replace("{status}", allowed ? "permitido" : "denegado"));
                    return true;
                }
                sender.sendMessage(msg(sender, "craft-usage"));
                return true;
            }
            default -> { return sendHelp(sender); }
        }
    }

    private String canonical(String subcommand) {
        return switch (subcommand.toLowerCase(Locale.ROOT)) {
            case "ayuda" -> "help";
            case "idioma" -> "lang";
            case "informacion" -> "info";
            case "givebook" -> "libro";
            case "togglecraft" -> "crafteo";
            default -> subcommand.toLowerCase(Locale.ROOT);
        };
    }

    private boolean isKind(String kind) { return Set.of("compass", "lodestone", "all").contains(kind.toLowerCase(Locale.ROOT)); }
    private void giveItems(Player player, String kind) {
        if (kind.equals("compass") || kind.equals("all")) give(player, compasses.createCompass(null, player));
        if (kind.equals("lodestone") || kind.equals("all")) give(player, compasses.createLodestone(player));
    }
    private boolean noPermission(CommandSender sender) { sender.sendMessage(msg(sender, "no-permission")); return true; }
    private boolean sendHelp(CommandSender sender) { sender.sendMessage(msg(sender, admin(sender) ? "help-admin" : "help-player")); return true; }
    private void give(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean isAdmin = admin(sender);
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("help", "ayuda", "lang", "idioma", "info", "informacion"));
            if (isAdmin) options.addAll(List.of("give", "givelodestone", "reset", "wipelinks", "setpreserve", "radius", "reload", "libro", "givebook", "crafteo", "togglecraft"));
            return filter(options, args[0]);
        }
        String sub = canonical(args[0]);
        if (args.length == 2 && Set.of("libro", "info", "wipelinks", "givelodestone").contains(sub) && isAdmin) return online(args[1]);
        if (args.length == 2 && sub.equals("crafteo") && isAdmin) {
            List<String> options = new ArrayList<>(List.of("toggle"));
            options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return filter(options, args[1]);
        }
        if (args.length == 2 && sub.equals("give") && isAdmin) {
            List<String> options = new ArrayList<>(List.of("compass", "lodestone", "all"));
            options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return filter(options, args[1]);
        }
        if (args.length == 3 && sub.equals("give") && isAdmin) return online(args[2]);
        if (args.length == 2 && sub.equals("lang") && sender instanceof Player) return filter(List.of("en", "es"), args[1]);
        if (args.length == 3 && sub.equals("reset") && isAdmin) return filter(List.of("lives", "deaths", "compass", "rtp", "all"), args[2]);
        if (args.length == 2 && sub.equals("setpreserve") && isAdmin) return filter(List.of("true", "false"), args[1]);
        if (args.length == 2 && sub.equals("radius") && isAdmin) return filter(List.of("0", "1000", "10000"), args[1]);
        return List.of();
    }

    private List<String> online(String prefix) { return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), prefix); }
    private List<String> filter(List<String> options, String prefix) { return options.stream().filter(option -> option.regionMatches(true, 0, prefix, 0, prefix.length())).toList(); }
}
