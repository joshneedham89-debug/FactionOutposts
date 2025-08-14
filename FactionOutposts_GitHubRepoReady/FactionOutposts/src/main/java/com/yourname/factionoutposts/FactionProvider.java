
package com.yourname.factionoutposts;

import org.bukkit.entity.Player;

public interface FactionProvider {
    String getFactionId(Player player);
    default void depositToFactionBank(String factionId, double amount) {}
}
