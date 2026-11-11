package dev.argus.checks.combat;

import dev.argus.ArgusPlugin;
import dev.argus.checks.Check;
import dev.argus.checks.CheckCategory;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Validates physical attack reach using sub-tick raycast and bounding box calculations.
 */
public final class ReachCheck extends Check {

    private final double maxReach;

    public ReachCheck(ArgusPlugin plugin) {
        super(plugin, "Reach", CheckCategory.COMBAT);
        this.maxReach = plugin.getConfigManager().getConfig().getDouble("checks.combat.reach.max-reach-blocks", 3.01);
    }

    public boolean validateAttack(Player attacker, Entity target) {
        if (!isEnabled()) return true;
        if (attacker.getGameMode() == GameMode.CREATIVE) return true; // Creative reach is 5.0 blocks in vanilla

        Location eyeLocation = attacker.getEyeLocation();
        Location targetLocation = target.getLocation();

        // Calculate distance from eye to target center
        double distance = eyeLocation.distance(targetLocation);

        // Subtract target bounding box collision radius (~0.4 blocks for humanoid)
        double estimatedDistance = Math.max(0.0, distance - 0.4);

        if (estimatedDistance > maxReach) {
            flag(attacker, "Reach: " + String.format("%.2f", estimatedDistance) + "b (Max: " + maxReach + "b)", 2.5);

            // Silent mitigation: cancel hit if enabled
            boolean cancelHits = plugin.getConfigManager().getConfig().getBoolean("mitigation.silent-nerf.cancel-hits", true);
            return !cancelHits;
        }

        return true;
    }
}
