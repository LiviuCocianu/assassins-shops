package io.github.idoomful.assassinseconomy.data.SQL;

import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.HashMap;

public class Lite {
    private Connection con = null;
    private final DMain plugin;

    // TODO set these up
    private final String DATABASE = "players";
    private final String TABLE = "balance";

    public Lite(DMain plugin) {
        this.plugin = plugin;

        setup();
        getConnection();
    }

    public boolean isConnectionActive() {
        try {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Connection getConnection() {
        try {
            if(isConnectionActive()) return con;
            else con = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + DATABASE + ".db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return con;
    }

    private void setup() {
        try(
                Connection connection = getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.execute("CREATE TABLE IF NOT EXISTS `" + TABLE + "`(" +
                    "`name` varchar(16), " +
                    "`balanceMap` JSON" +
                    ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void exists(String player, Callback<Boolean> result) {
        try(PreparedStatement ps = con.prepareStatement("SELECT 1 FROM " + TABLE + " WHERE `name`=?")) {
            ps.setString(1, player);
            ResultSet rs = ps.executeQuery();
            result.done(rs.next());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void addEntry(String player, HashMap<String, Integer> balances) {
        try(PreparedStatement ps = con.prepareStatement("INSERT INTO " + TABLE + " (name,balanceMap) VALUES (?,?)")) {
            ps.setString(1, player);
            ps.setString(2, plugin.getGson().toJson(new MapWrapper(balances)));
            ps.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void removeEntry(String player) {
        try(PreparedStatement ps = con.prepareStatement("DELETE FROM " + TABLE + " WHERE `name`=?")) {
            ps.setString(1, player);
            ps.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void getCurrencies(String player, Callback<HashMap<String, Integer>> result) {
        try(PreparedStatement ps = con.prepareStatement("SELECT `balanceMap` FROM " + TABLE + " WHERE `name`=?")) {
            ps.setString(1, player);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                MapWrapper map = plugin.getGson().fromJson(rs.getString(1), MapWrapper.class);

                if(SettingsYML.Currencies.OPTIONS.getIDs().size() > map.getMap().size()) {
                    SettingsYML.Currencies.OPTIONS.getIDs().forEach(id -> {
                        if(!map.getMap().containsKey(id)) map.getMap().put(id, 0);
                    });
                } else if(SettingsYML.Currencies.OPTIONS.getIDs().size() < map.getMap().size()) {
                    new HashMap<>(map.getMap()).keySet().forEach(id -> {
                        if(!SettingsYML.Currencies.OPTIONS.getIDs().contains(id)) map.getMap().remove(id);
                    });
                }

                result.done(map.getMap());
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void setCurrencies(String player, HashMap<String, Integer> map) {
        try(PreparedStatement ps = con.prepareStatement("UPDATE " + TABLE + " SET `balanceMap`=? WHERE `name`=?")) {
            ps.setString(1, plugin.getGson().toJson(new MapWrapper(map)));
            ps.setString(2, player);
            ps.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void addCurrency(String player, String currency, int amount) {
        getCurrencies(player, map -> {
            HashMap<String, Integer> output = new HashMap<>(map);
            Integer old = output.get(currency);
            output.put(currency, old + amount);
            setCurrencies(player, output);
        });
    }

    public void subtractCurrency(String player, String currency, int amount) {
        getCurrencies(player, map -> {
            HashMap<String, Integer> output = new HashMap<>(map);
            output.put(currency, Math.max((output.get(currency) - amount), 0));
            setCurrencies(player, output);
        });
    }
}
