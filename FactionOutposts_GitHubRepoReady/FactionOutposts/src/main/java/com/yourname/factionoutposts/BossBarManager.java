
package com.yourname.factionoutposts;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BossBarManager {
    private final FactionOutposts plugin;
    private final OutpostManager manager;
    private final Map<String, BossBar> bars = new HashMap<>();
    private final Map<String, Set<Player>> viewers = new HashMap<>();

    public BossBarManager(FactionOutposts plugin, OutpostManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private BossBar getBar(String outpostName){
        return bars.computeIfAbsent(outpostName, name -> {
            BarColor color = BarColor.valueOf(plugin.getConfig().getString("bossbar.color","GREEN").toUpperCase());
            BarStyle style = BarStyle.valueOf(plugin.getConfig().getString("bossbar.style","SOLID").toUpperCase());
            BossBar bar = Bukkit.createBossBar("Capturing " + name, color, style);
            bar.setVisible(true);
            return bar;
        });
    }

    public void update(){
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) return;
        for (Outpost o : manager.getOutposts()) {
            BossBar bar = getBar(o.getName());
            double progress = manager.getProgressRatio(o.getName());
            if (progress < 0) progress = 0;
            if (progress > 1) progress = 1;
            String title = "Outpost " + o.getName();
            String contest = manager.getLastContestingFaction(o.getName());
            if (contest != null) title += " §7[§e" + contest + "§7]";
            bar.setTitle(title.replace("&","§"));
            bar.setProgress(progress);

            Set<Player> keep = new HashSet<>();
            for (Player p : o.getPlayersInside()) { bar.addPlayer(p); keep.add(p); }
            Set<Player> current = viewers.computeIfAbsent(o.getName(), k -> new HashSet<>());
            for (Player prev : new HashSet<>(current)) if (!keep.contains(prev)) bar.removePlayer(prev);
            viewers.put(o.getName(), keep);
        }
    }

    public void clearAll(){
        for (BossBar bar : bars.values()) bar.removeAll();
        bars.clear();
        viewers.clear();
    }
}
