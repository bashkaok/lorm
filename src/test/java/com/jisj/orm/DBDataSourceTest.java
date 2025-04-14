package com.jisj.orm;

import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static com.jisj.orm.Const.TST_PATH;
import static org.junit.jupiter.api.Assertions.*;

class DBDataSourceTest {

    @Test
    void unwrap() {
    }

    @Test
    void isWrapperFor() {
        DBDataSource ds = DBDataSource.newPooledDataSource(DBDataSource.StandardConnection.MEMORY);
        assertTrue(ds.isWrapperFor(SQLiteConnectionPoolDataSource.class));
        ds = DBDataSource.newDataSource(DBDataSource.StandardConnection.MEMORY);
        assertTrue(ds.isWrapperFor(SQLiteDataSource.class));
    }

    @Test
    void newDataSource() throws IOException, SQLException {
        Path testDb = TST_PATH.resolve("test-db.sqlite");
        Files.deleteIfExists(testDb);
        DBDataSource.newDataSource(testDb).getConnection();
        assertTrue(Files.exists(testDb));
    }
}