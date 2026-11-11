package dev.argus.alts;

import dev.argus.ArgusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Manages IP relationship tracking and enforces primary-account protection policies.
 */
public final class AltManager {

    private final ArgusPlugin plugin;

    public AltManager(ArgusPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Evaluates a player's connection against the stored IP ledger and configured alt policy.
     */
    public AltEvaluationResult evaluateConnection(UUID uuid, String username, String ipAddress) {
        long now = System.currentTimeMillis();
        List<PlayerRecord> linkedAccounts = getRecordsByIp(ipAddress);

        // Case 1: First time this IP has ever connected to the server
        if (linkedAccounts.isEmpty()) {
            insertRecord(new PlayerRecord(uuid, username, ipAddress, now, now, 50, 0));
            return new AltEvaluationResult.Allowed(true, username, 1);
        }

        // Check if current UUID is already registered on this IP
        PlayerRecord existingSelf = linkedAccounts.stream()
                .filter(record -> record.uuid().equals(uuid))
                .findFirst()
                .orElse(null);

        if (existingSelf != null) {
            updateLastSeen(uuid, username, now);
            PlayerRecord primary = getPrimaryRecord(linkedAccounts);
            boolean isSelfPrimary = primary.uuid().equals(uuid);
            return new AltEvaluationResult.Allowed(isSelfPrimary, primary.username(), linkedAccounts.size());
        }

        // New UUID attempting to connect from an IP with existing accounts
        PlayerRecord primary = getPrimaryRecord(linkedAccounts);
        boolean altsAllowed = plugin.getConfigManager().getConfig().getBoolean("alts.allowed", false);

        if (!altsAllowed) {
            String action = plugin.getConfigManager().getConfig().getString("alts.punishment-action", "KICK").toUpperCase();
            String rawMessage = plugin.getConfigManager().getConfig().getString("alts.disconnect-message",
                    "&c&lArgusAC &8» &7Alt accounts are not permitted on this server!\n&ePlease join using your primary account: &a{primary_account}");
            String formattedMessage = ChatColor.translateAlternateColorCodes('&', rawMessage.replace("{primary_account}", primary.username()));

            // In-game notification to the primary account if they are currently online
            boolean notifyPrimary = plugin.getConfigManager().getConfig().getBoolean("alts.notify-primary-in-game", true);
            if (notifyPrimary) {
                Player onlinePrimary = Bukkit.getPlayer(primary.uuid());
                if (onlinePrimary != null && onlinePrimary.isOnline()) {
                    String notifyTemplate = plugin.getConfigManager().getConfig().getString("alts.primary-notify-message",
                            "&e&lArgusAC &8» &7Notice: A secondary account (&c{alt_player}&7) attempted to connect from your IP and was rejected.");
                    onlinePrimary.sendMessage(ChatColor.translateAlternateColorCodes('&', notifyTemplate.replace("{alt_player}", username)));
                }
            }

            return new AltEvaluationResult.Rejected(action, primary.username(), formattedMessage);
        }

        // Alts allowed up to max limit
        int maxAlts = plugin.getConfigManager().getConfig().getInt("alts.max-alts-per-ip", 3);
        if (linkedAccounts.size() >= maxAlts) {
            String limitMsg = ChatColor.RED + "[ArgusAC] Maximum allowed accounts (" + maxAlts + ") exceeded for this IP.";
            return new AltEvaluationResult.Rejected("KICK", primary.username(), limitMsg);
        }

        insertRecord(new PlayerRecord(uuid, username, ipAddress, now, now, 50, 0));
        return new AltEvaluationResult.Allowed(false, primary.username(), linkedAccounts.size() + 1);
    }

    private PlayerRecord getPrimaryRecord(List<PlayerRecord> records) {
        return records.stream()
                .min(Comparator.comparingLong(PlayerRecord::firstSeen))
                .orElse(records.getFirst());
    }

    public List<PlayerRecord> getRecordsByIp(String ipAddress) {
        List<PlayerRecord> list = new ArrayList<>();
        String sql = "SELECT uuid, username, ip_address, first_seen, last_seen, trust_score, flags_count FROM argus_players WHERE ip_address = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ipAddress);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlayerRecord(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("username"),
                            rs.getString("ip_address"),
                            rs.getLong("first_seen"),
                            rs.getLong("last_seen"),
                            rs.getInt("trust_score"),
                            rs.getInt("flags_count")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[ArgusAC] Failed to fetch player records for IP: " + ipAddress + " (" + e.getMessage() + ")");
        }
        return list;
    }

    public List<PlayerRecord> getRecordsByUsername(String username) {
        String ip = null;
        String sql = "SELECT ip_address FROM argus_players WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ip = rs.getString("ip_address");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[ArgusAC] Failed to find IP for user: " + username);
        }

        return ip != null ? getRecordsByIp(ip) : List.of();
    }

    private void insertRecord(PlayerRecord record) {
        String sql = "INSERT INTO argus_players (uuid, username, ip_address, first_seen, last_seen, trust_score, flags_count) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.uuid().toString());
            ps.setString(2, record.username());
            ps.setString(3, record.ipAddress());
            ps.setLong(4, record.firstSeen());
            ps.setLong(5, record.lastSeen());
            ps.setInt(6, record.trustScore());
            ps.setInt(7, record.flagsCount());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[ArgusAC] Failed to insert player record: " + record.username());
        }
    }

    private void updateLastSeen(UUID uuid, String username, long now) {
        String sql = "UPDATE argus_players SET last_seen = ?, username = ? WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, now);
            ps.setString(2, username);
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[ArgusAC] Failed to update last_seen for " + username);
        }
    }
}
