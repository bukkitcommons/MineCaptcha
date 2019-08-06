package com.mcsunnyside.MineCaptcha.Database;

import org.jetbrains.annotations.NotNull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
    private MySQLCore core;
    // Fix null pointer...

    /**
     * Creates a new database and validates its connection.
     * <p>
     * If the connection is invalid, this will throw a ConnectionException.
     *
     * @param core The core for the database, either MySQL or SQLite.
     * @throws ConnectionException If the connection was invalid
     */
    public Database(@NotNull MySQLCore core) throws ConnectionException {
        try {
            try {
                if (!core.getConnection().isValid(10)) {
                    throw new ConnectionException("The database does not appear to be valid!");
                }
            } catch (AbstractMethodError e) {
                // You don't need to validate this core.
            }
        } catch (SQLException e) {
            throw new ConnectionException(e.getMessage());
        }
        this.core = core;
    }

    /**
     * Closes the database
     */
    public void close() {
        this.core.close();
    }


    /**
     * Executes the given statement either immediately, or soon.
     *
     * @param query The query
     * @param objs  The string values for each ? in the given query.
     */
    public void execute(@NotNull String query, @NotNull Object... objs) {
        BufferStatement bs = new BufferStatement(query, objs);
        core.queue(bs);
    }

    /**
     * Returns true if the given table has the given column
     *
     * @param table  The table
     * @param column The column
     * @return True if the given table has the given column
     * @throws SQLException If the database isn't connected
     */
    public boolean hasColumn(@NotNull String table, @NotNull String column) throws SQLException {
        if (!hasTable(table))
            return false;
        String query = "SELECT * FROM " + table + " LIMIT 0,1";
        try {
            PreparedStatement ps = this.getConnection().prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getString(column) != null)
                    return true;
            }
        } catch (SQLException e) {
            return false;
        }
        return false; // Uh, wtf.
    }

    /**
     * Returns true if the table exists
     *
     * @param table The table to check for
     * @return True if the table is found
     * @throws SQLException Throw exception when failed execute somethins on SQL
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean hasTable(@NotNull String table) throws SQLException {
        ResultSet rs = getConnection().getMetaData().getTables(null, null, "%", null);
        while (rs.next()) {
            if (table.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                rs.close();
                return true;
            }
        }
        rs.close();
        return false;
    }

    /**
     * Fetches the connection to this database for querying. Try to avoid doing
     * this in the main thread.
     *
     * @return Fetches the connection to this database for querying.
     */
    public Connection getConnection() {
        return core.getConnection();
    }

    /**
     * Returns the database core object, that this database runs on.
     *
     * @return the database core object, that this database runs on.
     */
    public MySQLCore getCore() {
        return core;
    }

    /**
     * Represents a connection error, generally when the server can't connect to
     * MySQL or something.
     */
    public static class ConnectionException extends Exception {
        private static final long serialVersionUID = 8348749992936357317L;
        private ConnectionException(String msg) {
            super(msg);
        }
    }
}