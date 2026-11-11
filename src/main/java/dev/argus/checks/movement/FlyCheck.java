package dev.argus.checks.movement;

import dev.argus.ArgusPlugin;
import dev.argus.checks.Check;
import dev.argus.checks.CheckCategory;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates vertical kinematics against vanilla Minecraft gravity and ground contact state.
 */
public final class FlyCheck extends Check {

    private final Map<UUID, Double> airTicks = new ConcurrentHashMap<>();

    public FlyCheck(ArgusPlugin plugin) {
        super(plugin, "Fly", CheckCategory.MOVEMENT);
    }

    public void processMovement(Player player, Location from, Location to) {
        if (!isEnabled()) return;
        if (player.getAllowFlight() || player.getGameMode() == GameMode.CREATIVE || player.isGliding()) return;

        UUID uuid = player.getUniqueId();
        double deltaY = to.getY() - from.getY();

        // Check if player is airborne
        if (!player.isOnGround()) {
            double currentAir = airTicks.compute(uuid, (k, v) -> (v == null ? 0.0 : v) + 1.0);

            // Floating detection: zero vertical movement while mid-air for multiple consecutive ticks
            if (Math.abs(deltaY) < 0.0001 && currentAir > 8.0) {
                flag(player, "Hovering in mid-air (AirTicks: " + (int) currentAir + ")", 1.5);
                setbackPlayer(player, from);
            }

            // Ascending without upward velocity source (jump / levitation)
            if (deltaY > 0.0 && currentAir > 15.0) {
                flag(player, "Ascending without ground contact (AirTicks: " + (int) currentAir + ")", 2.0);
                setbackPlayer(player, from);
            }
        } else {
            airTicks.put(uuid, 0.0);
        }
    }

    private void setbackPlayer(Player player, Location safeLocation) {
        plugin.getScheduler().runAtEntity(player, p -> player.teleport(safeLocation));
    }

    public void removePlayer(UUID uuid) {
        airTicks.remove(uuid);
        reset(uuid);
    }
}
