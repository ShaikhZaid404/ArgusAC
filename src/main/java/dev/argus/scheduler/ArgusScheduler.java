package dev.argus.scheduler;

import dev.argus.ArgusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Unified scheduler abstraction bridging Paper (BukkitScheduler) and Folia (RegionScheduler/AsyncScheduler).
 * Ensures thread-safe execution across both traditional tick loops and regionised threading.
 */
public final class ArgusScheduler {

    private final ArgusPlugin plugin;
    private final boolean folia;
    private final Executor virtualThreadExecutor;

    public ArgusScheduler(ArgusPlugin plugin) {
        this.plugin = plugin;
        this.folia = plugin.isFolia();
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Executes a task asynchronously using Java 21 Virtual Threads.
     */
    public void runAsync(Runnable runnable) {
        virtualThreadExecutor.execute(runnable);
    }

    /**
     * Executes a task on the global server thread (or global region on Folia).
     */
    public void runGlobal(Runnable runnable) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Executes a task synchronized with the region of a specific entity.
     */
    public void runAtEntity(Entity entity, Consumer<Entity> consumer) {
        if (folia) {
            entity.getScheduler().run(plugin, task -> consumer.accept(entity), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> consumer.accept(entity));
        }
    }

    /**
     * Executes a task synchronized with a specific location region.
     */
    public void runAtLocation(Location location, Runnable runnable) {
        if (folia) {
            Bukkit.getRegionScheduler().execute(plugin, location, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Schedules a repeating task globally.
     */
    public void runGlobalRepeating(Runnable runnable, long initialDelayTicks, long periodTicks) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), initialDelayTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, initialDelayTicks, periodTicks);
        }
    }

    public Executor getVirtualThreadExecutor() {
        return virtualThreadExecutor;
    }
}
