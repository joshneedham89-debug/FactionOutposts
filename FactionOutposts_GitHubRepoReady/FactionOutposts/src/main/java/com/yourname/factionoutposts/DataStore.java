
package com.yourname.factionoutposts;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataStore implements AutoCloseable {
    private final FactionOutposts plugin;
    private HikariDataSource ds;

    public DataStore(FactionOutposts plugin) {
        this.plugin = plugin;
        if (!plugin.getConfig().getBoolean("mysql.enabled", false)) return;
        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(plugin.getConfig().getString("mysql.jdbc_url","jdbc:mysql://localhost:3306/mc?useSSL=false&allowPublicKeyRetrieval=true"));
            cfg.setUsername(plugin.getConfig().getString("mysql.username","root"));
            cfg.setPassword(plugin.getConfig().getString("mysql.password",""));
            cfg.setMaximumPoolSize(5);
            cfg.setPoolName("FactionOutpostsPool");
            ds = new HikariDataSource(cfg);
            init();
        } catch (Throwable t) {
            plugin.getLogger().warning("[FactionOutposts] Failed to init MySQL: " + t.getMessage());
            ds = null;
        }
    }

    public boolean isActive(){ return ds != null; }

    private void init() throws SQLException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS outpost_owners (name VARCHAR(64) PRIMARY KEY, owner VARCHAR(64) NULL)")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS outpost_payouts (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64), faction VARCHAR(64), amount DOUBLE, paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")) {
                ps.executeUpdate();
            }
        }
    }

    public String readOwner(String outpostName) {
        if (ds == null) return null;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT owner FROM outpost_owners WHERE name=?")) {
            ps.setString(1, outpostName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) { plugin.getLogger().warning("[FactionOutposts] readOwner: " + e.getMessage()); }
        return null;
    }

    public void writeOwner(String outpostName, String owner) {
        if (ds == null) return;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO outpost_owners(name,owner) VALUES(?,?) ON DUPLICATE KEY UPDATE owner=VALUES(owner)")) {
            ps.setString(1, outpostName);
            ps.setString(2, owner);
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning("[FactionOutposts] writeOwner: " + e.getMessage()); }
    }

    public void logPayout(String outpostName, String faction, double amount){
        if (ds == null) return;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO outpost_payouts(name,faction,amount) VALUES(?,?,?)")) {
            ps.setString(1, outpostName);
            ps.setString(2, faction);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning("[FactionOutposts] logPayout: " + e.getMessage()); }
    }

    @Override
    public void close() {
        if (ds != null) ds.close();
    }
}
