
package com.yourname.factionoutposts;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OutpostManager {
    private final FactionOutposts plugin;
    final FactionProvider factionProvider;
    private final Map<String, Outpost> outposts = new ConcurrentHashMap<>();
    private final Map<String, CaptureState> captureStates = new ConcurrentHashMap<>();
    private final Map<String, java.util.List<org.bukkit.potion.PotionEffect>> ownerBuffs = new ConcurrentHashMap<>();
    private final DataStore dataStore = new DataStore(plugin);
    private final BossBarManager bossBars = new BossBarManager(plugin, this);

    public OutpostManager(FactionOutposts plugin, FactionProvider factionProvider) {
        this.plugin = plugin;
        this.factionProvider = factionProvider;
        loadFromConfig();
    }

    public void loadFromConfig(){
        outposts.clear();
        captureStates.clear();
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains("outposts")) return;
        for (String key : cfg.getConfigurationSection("outposts").getKeys(false)) {
            String path = "outposts." + key;
            String world = cfg.getString(path + ".world");
            double x1 = cfg.getDouble(path + ".x1");
            double y1 = cfg.getDouble(path + ".y1");
            double z1 = cfg.getDouble(path + ".z1");
            double x2 = cfg.getDouble(path + ".x2");
            double y2 = cfg.getDouble(path + ".y2");
            double z2 = cfg.getDouble(path + ".z2");
            int captureTime = cfg.getInt(path + ".capture_time", 300);
            double moneyPerHour = cfg.getDouble(path + ".rewards.money_per_hour", 0.0);
            Location c1 = new Location(Bukkit.getWorld(world), x1, y1, z1);
            Location c2 = new Location(Bukkit.getWorld(world), x2, y2, z2);
            Outpost o = new Outpost(key.toLowerCase(), world, c1, c2, captureTime, moneyPerHour);
            o.setOwnerFaction(cfg.getString(path + ".owner", null));
            outposts.put(o.getName(), o);
            captureStates.put(o.getName(), new CaptureState());

            java.util.List<String> buffs = cfg.getStringList(path + ".rewards.buffs");
            java.util.List<org.bukkit.potion.PotionEffect> list = new java.util.ArrayList<>();
            for (String s : buffs) {
                try {
                    String[] parts = s.split(":");
                    org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(parts[0].toUpperCase());
                    int amp = parts.length>1 ? Integer.parseInt(parts[1]) : 0;
                    int dur = parts.length>2 ? Integer.parseInt(parts[2]) : 200;
                    if (type != null) list.add(new org.bukkit.potion.PotionEffect(type, dur, amp, true, false));
                } catch (Exception ignored) {}
            }
            ownerBuffs.put(o.getName(), list);
            if (dataStore.isActive()) {
                String dbOwner = dataStore.readOwner(o.getName());
                if (dbOwner != null) o.setOwnerFaction(dbOwner);
            }
        }
    }

    public void saveAll(){
        FileConfiguration cfg = plugin.getConfig();
        for (Outpost o : outposts.values()) {
            String path = "outposts." + o.getName();
            cfg.set(path + ".world", o.getWorldName());
            cfg.set(path + ".x1", o.getCorner1().getX());
            cfg.set(path + ".y1", o.getCorner1().getY());
            cfg.set(path + ".z1", o.getCorner1().getZ());
            cfg.set(path + ".x2", o.getCorner2().getX());
            cfg.set(path + ".y2", o.getCorner2().getY());
            cfg.set(path + ".z2", o.getCorner2().getZ());
            cfg.set(path + ".capture_time", o.getCaptureTimeSeconds());
            cfg.set(path + ".rewards.money_per_hour", o.getMoneyPerHour());
            cfg.set(path + ".owner", o.getOwnerFaction());
            if (dataStore.isActive()) dataStore.writeOwner(o.getName(), o.getOwnerFaction());
        }
        plugin.saveConfig();
    }

    public Collection<Outpost> getOutposts(){ return new ArrayList<>(outposts.values()); }
    public Outpost getOutpost(String name){ return outposts.get(name.toLowerCase()); }

    public void createOutpost(String name, Location c1, Location c2, int captureSec, double moneyPerHour) {
        Outpost o = new Outpost(name.toLowerCase(), c1.getWorld().getName(), c1, c2, captureSec, moneyPerHour);
        outposts.put(o.getName(), o);
        captureStates.put(o.getName(), new CaptureState());
        saveAll();
    }

    public void deleteOutpost(String name){
        outposts.remove(name.toLowerCase());
        captureStates.remove(name.toLowerCase());
        plugin.getConfig().set("outposts." + name.toLowerCase(), null);
        plugin.saveConfig();
    }

    public void tick(){
        for (Outpost outpost : outposts.values()) {
            CaptureState state = captureStates.computeIfAbsent(outpost.getName(), k -> new CaptureState());
            java.util.Map<String, Integer> factionCounts = new java.util.HashMap<>();
            for (Player p : outpost.getPlayersInside()) {
                String faction = factionProvider.getFactionId(p);
                if (faction == null) continue;
                factionCounts.put(faction, factionCounts.getOrDefault(faction, 0) + 1);
            }

            if (factionCounts.isEmpty()) {
                state.decay(plugin.getConfig().getInt("capture.decay-per-tick", 20));
                continue;
            }

            String topFaction = null; int top = 0; int total = 0;
            for (java.util.Map.Entry<String,Integer> e : factionCounts.entrySet()) {
                total += e.getValue();
                if (e.getValue() > top) { top = e.getValue(); topFaction = e.getKey(); }
            }
            double threshold = plugin.getConfig().getDouble("capture.required-majority", 0.6D);
            if (topFaction != null && ((double)top / (double)total) >= threshold) {
                if (topFaction.equals(outpost.getOwnerFaction())) {
                    state.reset();
                } else {
                    state.progress += plugin.getConfig().getInt("capture.progress-per-tick", 20);
                    state.lastContestingFaction = topFaction;
                    if (state.progress >= outpost.getCaptureTimeSeconds()) {
                        outpost.setOwnerFaction(topFaction);
                        state.reset();
                        String msg = plugin.getConfig().getString("messages.captured","&a{faction} captured {outpost}!")
                            .replace("{faction}", topFaction).replace("{outpost}", outpost.getName());
                        Bukkit.broadcastMessage(msg.replace("&","§"));
                        saveAll();
                    }
                }
            } else {
                state.decay(plugin.getConfig().getInt("capture.decay-per-tick", 20));
            }
        }
        bossBars.update();
        applyOwnerBuffs();
    }

    private void applyOwnerBuffs(){
        for (Outpost o : outposts.values()) {
            String owner = o.getOwnerFaction();
            if (owner == null) continue;
            java.util.List<org.bukkit.potion.PotionEffect> buffs = ownerBuffs.getOrDefault(o.getName(), java.util.Collections.emptyList());
            if (buffs.isEmpty()) continue;
            for (org.bukkit.entity.Player p : o.getPlayersInside()) {
                String pf = factionProvider.getFactionId(p);
                if (pf != null && pf.equals(owner)) {
                    for (org.bukkit.potion.PotionEffect pe : buffs) p.addPotionEffect(pe, true);
                }
            }
        }
    }

    public double getProgressRatio(String outpostName){
        CaptureState cs = captureStates.get(outpostName);
        Outpost o = outposts.get(outpostName);
        if (cs == null || o == null) return 0.0;
        double req = Math.max(1, o.getCaptureTimeSeconds());
        return Math.min(1.0, cs.progress / req);
    }

    public String getLastContestingFaction(String outpostName){
        CaptureState cs = captureStates.get(outpostName);
        return cs == null ? null : cs.lastContestingFaction;
    }

    public void payoutCycle() {
        int interval = Math.max(1, plugin.getConfig().getInt("payout.interval-minutes", 60));
        for (Outpost o : outposts.values()) {
            String owner = o.getOwnerFaction();
            if (owner == null) continue;
            double perHour = o.getMoneyPerHour();
            if (perHour <= 0.0) continue;
            double amount = perHour * (interval / 60.0D);
            if (amount <= 0) continue;
            try {
                factionProvider.depositToFactionBank(owner, amount);
                if (dataStore.isActive()) dataStore.logPayout(o.getName(), owner, amount);
                String msg = plugin.getConfig().getString("messages.payout",
                        "&aOutpost &6{outpost}&a paid &2${amount}&a to faction bank of &6{faction}&a.");
                msg = msg.replace("{amount}", String.format("%.2f", amount))
                         .replace("{faction}", owner)
                         .replace("{outpost}", o.getName());
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg.replace("&","§")));
            } catch (Throwable t) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Failed to pay faction bank for outpost "+o.getName()+" owner "+owner, t);
            }
        }
    }

    public void forceClaim(String outpostName, String factionId){
        Outpost o = getOutpost(outpostName);
        if (o==null) return;
        o.setOwnerFaction(factionId);
        saveAll();
    }

    public void shutdown(){
        try { bossBars.clearAll(); } catch (Throwable ignored) {}
        try { dataStore.close(); } catch (Throwable ignored) {}
    }
}
