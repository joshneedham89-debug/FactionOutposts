
package com.yourname.factionoutposts;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class SaberFactionsProvider implements FactionProvider {
    private final FactionOutposts plugin;
    private Class<?> clsFPlayers, clsFPlayer, clsFactions, clsFaction, clsEcon;
    private Method mFPlayers_getInstance, mFPlayers_getByPlayer, mFPlayer_getFaction, mFaction_isWilderness, mFaction_getId;
    private Method mEcon_isEnabled, mEcon_modifyFactionBalance;
    private Object factionsInstance;

    public SaberFactionsProvider(FactionOutposts plugin) {
        this.plugin = plugin;
        try {
            clsFPlayers = Class.forName("com.massivecraft.factions.FPlayers");
            clsFPlayer  = Class.forName("com.massivecraft.factions.FPlayer");
            clsFactions = Class.forName("com.massivecraft.factions.Factions");
            clsFaction  = Class.forName("com.massivecraft.factions.Faction");
            clsEcon     = Class.forName("com.massivecraft.factions.integration.Econ");

            mFPlayers_getInstance = clsFPlayers.getMethod("getInstance");
            mFPlayers_getByPlayer = clsFPlayers.getMethod("getByPlayer", org.bukkit.entity.Player.class);
            mFPlayer_getFaction   = clsFPlayer.getMethod("getFaction");
            mFaction_isWilderness = clsFaction.getMethod("isWilderness");
            try { mFaction_getId = clsFaction.getMethod("getId"); }
            catch (NoSuchMethodException e) { mFaction_getId = clsFaction.getMethod("getTag"); }

            mEcon_isEnabled = clsEcon.getMethod("isEnabled");
            mEcon_modifyFactionBalance = clsEcon.getMethod("modifyFactionBalance", clsFaction, double.class);

            Method mFactions_getInstance = clsFactions.getMethod("getInstance");
            factionsInstance = mFactions_getInstance.invoke(null);
        } catch (Throwable t) {
            plugin.getLogger().warning("[FactionOutposts] Reflection init for SaberFactions failed: " + t.getMessage());
        }
    }

    @Override
    public String getFactionId(Player player) {
        try {
            Object fplayers = mFPlayers_getInstance.invoke(null);
            Object fplayer  = mFPlayers_getByPlayer.invoke(fplayers, player);
            if (fplayer == null) return null;
            Object faction  = mFPlayer_getFaction.invoke(fplayer);
            if (faction == null) return null;
            boolean wilderness = (boolean) mFaction_isWilderness.invoke(faction);
            if (wilderness) return null;
            Object id = mFaction_getId.invoke(faction);
            return id == null ? null : id.toString();
        } catch (Throwable t) {
            plugin.getLogger().warning("[FactionOutposts] getFactionId reflection failed: " + t.getMessage());
            return null;
        }
    }

    @Override
    public void depositToFactionBank(String factionId, double amount) {
        if (amount <= 0) return;
        try {
            Method mGetFactionById = clsFactions.getMethod("getFactionById", String.class);
            Object faction = mGetFactionById.invoke(factionsInstance, factionId);
            if (faction == null) return;
            boolean econEnabled = (boolean) mEcon_isEnabled.invoke(null);
            if (!econEnabled) {
                plugin.getLogger().warning("[FactionOutposts] Factions econ disabled. Enable with /f config econEnabled true");
                return;
            }
            mEcon_modifyFactionBalance.invoke(null, faction, amount);
        } catch (Throwable t) {
            plugin.getLogger().warning("[FactionOutposts] depositToFactionBank reflection failed: " + t.getMessage());
        }
    }
}
