package dev.argus.checks;

import dev.argus.ArgusPlugin;
import dev.argus.checks.combat.AutoClickerCheck;
import dev.argus.checks.combat.KillAuraCheck;
import dev.argus.checks.combat.ReachCheck;
import dev.argus.checks.movement.FlyCheck;
import dev.argus.checks.movement.ScaffoldCheck;
import dev.argus.checks.movement.SpeedCheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry and lifecycle manager for all detection modules.
 */
public final class CheckManager {

    private final ArgusPlugin plugin;
    private final List<Check> checks = new ArrayList<>();

    private final KillAuraCheck killAuraCheck;
    private final ReachCheck reachCheck;
    private final AutoClickerCheck autoClickerCheck;
    private final FlyCheck flyCheck;
    private final SpeedCheck speedCheck;
    private final ScaffoldCheck scaffoldCheck;

    public CheckManager(ArgusPlugin plugin) {
        this.plugin = plugin;

        this.killAuraCheck = register(new KillAuraCheck(plugin));
        this.reachCheck = register(new ReachCheck(plugin));
        this.autoClickerCheck = register(new AutoClickerCheck(plugin));
        this.flyCheck = register(new FlyCheck(plugin));
        this.speedCheck = register(new SpeedCheck(plugin));
        this.scaffoldCheck = register(new ScaffoldCheck(plugin));

        // Schedule periodic violation decay task (every 10s = 200 ticks)
        int decaySeconds = plugin.getConfigManager().getConfig().getInt("violations.decay-interval-seconds", 10);
        plugin.getScheduler().runGlobalRepeating(this::decayAll, decaySeconds * 20L, decaySeconds * 20L);
    }

    private <T extends Check> T register(T check) {
        checks.add(check);
        return check;
    }

    public void decayAll() {
        for (Check check : checks) {
            check.decay();
        }
    }

    public void reloadChecks() {
        for (Check check : checks) {
            check.reloadConfig();
        }
    }

    public List<Check> getChecks() {
        return Collections.unmodifiableList(checks);
    }

    public KillAuraCheck getKillAuraCheck() {
        return killAuraCheck;
    }

    public ReachCheck getReachCheck() {
        return reachCheck;
    }

    public AutoClickerCheck getAutoClickerCheck() {
        return autoClickerCheck;
    }

    public FlyCheck getFlyCheck() {
        return flyCheck;
    }

    public SpeedCheck getSpeedCheck() {
        return speedCheck;
    }

    public ScaffoldCheck getScaffoldCheck() {
        return scaffoldCheck;
    }
}
