
package com.yourname.factionoutposts;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FallbackProvider implements FactionProvider {
    private final Map<String, String> manual = new ConcurrentHashMap<>();
    private final FactionOutposts plugin;

    public FallbackProvider(FactionOutposts plugin){
        this.plugin = plugin;
    }

    public void setPlayerFaction(String playerName, String factionId){
        manual.put(playerName.toLowerCase(), factionId);
    }

    @Override
    public String getFactionId(Player player) {
        return manual.get(player.getName().toLowerCase());
    }
}
