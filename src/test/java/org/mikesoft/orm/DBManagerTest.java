package org.mikesoft.orm;

import org.junit.jupiter.api.*;
import org.mikesoft.orm.testdata.MainEntity;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mikesoft.orm.StatementBuilder.buildCreateTableStatement;
import static org.mikesoft.orm.utils.sqlExWrap;
import static org.mikesoft.orm.utils.streamOf;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DBManagerTest {
    static DAOImpl<?,?> dao;
    static SQLiteDataSource dataSource = new SQLiteDataSource();

    @BeforeAll
    public static void setUp() throws IOException {
        InputStream ins = DBManagerTest.class.getClassLoader().getResourceAsStream("log.properties");
        LogManager.getLogManager().readConfiguration(ins);
        DBManager.log.setLevel(Level.FINE);
//        dataSource.setUrl("jdbc:sqlite::memory:");
        dataSource.setUrl("jdbc:sqlite:memory:?cache=shared");
//        dataSource.setUrl("jdbc:sqlite:file:target/test-data/testdb.sqlite");
        dao = (DAOImpl<?,?>) DAOFactory.createDAO(dataSource, MainEntity.class);
        DBManager.createTableIfNotExists(dao);
    }

    @AfterAll
    static void close() {
    }

    @Test
    @Order(1)
    void dropTableIfExists() {
        DBManager.createTableIfNotExists(dao);
        assertTrue(DBManager.tableExists(dao));
        DBManager.dropTableIfExists(dao);
        assertFalse(DBManager.tableExists(dao));
    }

    @Test
    @Order(2)
    void createTableIfNotExists() {
        DBManager.dropTableIfExists(dao);
        assertFalse(DBManager.tableExists(dao));
        DBManager.createTableIfNotExists(dao);
        assertTrue(DBManager.tableExists(dao));
    }

    @Test
    void getMetaData() {

    }

    @Test
    void getTables() throws SQLException {
        dao.withConnection(connection -> {
            System.out.println("Tables:");
            DBManager.getTables(connection).forEach(tbl -> System.out.println(sqlExWrap(() -> tbl.getString(3))));
            System.out.println("Schemas:");
            streamOf(DBManager.getMetaData(connection).getSchemas())
                    .forEach(sc -> System.out.println(sqlExWrap(() -> sc.getString(1))));
            return null;
        });
    }

    @Test
    public void getCreateStatement_from_DB() throws SQLException {
        DBManager.createTableIfNotExists(dao);
        String fromDB;
        try (var connection = dataSource.getConnection()) {
            fromDB = DBManager.getCreateStatement(connection, dao.getProfile().getTableName());
            assertFalse(fromDB.isEmpty());
        }
        assertEquals(fromDB, buildCreateTableStatement(dao.getProfile(), false));
    }

    @Test
    void tableEquals() {
        assertTrue(DBManager.tableEquals(dao));
    }


}