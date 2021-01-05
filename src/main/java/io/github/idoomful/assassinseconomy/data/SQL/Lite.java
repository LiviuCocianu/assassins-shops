package io.github.idoomful.assassinseconomy.data.SQL;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;

public class Lite<T extends JavaPlugin> {
    private Connection con = null;
    private final T plugin;

    // TODO set these up
    private final String DATABASE = "";
    private final String TABLE = "";

    public Lite(T plugin) {
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
                    "`name` varchar(16)" +
                    ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
