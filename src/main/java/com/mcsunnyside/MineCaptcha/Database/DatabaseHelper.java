package com.mcsunnyside.MineCaptcha.Database;

import com.mcsunnyside.MineCaptcha.MineCaptcha;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {
    public static void createInfoTable(@NotNull Database db, @NotNull MineCaptcha plugin) throws SQLException {
        Statement st = db.getConnection().createStatement();
        String createTable = "CREATE TABLE IF NOT EXISTS "+plugin.getConfig().getString("database.tableprefix")+"info"+" (username VARCHAR(255) NOT NULL PRIMARY KEY, ipaddress VARCHAR(255) NOT NULL)";
        st.execute(createTable);
    }


    public static PlayerQueryResult queryPlayer(@NotNull Database db, @NotNull MineCaptcha plugin, @NotNull String playerName) throws SQLException {
        String queryPlayer = "SELECT username,ipaddress FROM "+plugin.getConfig().getString("database.tableprefix")+"info"+" WHERE username=? LIMIT 1";
        PreparedStatement st = db.getConnection().prepareStatement(queryPlayer);
        st.setString(1,playerName);
        ResultSet set = st.executeQuery();
        set.last();
        if(set.getRow()<1)
            return new PlayerQueryResult(false,null,null);
        return new PlayerQueryResult(true,set.getString("username"),set.getString("ipaddress"));
    }
}
