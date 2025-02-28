package org.mikesoft.orm.repository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mikesoft.orm.DAOException;
import org.mikesoft.orm.DBEnvironment;
import org.mikesoft.orm.entity.JoinTableEntityIntID;
import org.mikesoft.orm.testdata.EmbeddedEntity;
import org.mikesoft.orm.testdata.MainEntity;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;

class PersistRepositoryImplTest {
    private static DBEnvironment db;
    private static PersistRepository<MainEntity, Integer> per;

    @SuppressWarnings("unchecked")
    @BeforeAll
    public static void setUp() throws IOException {
        InputStream ins = PersistRepositoryImplTest.class.getClassLoader().getResourceAsStream("log.properties");
        LogManager.getLogManager().readConfiguration(ins);

        SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
        dataSource.setUrl("jdbc:sqlite:memory:&cache=shared");
        db = new DBEnvironment(DBEnvironment.StandardConnection.MEMORY_CACHE);
//        db = new DBEnvironment(DBEnvironment.StandardConnection.FILE_CURRENT_PATH);
        db.setStartMode(DBEnvironment.StartMode.DROP_AND_CREATE);
        db.initializeEntities(MainEntity.class, EmbeddedEntity.class);
//        per = (PersistRepository<MainEntity, Integer>) new PersistRepositoryImpl<>(db.getGlobal().getCrudRepository(MainEntity.class));
        per = (PersistRepository<MainEntity, Integer>) db.getGlobal().getPersistRepository(MainEntity.class);
        assertNotNull(per);
    }

    @AfterAll
    static void close() {
        db.close();
    }


    @Test
    void save() throws DAOException {
        MainEntity main = MainEntity.builder()
                .stringField("stringColumn")
                .stringUniqueField("Unique")
                .stringDefaultField("DEFAULT")
                .doubleField(123.123)
                .floatField(345.345F)
                .booleanField(true)
                .build();

        EmbeddedEntity e1 = EmbeddedEntity.builder().firstField("Field1").build();
        EmbeddedEntity e2 = EmbeddedEntity.builder().firstField("Field1").build();
        //join_MainTable_with_EmbeddedTable
        main.setEmbeddedList(List.of((e1)));
        //MainTable_EmbeddedTable
        main.setEmbeddedListDefault(List.of(e2));

        per.save(main);
        assertNotNull(main.getId());
        assertTrue(main.getId() > 0);
        assertNotNull(e1.getId());
        assertTrue(e1.getId() > 0);
        assertTrue(e2.getId() > 0);
        JoinCRUDRepository<?,?> joinCrud1 = (JoinCRUDRepository<?, ?>) db.getGlobal().getCrudRepository("join_MainTable_with_EmbeddedTable");
        JoinTableEntityIntID join1 = (JoinTableEntityIntID) joinCrud1.getAll().findFirst().orElseThrow();
        assertEquals(join1.getOwnerId(), main.getId());
        assertEquals(join1.getEmbeddedId(), e1.getId());

        JoinCRUDRepository<?,?> joinCrud2 = (JoinCRUDRepository<?, ?>) db.getGlobal().getCrudRepository("MainTable_EmbeddedTable");
        JoinTableEntityIntID join2 = (JoinTableEntityIntID) joinCrud2.getAll().findFirst().orElseThrow();
        assertEquals(join2.getOwnerId(), main.getId());
        assertEquals(join2.getEmbeddedId(), e2.getId());

    }

    @Test
    void saveOrUpdate() {
    }

    @Test
    void load() {
    }

    @Test
    void update() {
    }

    @Test
    void refresh() {
    }

    @Test
    void persist() {
    }
}