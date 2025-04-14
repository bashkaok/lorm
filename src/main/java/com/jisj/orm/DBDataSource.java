package com.jisj.orm;

import org.sqlite.SQLiteDataSource;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 *  Factory for connections to the physical data source
 */
public class DBDataSource implements DataSource {
    private static final Logger log = Logger.getLogger(DBDataSource.class.getName());
    private final DataSource dataSource;

    private DBDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
        config.enableCaseSensitiveLike(false);
        config.enforceForeignKeys(true);
        ((SQLiteDataSource)dataSource).setConfig(config);
        log.fine("New instance: " + config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Unwrap the SQLLite datasource
     * @param iface A Class defining an interface that the result must implement.
     * @return SQLite data source implementation
     * @param <T> {@link SQLiteDataSource} || {@link SQLiteConnectionPoolDataSource}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) return (T) dataSource;
        throw new SQLFeatureNotSupportedException("Datasource not found for " + iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface == SQLiteDataSource.class || iface == SQLiteConnectionPoolDataSource.class;
    }

    /**
     * Creates new {@link org.sqlite.SQLiteDataSource SQLiteDataSource} data source instance with standard connection
     * @param standardConnection {@link StandardConnection}
     * @return DBDataSource instance
     * @see #isWrapperFor(Class)
     */
    public static DBDataSource newDataSource(StandardConnection standardConnection) {
        DBDataSource db = new DBDataSource(new SQLiteDataSource());
        ((SQLiteDataSource)db.dataSource).setUrl(standardConnection.getUrl());
        log.info("Connection set: " + ((SQLiteDataSource)db.dataSource).getUrl());
        return db;
    }

    /**
     * Creates new {@link org.sqlite.SQLiteDataSource SQLiteDataSource} data source instance with connection string
     * @param dbPath path to DB file
     * @return DBDataSource instance
     */
    public static DBDataSource newDataSource(Path dbPath) {
        DBDataSource db = new DBDataSource(new SQLiteDataSource());
        ((SQLiteDataSource)db.dataSource).setUrl("jdbc:sqlite:" + dbPath.toAbsolutePath());
        log.info("Connection set: " + ((SQLiteDataSource)db.dataSource).getUrl());
        return db;
    }

    /**
     * Creates new pooled {@link SQLiteConnectionPoolDataSource} data source instance with standard connection
     * @param standardConnection {@link StandardConnection}
     * @return DBDataSource instance
     * @see #isWrapperFor(Class)
     */
    public static DBDataSource newPooledDataSource(StandardConnection standardConnection) {
        DBDataSource db = new DBDataSource(new SQLiteConnectionPoolDataSource());
        ((SQLiteDataSource)db.dataSource).setUrl(standardConnection.getUrl());
        log.info("Connection set: " + ((SQLiteDataSource)db.dataSource).getUrl());
        return db;
    }

    /**
     *
     * @return The location of the database file
     */
    public String getUrl() {
        return ((SQLiteDataSource)dataSource).getUrl();
    }

    /**
     * Simply database connection
     */
    @SuppressWarnings("LombokGetterMayBeUsed")
    public enum StandardConnection {
        /**
         * DB will be created in memory
         */
        MEMORY("jdbc:sqlite::memory:"),
        /**
         * DB will be created in memory common to all connections
         */
        MEMORY_CACHE("jdbc:sqlite:memory:?cache=shared"),
        FILE_MEMORY_MODE("jdbc:sqlite:file:?mode=memory&cache=shared"),
        /**
         * Connection string sets at
         */
        FILE_CURRENT_PATH("jdbc:sqlite:file:");

        private final String url;

        StandardConnection(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

}
