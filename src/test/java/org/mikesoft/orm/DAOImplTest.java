package org.mikesoft.orm;

import org.mikesoft.orm.entity.JoinTableEntity;
import org.junit.jupiter.api.*;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.LogManager;

import static org.dazlib.config.Prefs.LOG_PROPERTIES_NAME;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DAOImplTest {
    private static DBEnvironment db;
    private static DAOImpl<MainTestEntity> dao;
    private static MainTestEntity me = MainTestEntity.builder()
            .stringField("stringColumn")
            .stringUniqueField("Unique")
            .doubleField(123.123)
            .floatField(345.345F)
            .booleanField(true)
            .build();

    @SuppressWarnings("unchecked")
    @BeforeAll
    public static void setUp() throws IOException {
        InputStream ins = DAOImplTest.class.getClassLoader().getResourceAsStream(LOG_PROPERTIES_NAME.defValue);
        LogManager.getLogManager().readConfiguration(ins);

        db = new DBEnvironment(DBEnvironment.StandardConnection.MEMORY_CACHE);
        db.setStartMode(DBEnvironment.StartMode.DROP_AND_CREATE);
        db.initializeEntities(List.of(MainTestEntity.class, EmbeddedEntity.class));
        dao = (DAOImpl<MainTestEntity>) DAOFactory.createDAO(db.getDataSource(), MainTestEntity.class);
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
        assertSame(((SQLiteException) assertThrows(SQLException.class, () -> dao.create(me))
                .getCause()).getResultCode(), SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY);

        MainTestEntity copy = dao.read(me.getId()).orElseThrow();
        copy.setId(null);
        assertSame(((SQLiteException) assertThrows(SQLException.class, () -> dao.create(copy))
                .getCause()).getResultCode(), SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE);


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

        MainTestEntity upd = MainTestEntity.builder()
                .stringField("stringColumn")
                .stringDefaultField("constrain_default")
                .stringUniqueField("Unique2")
                .doubleField(0.0)
                .floatField(0f)
                .booleanField(false)
                .build();
        assertEquals(1, dao.create(upd));
        assertNotEquals(-1, upd.getId());

        MainTestEntity expected = MainTestEntity.builder()
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
        MainTestEntity refreshed = MainTestEntity.builder().id(me.getId()).build();
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
        MainTestEntity e1 = MainTestEntity.builder()
                .stringField("stringColumn1")
                .stringDefaultField("default1")
                .stringUniqueField("Unique1")
                .doubleField(1.0)
                .booleanField(true).build();
        MainTestEntity e2 = MainTestEntity.builder()
                .stringField("stringColumn2")
                .stringDefaultField("default2")
                .stringUniqueField("Unique2")
                .doubleField(200.0)
                .booleanField(true).build();
        MainTestEntity e3 = MainTestEntity.builder()
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
        MainTestEntity e1 = MainTestEntity.builder()
                .id(101)
                .stringField("Column101")
                .stringDefaultField("default101")
                .stringUniqueField("Unique101")
                .build();
        MainTestEntity e2 = MainTestEntity.builder()
                .id(102)
                .stringField("Column102")
                .stringDefaultField("default102")
                .stringUniqueField("Unique102")
                .build();
        MainTestEntity e3 = MainTestEntity.builder()
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

        MainTestEntity e1 = MainTestEntity.builder()
                .id(101)
                .stringField("Column101")
                .stringDefaultField("default101")
                .stringUniqueField("Unique101")
                .doubleField(0.0)
                .build();
        MainTestEntity e2 = MainTestEntity.builder()
                .id(102)
                .stringField("Column102")
                .stringDefaultField("default102")
                .stringUniqueField("Unique102")
                .doubleField(0.0)
                .build();
        MainTestEntity e3 = MainTestEntity.builder()
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
        MainTestEntity e1 = MainTestEntity.builder()
                .id(901)
                .stringField("Column901")
                .stringDefaultField("default901")
                .stringUniqueField("Unique901")
                .doubleField(0.0)
                .build();

        MainTestEntity e2 = MainTestEntity.builder()
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
        assertEquals(((SQLiteException) assertThrowsExactly(SQLException.class, () -> dao.updateField(e1.getId(), "stringUniqueField", "Unique902"))
                .getCause()).getResultCode(), SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE);
        //wrong id
        assertEquals(0, dao.updateField(-1, "stringUniqueField", "Unique903"));

    }

    @SuppressWarnings("unchecked")
    @Test
    void joinTable() throws SQLException {
        dao.create(me);
        DAOImpl<JoinTableEntity<Integer>> joinDao = (DAOImpl<JoinTableEntity<Integer>>) db.getGlobal().getDao("tbl_joined");
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY, ((SQLiteException) assertThrowsExactly(SQLException.class,
                        ()-> joinDao.create(new JoinTableEntity<>(99999, 9999)))
                .getCause()).getResultCode());
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY, ((SQLiteException) assertThrowsExactly(SQLException.class,
                ()-> joinDao.create(new JoinTableEntity<>(me.getId(), 9999)))
                .getCause()).getResultCode());
        DAOImpl<EmbeddedEntity> embedDao = (DAOImpl<EmbeddedEntity>) db.getGlobal().getDao(EmbeddedEntity.class);
        EmbeddedEntity embed = EmbeddedEntity.builder().firstField("Wow").build();
        assertEquals(1,embedDao.create(embed));
        joinDao.create(new JoinTableEntity<>(me.getId(), embed.getId()));
        assertEquals(1, joinDao.findAll("OWNER_ID=?", me.getId()).size());
        assertEquals(1, embedDao.delete(embed.getId()));
        assertEquals(0, joinDao.findAll("OWNER_ID=?", me.getId()).size());
    }
}