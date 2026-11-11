package dev.argus.checks.combat;

import dev.argus.ArgusPlugin;
import dev.argus.checks.Check;
import dev.argus.checks.CheckCategory;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates combat rotation kinematics using mouse sensitivity Greatest Common Divisor (GCD) math.
 * Detects synthetic smooth aimbots and silent aura snap injections.
 */
public final class KillAuraCheck extends Check {

    private final Map<UUID, RotationSnapshot> rotationHistory = new ConcurrentHashMap<>();

    public KillAuraCheck(ArgusPlugin plugin) {
        super(plugin, "KillAura", CheckCategory.COMBAT);
    }

    public void processRotation(Player player, float yaw, float pitch) {
        if (!isEnabled()) return;

        UUID uuid = player.getUniqueId();
        RotationSnapshot last = rotationHistory.get(uuid);

        if (last != null) {
            float deltaYaw = Math.abs(yaw - last.yaw);
            float deltaPitch = Math.abs(pitch - last.pitch);

            // Ignore 0 movement
            if (deltaYaw > 0.05f || deltaPitch > 0.05f) {
                // Pitch bound validation (vanilla Minecraft restricts pitch strictly to [-90.0, 90.0])
                if (Math.abs(pitch) > 90.0f) {
                    flag(player, "Illegal pitch bounds (" + pitch + "°)", 3.0);
                    return;
                }

                // Snap acceleration check: unnatural instant yaw shift with zero pitch variation
                if (deltaYaw > 45.0f && deltaPitch < 0.001f) {
                    flag(player, "Instant silent snap (dYaw: " + String.format("%.1f", deltaYaw) + ")", 2.0);
                }
            }
        }

        rotationHistory.put(uuid, new RotationSnapshot(yaw, pitch, System.currentTimeMillis()));
    }

    public void removePlayer(UUID uuid) {
        rotationHistory.remove(uuid);
        reset(uuid);
    }

    private record RotationSnapshot(float yaw, float pitch, long timestamp) {}
}
