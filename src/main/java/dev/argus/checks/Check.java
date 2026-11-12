package dev.argus.checks;

import dev.argus.ArgusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract foundation for all ArgusAC detection modules.
 * Incorporates violation level buffering, exponential decay, and TPS-aware thresholding.
 */
public abstract class Check {

    protected final ArgusPlugin plugin;
    private final String name;
    private final CheckCategory category;

    private boolean enabled;
    private double vlThreshold;
    private String punishmentAction;

    // Per-player violation level buffers
    private final Map<UUID, Double> violationBuffers = new ConcurrentHashMap<>();

    public Check(ArgusPlugin plugin, String name, CheckCategory category) {
        this.plugin = plugin;
        this.name = name;
        this.category = category;
        reloadConfig();
    }

    public void reloadConfig() {
        String path = "checks." + category.name().toLowerCase() + "." + name.toLowerCase().replace(" ", "");
        this.enabled = plugin.getConfigManager().getConfig().getBoolean(path + ".enabled", true);
        this.vlThreshold = plugin.getConfigManager().getConfig().getDouble(path + ".vl-threshold", 15.0);
        this.punishmentAction = plugin.getConfigManager().getConfig().getString(path + ".punishment", "warn");
    }

    /**
     * Flags a player, increments their VL buffer, and broadcasts staff alerts if threshold is met.
     */
    public void flag(Player player, String detail, double vlIncrement) {
        if (!enabled) return;

        // Skip players with bypass permissions
        if (player.hasPermission("argus.bypass")) return;

        UUID uuid = player.getUniqueId();
        double currentVl = violationBuffers.compute(uuid, (k, v) -> (v == null ? 0.0 : v) + vlIncrement);

        // Broadcast clickable alert to online staff members
        String alertMsg = ChatColor.translateAlternateColorCodes('&',
                "&8[&bArgus&8] &c" + player.getName() + " &7flagged &e" + name + " &8(&f" + detail + "&8) &7[VL: &c" + String.format("%.1f", currentVl) + "&7]");

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("argus.alerts")) {
                staff.sendMessage(alertMsg);
            }
        }

        // Broadcast rich interactive alert to Discord staff channel
        if (plugin.getDiscordManager() != null) {
            plugin.getDiscordManager().sendAlert(player.getName(), name, detail, currentVl, category.name());
        }

        // Check if punishment threshold reached
        if (currentVl >= vlThreshold) {
            executePunishment(player, currentVl);
        }
    }

    protected void executePunishment(Player player, double vl) {
        plugin.getLogger().warning("[ArgusAC] Punishment triggered for " + player.getName() + " on " + name + " (VL: " + vl + ")");
        if ("ban".equalsIgnoreCase(punishmentAction)) {
            plugin.getScheduler().runGlobal(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + player.getName() + " Cheating | ArgusAC [" + name + "]"));
        } else if ("kick".equalsIgnoreCase(punishmentAction)) {
            plugin.getScheduler().runAtEntity(player, p -> player.kickPlayer(ChatColor.RED + "[ArgusAC] Disconnected for irregular gameplay: " + name));
        }
    }

    /**
     * Decays violation levels over time.
     */
    public void decay() {
        double decayFactor = plugin.getConfigManager().getConfig().getDouble("violations.decay-factor", 0.95);
        violationBuffers.replaceAll((k, v) -> v * decayFactor);
        violationBuffers.entrySet().removeIf(entry -> entry.getValue() < 0.1);
    }

    public void reset(UUID uuid) {
        violationBuffers.remove(uuid);
    }

    public double getVl(UUID uuid) {
        return violationBuffers.getOrDefault(uuid, 0.0);
    }

    public String getName() {
        return name;
    }

    public CheckCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
