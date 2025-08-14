
package com.yourname.factionoutposts;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OutpostCommand implements CommandExecutor {
    private final FactionOutposts plugin;
    private final OutpostManager manager;

    private final java.util.Map<String, Location> pos1 = new java.util.HashMap<>();
    private final java.util.Map<String, Location> pos2 = new java.util.HashMap<>();

    public OutpostCommand(FactionOutposts plugin, OutpostManager manager){
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("outpost.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§6/outpost setpos1 | setpos2 | create <name> [captureSec] [perHour] | createwe <name> [captureSec] [perHour] | delete <name> | list | info <name> | forceclaim <name> <factionId>");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("setpos1") && sender instanceof Player) {
            Player p = (Player) sender;
            pos1.put(p.getName(), p.getLocation());
            p.sendMessage("§aSet pos1.");
            return true;
        }
        if (sub.equals("setpos2") && sender instanceof Player) {
            Player p = (Player) sender;
            pos2.put(p.getName(), p.getLocation());
            p.sendMessage("§aSet pos2.");
            return true;
        }
        if (sub.equals("createwe") && args.length >= 2 && sender instanceof Player) {
            Player p = (Player) sender;
            String name = args[1].toLowerCase();
            int capture = 300;
            double money = 0.0;
            if (args.length >= 3) { try { capture = Integer.parseInt(args[2]); } catch (Exception ignored){} }
            if (args.length >= 4) { try { money = Double.parseDouble(args[3]); } catch (Exception ignored){} }
            try {
                com.sk89q.worldedit.bukkit.BukkitPlayer wep = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(p);
                com.sk89q.worldedit.session.LocalSession sess = com.sk89q.worldedit.WorldEdit.getInstance().getSessionManager().get(wep);
                com.sk89q.worldedit.regions.Region sel = sess.getSelection(wep.getWorld());
                if (sel == null) { p.sendMessage("§cNo WorldEdit selection."); return true; }
                org.bukkit.World bw = p.getWorld();
                com.sk89q.worldedit.math.BlockVector3 min = sel.getMinimumPoint();
                com.sk89q.worldedit.math.BlockVector3 max = sel.getMaximumPoint();
                org.bukkit.Location c1 = new org.bukkit.Location(bw, min.getX(), min.getY(), min.getZ());
                org.bukkit.Location c2 = new org.bukkit.Location(bw, max.getX(), max.getY(), max.getZ());
                manager.createOutpost(name, c1, c2, capture, money);
                p.sendMessage("§aOutpost created from WorldEdit selection: " + name);
            } catch (Throwable t) {
                p.sendMessage("§cWorldEdit not found or API error: " + t.getMessage());
            }
            return true;
        }
        if (sub.equals("create") && args.length >= 2 && sender instanceof Player) {
            Player p = (Player) sender;
            String name = args[1].toLowerCase();
            Location c1 = pos1.get(p.getName()), c2 = pos2.get(p.getName());
            if (c1 == null || c2 == null) {
                p.sendMessage("§cSet pos1 and pos2 first with /outpost setpos1 and /outpost setpos2");
                return true;
            }
            int capture = 300;
            double money = 0.0;
            if (args.length >= 3) { try { capture = Integer.parseInt(args[2]); } catch (Exception ignored){} }
            if (args.length >= 4) { try { money = Double.parseDouble(args[3]); } catch (Exception ignored){} }
            manager.createOutpost(name, c1, c2, capture, money);
            p.sendMessage("§aOutpost created: " + name);
            return true;
        }
        if (sub.equals("list")) {
            sender.sendMessage("§6Outposts:");
            for (Outpost o : manager.getOutposts()) sender.sendMessage(" - " + o.getName() + " owner=" + o.getOwnerFaction());
            return true;
        }
        if (sub.equals("delete") && args.length >= 2) {
            manager.deleteOutpost(args[1].toLowerCase());
            sender.sendMessage("§aDeleted.");
            return true;
        }
        if (sub.equals("forceclaim") && args.length >= 3) {
            manager.forceClaim(args[1].toLowerCase(), args[2]);
            sender.sendMessage("§aForced claim.");
            return true;
        }
        if (sub.equals("info") && args.length >= 2) {
            Outpost o = manager.getOutpost(args[1].toLowerCase());
            if (o==null) { sender.sendMessage("§cNo outpost."); return true; }
            sender.sendMessage("§6Outpost " + o.getName() + " owner=" + o.getOwnerFaction() + " capture=" + o.getCaptureTimeSeconds() + "s perHour=$" + o.getMoneyPerHour());
            return true;
        }
        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }
}
