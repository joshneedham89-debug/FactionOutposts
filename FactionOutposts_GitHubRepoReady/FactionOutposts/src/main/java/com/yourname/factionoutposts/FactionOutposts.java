
package com.yourname.factionoutposts;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public class FactionOutposts extends JavaPlugin {
    OutpostManager outpostManager;
    FactionProvider factionProvider;
    private BukkitTask captureTask;
    private BukkitTask payoutTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("SaberFactions") != null) {
            factionProvider = new SaberFactionsProvider(this);
            getLogger().info("[FactionOutposts] SaberFactions detected (reflection).");
        } else {
            factionProvider = new FallbackProvider(this);
            getLogger().warning("[FactionOutposts] SaberFactions not found; using fallback provider.");
        }

        outpostManager = new OutpostManager(this, factionProvider);
        if (getCommand("outpost") != null) {
            getCommand("outpost").setExecutor(new OutpostCommand(this, outpostManager));
        }
        getServer().getPluginManager().registerEvents(new CaptureListener(this, outpostManager), this);

        int intervalTicks = getConfig().getInt("capture.tick-interval-ticks", 40);
        captureTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            try { outpostManager.tick(); } catch (Exception e) { getLogger().log(Level.SEVERE, "Error in outpost tick", e); }
        }, intervalTicks, intervalTicks);

        int payoutMinutes = getConfig().getInt("payout.interval-minutes", 60);
        long payoutTicks = Math.max(1, payoutMinutes) * 60L * 20L;
        payoutTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            try { outpostManager.payoutCycle(); } catch (Exception e) { getLogger().log(Level.SEVERE, "Error in outpost payout", e); }
        }, payoutTicks, payoutTicks);

        getLogger().info("[FactionOutposts] Enabled.");
    }

    @Override
    public void onDisable() {
        if (captureTask != null) captureTask.cancel();
        if (payoutTask != null) payoutTask.cancel();
        if (outpostManager != null) { outpostManager.saveAll(); outpostManager.shutdown(); }
        getLogger().info("[FactionOutposts] Disabled.");
    }
}
