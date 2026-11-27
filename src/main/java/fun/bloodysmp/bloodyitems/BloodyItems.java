package fun.bloodysmp.bloodyitems;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;

public final class BloodyItems extends JavaPlugin implements Listener, TabCompleter {
    private final MiniMessage mini = MiniMessage.miniMessage();

    @Override public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("bi") != null) {
            getCommand("bi").setExecutor(this);
            getCommand("bi").setTabCompleter(this);
        }
        getLogger().info("BLOODYITEMS 2.0.0 enabled.");
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (!getConfig().getBoolean("resource-pack.enabled", true)) return;
        Player player = event.getPlayer();
        String url = getConfig().getString("resource-pack.url", "");
        if (url == null || url.isBlank() || url.contains("example.com")) {
            if (getConfig().getBoolean("resource-pack.required", true)) {
                getLogger().warning("Required resource pack URL is not configured; cannot prompt players.");
            }
            return;
        }
        try {
            URI.create(url).toURL();
            String sha1 = getConfig().getString("resource-pack.sha1", "");
            if (sha1 != null && !sha1.isBlank()) {
                byte[] hash = java.util.HexFormat.of().parseHex(sha1);
                if (hash.length != 20) throw new IllegalArgumentException("SHA-1 must be 40 hexadecimal characters");
            }
            player.setResourcePack(url, (sha1 == null || sha1.isBlank()) ? null : sha1,
                    getConfig().getBoolean("resource-pack.required", true));
        } catch (Exception ex) {
            getLogger().warning("Could not send resource pack: " + ex.getMessage());
            if (getConfig().getBoolean("resource-pack.required", true)) player.kick(mini.deserialize(getConfig().getString("resource-pack.failed-kick", "<red>Resource pack error")));
        }
    }

    @EventHandler public void onPackStatus(PlayerResourcePackStatusEvent event) {
        if (!getConfig().getBoolean("resource-pack.required", true)) return;
        switch (event.getStatus()) {
            case DECLINED -> event.getPlayer().kick(mini.deserialize(getConfig().getString("resource-pack.declined-kick", "<red>Resource pack required")));
            case FAILED_DOWNLOAD, INVALID_URL -> event.getPlayer().kick(mini.deserialize(getConfig().getString("resource-pack.failed-kick", "<red>Resource pack download failed")));
            default -> { }
        }
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return java.util.stream.Stream.of("help", "status", "reload")
                    .filter(value -> value.startsWith(prefix))
                    .filter(value -> !value.equals("reload") || sender.hasPermission("bloodyitems.admin"))
                    .toList();
        }
        return List.of();
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(mini.deserialize("<dark_red><bold>BLOODYITEMS 2.0</bold></dark_red> <gray>Commands: /bi help, /bi status, /bi reload"));
            return true;
        }
        if (args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("BLOODYITEMS: " + (getConfig().getBoolean("resource-pack.enabled") ? "resource pack enabled" : "resource pack disabled"));
            sender.sendMessage("Required: " + getConfig().getBoolean("resource-pack.required"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bloodyitems.admin")) { sender.sendMessage("No permission."); return true; }
            reloadConfig(); sender.sendMessage("BLOODYITEMS config reloaded."); return true;
        }
        sender.sendMessage("Unknown subcommand. Use /bi help.");
        return true;
    }
}
