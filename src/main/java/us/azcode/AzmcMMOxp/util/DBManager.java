package us.azcode.AzmcMMOxp.util;

import us.azcode.AzmcMMOxp.AzXPBoost;
import us.azcode.AzmcMMOxp.model.BoostType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DBManager {
    private Connection connection;
    private final BoostManager boostManager;
    private final AzXPBoost plugin;

    public DBManager(AzXPBoost plugin, BoostManager boostManager) {
        this.plugin = plugin;
        this.boostManager = boostManager;
        setupDatabase();
    }


    private void setupDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = plugin.getDataFolder() + File.separator + "boosts.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS player_boosts (uuid TEXT, boost_name TEXT, available INTEGER, quantity INTEGER)");
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveBoost(UUID uuid, BoostType boostType) {
        try {
            try (PreparedStatement checkStatement = connection.prepareStatement("SELECT quantity FROM player_boosts WHERE uuid = ? AND boost_name = ?")) {
                checkStatement.setString(1, uuid.toString());
                checkStatement.setString(2, boostType.getName());
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (resultSet.next()) {
                        int newQuantity = resultSet.getInt("quantity") + 1;
                        try (PreparedStatement updateStatement = connection.prepareStatement("UPDATE player_boosts SET quantity = ? WHERE uuid = ? AND boost_name = ?")) {
                            updateStatement.setInt(1, newQuantity);
                            updateStatement.setString(2, uuid.toString());
                            updateStatement.setString(3, boostType.getName());
                            updateStatement.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO player_boosts (uuid, boost_name, available, quantity) VALUES (?, ?, ?, ?)")) {
                            insertStatement.setString(1, uuid.toString());
                            insertStatement.setString(2, boostType.getName());
                            insertStatement.setInt(3, 1);
                            insertStatement.setInt(4, 1);
                            insertStatement.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<BoostType> loadBoosts(UUID uuid) {
        List<BoostType> boosts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT boost_name, quantity FROM player_boosts WHERE uuid = ? AND available = 1")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String boostName = resultSet.getString("boost_name");
                    int quantity = resultSet.getInt("quantity");
                    BoostType boostType = boostManager.getBoostType(boostName);
                    if (boostType != null && quantity > 0) {
                        boosts.add(boostType);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return boosts;
    }

    public void setBoostUnavailable(UUID uuid, BoostType boostType) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE player_boosts SET quantity = quantity - 1 WHERE uuid = ? AND boost_name = ?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, boostType.getName());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
