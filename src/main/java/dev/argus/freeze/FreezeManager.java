package dev.argus.freeze;

import dev.argus.ArgusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Screen-share investigation and player freeze management.
 */
public final class FreezeManager {

    private final ArgusPlugin plugin;
    private final Map<UUID, FrozenSession> frozenPlayers = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public FreezeManager(ArgusPlugin plugin) {
        this.plugin = plugin;
    }

    public String freezePlayer(Player target, String staffName) {
        UUID uuid = target.getUniqueId();
        String code = "ARG-" + (1000 + random.nextInt(9000));
        FrozenSession session = new FrozenSession(uuid, target.getName(), target.getLocation(), code, System.currentTimeMillis());
        frozenPlayers.put(uuid, session);

        // Display screen title
        target.sendTitle(
                ChatColor.RED + "" + ChatColor.BOLD + "FROZEN BY STAFF",
                ChatColor.YELLOW + "Do NOT disconnect! Join Discord & run /verify " + code,
                10, 72000, 20
        );

        target.sendMessage(ChatColor.RED + "==================================================");
        target.sendMessage(ChatColor.YELLOW + "You have been frozen by " + ChatColor.WHITE + staffName + ChatColor.YELLOW + " for investigation.");
        target.sendMessage(ChatColor.GRAY + "1. Join our Discord server.");
        target.sendMessage(ChatColor.GRAY + "2. Run: " + ChatColor.AQUA + "/verify " + code);
        target.sendMessage(ChatColor.RED + "Logging out will result in an AUTOMATIC PERMANENT BAN.");
        target.sendMessage(ChatColor.RED + "==================================================");

        return code;
    }

    public boolean unfreezePlayer(Player target) {
        FrozenSession removed = frozenPlayers.remove(target.getUniqueId());
        if (removed != null) {
            target.sendTitle("", "", 0, 1, 0);
            target.sendMessage(ChatColor.GREEN + "[ArgusAC] You have been unfrozen by staff. Thank you for your cooperation.");
            return true;
        }
        return false;
    }

    public void handleQuit(Player player) {
        FrozenSession session = frozenPlayers.remove(player.getUniqueId());
        if (session != null) {
            boolean autoBan = plugin.getConfigManager().getConfig().getBoolean("freeze.auto-ban-on-disconnect", true);
            if (autoBan) {
                String reason = plugin.getConfigManager().getConfig().getString("freeze.auto-ban-reason",
                        "[Argus] Disconnecting while frozen / Screen-share evasion");
                plugin.getScheduler().runGlobal(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + player.getName() + " " + reason));
            }
        }
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.containsKey(uuid);
    }

    public Location getFrozenLocation(UUID uuid) {
        FrozenSession session = frozenPlayers.get(uuid);
        return session != null ? session.lockedLocation() : null;
    }

    public record FrozenSession(UUID uuid, String name, Location lockedLocation, String code, long timestamp) {}
}
