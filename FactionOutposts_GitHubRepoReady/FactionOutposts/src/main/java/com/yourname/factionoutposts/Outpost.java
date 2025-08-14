
package com.yourname.factionoutposts;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Outpost {
    private final String name;
    private final String worldName;
    private final Location corner1;
    private final Location corner2;
    private final int captureTimeSeconds;
    private final double moneyPerHour;
    private String ownerFaction;

    public Outpost(String name, String worldName, Location c1, Location c2, int captureTimeSeconds, double moneyPerHour) {
        this.name = name;
        this.worldName = worldName;
        this.corner1 = c1;
        this.corner2 = c2;
        this.captureTimeSeconds = captureTimeSeconds;
        this.moneyPerHour = moneyPerHour;
    }
    public String getName(){ return name; }
    public String getWorldName(){ return worldName; }
    public Location getCorner1(){ return corner1; }
    public Location getCorner2(){ return corner2; }
    public int getCaptureTimeSeconds(){ return captureTimeSeconds; }
    public double getMoneyPerHour(){ return moneyPerHour; }
    public String getOwnerFaction(){ return ownerFaction; }
    public void setOwnerFaction(String f){ ownerFaction = f; }

    public boolean isInside(org.bukkit.Location loc){
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ);
    }

    public List<Player> getPlayersInside(){
        List<Player> list = new ArrayList<>();
        World w = corner1.getWorld();
        if (w==null) return list;
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        for (Player p : w.getPlayers()) {
            double x = p.getLocation().getX(), y = p.getLocation().getY(), z = p.getLocation().getZ();
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) list.add(p);
        }
        return list;
    }
}
