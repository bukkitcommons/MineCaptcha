package com.mcsunnyside.MineCaptcha.Database;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
//从自己的QuickShop-Reremake搬过来的改进版MySQL连接
public class MySQLCore {
    private static final int MAX_CONNECTIONS = 10; //改为最高10连接，多加2个connection，同时处理更多数据
    private static ArrayList<Connection> pool = new ArrayList<>();
    /** The connection properties... user, pass, autoReconnect.. */
    private Properties info;
    private String url;

    public MySQLCore(@NotNull String host, @NotNull String user, @NotNull String pass, @NotNull String database, int port, boolean useSSL) {
        info = new Properties();
        info.setProperty("autoReconnect", "true");
        info.setProperty("user", user);
        info.setProperty("password", pass);
        info.setProperty("useUnicode", "true");
        info.setProperty("characterEncoding", "utf8");
        info.setProperty("useSSL", String.valueOf(useSSL));
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database;
        for (int i = 0; i < MAX_CONNECTIONS; i++)
            pool.add(null);
    }

    public void close() {
        // Nothing, because queries are executed immediately for MySQL
    }

    public void flush() {
        // Nothing, because queries are executed immediately for MySQL
    }

    public void queue(@NotNull BufferStatement bs) {
        try {
            Connection con = this.getConnection();
            while (con == null) {
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    //ignore
                }
                // Try again
                con = this.getConnection();
            }
            PreparedStatement ps = bs.prepareStatement(con);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the database connection for executing queries on.
     *
     * @return The database connection
     */
    public Connection getConnection() {
        for (int i = 0; i < MAX_CONNECTIONS; i++) {
            Connection connection = pool.get(i);
            try {
                // If we have a current connection, fetch it
                if (connection != null && !connection.isClosed()) {
                    if (connection.isValid(10)) {
                        return connection;
                    }
                    // Else, it is invalid, so we return another connection.
                }
                connection = DriverManager.getConnection(this.url, info);
                pool.set(i, connection);
                return connection;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}