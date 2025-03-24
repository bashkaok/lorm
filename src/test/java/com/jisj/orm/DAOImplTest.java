package com.jisj.orm;

import org.junit.jupiter.api.*;
import com.jisj.orm.entity.JoinTableEntityIntID;
import com.jisj.orm.testdata.EmbeddedEntity;
import com.jisj.orm.testdata.MainEntity;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DAOImplTest {
    private static DBEnvironment db;
    private static DAOImpl<MainEntity, Integer> dao;
    private static final MainEntity me = MainEntity.builder()
            .stringField("stringColumn")
            .stringUniqueField("Unique")
            .doubleField(123.123)
            .floatField(345.345F)
            .booleanField(true)
            .build();

    @SuppressWarnings("unchecked")
    @BeforeAll
    public static void setUp() throws IOException {
        InputStream ins = DAOImplTest.class.getClassLoader().getResourceAsStream("log.properties");
        LogManager.getLogManager().readConfiguration(ins);

        db = new DBEnvironment(DBEnvironment.StandardConnection.MEMORY_CACHE);
        db.setStartMode(DBEnvironment.StartMode.DROP_AND_CREATE);
        db.initializeEntities(List.of(MainEntity.class, EmbeddedEntity.class));
        dao = (DAOImpl<MainEntity, Integer>) DAOFactory.createDAO(db.getDataSource(), MainEntity.class);
        assertNotNull(dao);
        assertEquals(4, db.getGlobal().getDaoSet().size());
    }

    @AfterAll
    static void close() {
        db.close();
    }

    @Test
    @Order(1)
    void create() throws SQLException {
        assertEquals(1, dao.create(me));
        assertNotEquals(null, me.getId());

        //Constraint
        assertSame(SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY, ((SQLiteException) assertThrows(SQLException.class, () -> dao.create(me))
                .getCause()).getResultCode());

        MainEntity copy = dao.read(me.getId()).orElseThrow();
        copy.setId(null);
        assertSame(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE, ((SQLiteException) assertThrows(SQLException.class, () -> dao.create(copy))
                .getCause()).getResultCode());


    }

    @Test
    @Order(2)
    void read() throws SQLException {
        assertEquals(me, dao.read(me.getId()).orElseThrow());
        assertNull(me.getUnAnnotatedField());
    }

    @Test
    @Order(3)
    void update() throws SQLException {

        MainEntity upd = MainEntity.builder()
                .stringField("stringColumn")
                .stringDefaultField("constrain_default")
                .stringUniqueField("Unique2")
                .doubleField(0.0)
                .floatField(0f)
                .booleanField(false)
                .build();
        assertEquals(1, dao.create(upd));
        assertNotEquals(-1, upd.getId());

        MainEntity expected = MainEntity.builder()
                .id(upd.getId())
                .stringField("updated_stringColumn")
                .stringDefaultField("constrain_default")
                .stringUniqueField("updated_Unique2")
                .doubleField(123.123)
                .floatField(345.345F)
                .booleanField(true)
                .build();

        upd.setStringField("updated_stringColumn");
        upd.setStringUniqueField("updated_Unique2");
        upd.setDoubleField(123.123);
        upd.setFloatField(345.345F);
        upd.setBooleanField(true);
        assertEquals(1, dao.update(upd));
        assertEquals(expected, dao.read(upd.getId()).orElseThrow());

        upd.setStringUniqueField("Unique");

        //Constraint with
        SQLException e = assertThrows(SQLException.class, () -> dao.update(upd));
        assertTrue(e.getMessage().contains("[SQLITE_CONSTRAINT_UNIQUE]"));
        assertTrue(e.getMessage().contains("UniqueField"));
    }


    @Test
    @Order(4)
    void refresh() throws SQLException {
        MainEntity refreshed = MainEntity.builder().id(me.getId()).build();
        assertEquals(1, dao.refresh(refreshed));
        assertEquals(me, refreshed);
    }

    @Test
    @Order(5)
    void findByUnique() throws SQLException {
        assertEquals(me, dao.findByUnique("UniqueField", "Unique").orElseThrow());
        assertTrue(assertThrowsExactly(IllegalArgumentException.class,
                ()-> dao.findByUnique("booleanField", true))
                .getMessage().contains("The column <booleanField> is not unique"));

        //for table constraint
        assertEquals(me,
                dao.findByUnique(new String[]{"stringField", "stringDefaultColumn"}, "stringColumn", "default").orElseThrow());

        //any not table constraint fields
        assertTrue(assertThrowsExactly(IllegalArgumentException.class,
                ()-> dao.findByUnique(new String[]{"doubleField", "booleanField"}, 123.123, true))
                .getMessage().contains("The columns <doubleField,booleanField> is not unique constraint"));
    }

    @Test
    @Order(6)
    void findAll() throws SQLException {
        MainEntity e1 = MainEntity.builder()
                .stringField("stringColumn1")
                .stringDefaultField("default1")
                .stringUniqueField("Unique1")
                .doubleField(1.0)
                .booleanField(true).build();
        MainEntity e2 = MainEntity.builder()
                .stringField("stringColumn2")
                .stringDefaultField("default2")
                .stringUniqueField("Unique2")
                .doubleField(200.0)
                .booleanField(true).build();
        MainEntity e3 = MainEntity.builder()
                .stringField("stringColumn3")
                .stringDefaultField("default3")
                .stringUniqueField("Unique3")
                .doubleField(1.0)
                .booleanField(false).build();
        assertEquals(1, dao.create(e1));
        assertEquals(1, dao.create(e2));
        assertEquals(1, dao.create(e3));
        assertEquals(2, dao.findAll("doubleField=1").size());
        assertEquals(1, dao.findAll("doubleField>199").size());
        assertEquals(1, dao.findAll("UniqueField=? AND doubleField=?", "Unique2", 200.0).size());
    }


    @Test
    @Order(7)
    void delete() throws SQLException {
        int id = me.getId();
        assertEquals(1, dao.delete(id));
        assertEquals(Optional.empty(), dao.read(id));
    }


    @Test
    @Order(8)
    void createAll() throws SQLException {
        MainEntity e1 = MainEntity.builder()
                .id(101)
                .stringField("Column101")
                .stringDefaultField("default101")
                .stringUniqueField("Unique101")
                .build();
        MainEntity e2 = MainEntity.builder()
                .id(102)
                .stringField("Column102")
                .stringDefaultField("default102")
                .stringUniqueField("Unique102")
                .build();
        MainEntity e3 = MainEntity.builder()
                .id(103)
                .stringField("Column103")
                .stringDefaultField("default103")
                .stringUniqueField("Unique103")
                .build();
        assertEquals(3, dao.createAll(List.of(e1,e2,e3)));
    }

    @Test
    @Order(9)
    void readAll() throws SQLException {
        dao.deleteAll("id>0");
        assertEquals(0, dao.readAll().count());

        MainEntity e1 = MainEntity.builder()
                .id(101)
                .stringField("Column101")
                .stringDefaultField("default101")
                .stringUniqueField("Unique101")
                .doubleField(0.0)
                .build();
        MainEntity e2 = MainEntity.builder()
                .id(102)
                .stringField("Column102")
                .stringDefaultField("default102")
                .stringUniqueField("Unique102")
                .doubleField(0.0)
                .build();
        MainEntity e3 = MainEntity.builder()
                .id(103)
                .stringField("Column103")
                .stringDefaultField("default103")
                .stringUniqueField("Unique103")
                .doubleField(0.0)
                .build();

        assertEquals(3, dao.createAll(List.of(e1,e2,e3)));
        assertEquals(e1, dao.read(101).orElseThrow());
        assertEquals(List.of(e1,e2,e3), dao.readAll().toList());

    }

    @Test
    @Order(10)
    void updateField() throws SQLException {
        MainEntity e1 = MainEntity.builder()
                .id(901)
                .stringField("Column901")
                .stringDefaultField("default901")
                .stringUniqueField("Unique901")
                .doubleField(0.0)
                .build();

        MainEntity e2 = MainEntity.builder()
                .id(902)
                .stringField("Column902")
                .stringDefaultField("default902")
                .stringUniqueField("Unique902")
                .doubleField(0.0)
                .build();

        assertEquals(2, dao.createAll(List.of(e1,e2)));
        assertEquals(1, dao.updateField(e1.getId(), "doubleField", 1.1));
        assertEquals(1.1, dao.read(e1.getId()).orElseThrow().getDoubleField());
        dao.updateField(e1.getId(), "stringUniqueField", "Unique903");
        assertEquals("Unique903", dao.read(e1.getId()).orElseThrow().getStringUniqueField());
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE, ((SQLiteException) assertThrowsExactly(SQLException.class, () -> dao.updateField(e1.getId(), "stringUniqueField", "Unique902"))
                .getCause()).getResultCode());
        //wrong id
        assertEquals(0, dao.updateField(-1, "stringUniqueField", "Unique903"));

    }

    @Test
    @Order(11)
    void query() throws SQLException {
        final String sql = """
                SELECT * FROM MainTable
                WHERE stringDefaultColumn LIKE ? AND doubleField IN (?)
                """;

        MainEntity e1 = MainEntity.builder()
                .stringField("stringColumn1")
                .stringDefaultField("default_query1")
                .stringUniqueField("Unique11")
                .doubleField(1.0)
                .booleanField(true).build();
        MainEntity e2 = MainEntity.builder()
                .stringField("stringColumn2")
                .stringDefaultField("default_query2")
                .stringUniqueField("Unique21")
                .doubleField(200.0)
                .booleanField(true).build();
        MainEntity e3 = MainEntity.builder()
                .stringField("stringColumn3")
                .stringDefaultField("default_not_query3")
                .stringUniqueField("Unique31")
                .doubleField(1.0)
                .booleanField(false).build();
        assertEquals(1, dao.create(e1));
        assertEquals(1, dao.create(e2));
        assertEquals(1, dao.create(e3));
        dao.readAll().forEach(System.out::println);
        List<?> response = dao.query(sql, "default_query%", Set.of(1.0,2.0,3.0));
        assertEquals(2, response.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void joinTable() throws SQLException {
        dao.create(me);
        DAOImpl<JoinTableEntityIntID, Integer> joinDao = (DAOImpl<JoinTableEntityIntID, Integer>) db.getGlobal().getDao("join_MainTable_with_EmbeddedTable");
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY, ((SQLiteException) assertThrowsExactly(SQLException.class,
                        ()-> joinDao.create(new JoinTableEntityIntID(99999, 9999)))
                .getCause()).getResultCode());
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY, ((SQLiteException) assertThrowsExactly(SQLException.class,
                ()-> joinDao.create(new JoinTableEntityIntID(me.getId(), 9999)))
                .getCause()).getResultCode());
//        DAOImpl<EmbeddedEntity, Integer> embedDao = (DAOImpl<EmbeddedEntity, Integer>) db.getGlobal().getDao(EmbeddedEntity.class);
        DAO<EmbeddedEntity, Integer> embedDao = (DAO<EmbeddedEntity, Integer>) db.getGlobal().getDao(EmbeddedEntity.class);
        EmbeddedEntity embed = EmbeddedEntity.builder().firstField("Wow").build();
        assertEquals(1,embedDao.create(embed));
        joinDao.create(new JoinTableEntityIntID(me.getId(), embed.getId()));
        assertEquals(1, joinDao.findAll("OWNER_ID=?", me.getId()).size());
        assertEquals(1, embedDao.delete(embed.getId()));
        assertEquals(0, joinDao.findAll("OWNER_ID=?", me.getId()).size());
    }
}