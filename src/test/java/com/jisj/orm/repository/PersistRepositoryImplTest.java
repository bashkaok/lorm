package com.jisj.orm.repository;

import com.jisj.orm.DBDataSource;
import com.jisj.orm.testdata.NestedEntity;
import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import com.jisj.orm.DAOException;
import com.jisj.orm.DBEnvironment;
import com.jisj.orm.entity.JoinTableEntityIntID;
import com.jisj.orm.testdata.EmbeddedEntity;
import com.jisj.orm.testdata.MainEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Log
class PersistRepositoryImplTest {
    private static DBEnvironment db;
    private static PersistRepository<MainEntity, Integer> per;
    private static PersistRepository<NestedEntity, Integer> perNest;
    private static CRUDRepository<EmbeddedEntity, ?> crudEmbed;

    @SuppressWarnings("unchecked")
    @BeforeAll
    public static void setUp() throws IOException {
        InputStream ins = PersistRepositoryImplTest.class.getClassLoader().getResourceAsStream("log-test.properties");
        LogManager.getLogManager().readConfiguration(ins);
        log.setLevel(Level.FINE);

        db = DBEnvironment.getInstance(DBDataSource.newPooledDataSource(DBDataSource.StandardConnection.MEMORY_CACHE));
        db.setStartMode(DBEnvironment.StartMode.DROP_AND_CREATE);
        db.initializeEntities(MainEntity.class, EmbeddedEntity.class, NestedEntity.class);

        per = (PersistRepository<MainEntity, Integer>) db.getGlobal().getPersistRepository(MainEntity.class);
        perNest = (PersistRepository<NestedEntity, Integer>) db.getGlobal().getPersistRepository(NestedEntity.class);
        crudEmbed = (CRUDRepository<EmbeddedEntity, ?>) db.getGlobal().getCrudRepository(EmbeddedEntity.class);
        assertNotNull(per);
    }

    @AfterAll
    static void close() {
        db.close();
    }


    @Test
    @Order(1)
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
        JoinCRUDRepository<?, ?> joinCrud1 = (JoinCRUDRepository<?, ?>) db.getGlobal().getCrudRepository("join_MainTable_with_EmbeddedTable");
//        JoinTableEntityIntID join1 = (JoinTableEntityIntID) joinCrud1.getAll().findFirst().orElseThrow(); //not terminal operator!!! Stream is not closed!!!
        JoinTableEntityIntID join1 = (JoinTableEntityIntID) joinCrud1.getAll().toList().getFirst();
        assertEquals(join1.getOwnerId(), main.getId());
        assertEquals(join1.getEmbeddedId(), e1.getId());

        JoinCRUDRepository<?, ?> joinCrud2 = (JoinCRUDRepository<?, ?>) db.getGlobal().getCrudRepository("MainTable_EmbeddedTable");
        JoinTableEntityIntID join2 = (JoinTableEntityIntID) joinCrud2.getAll().toList().getFirst();
        assertEquals(join2.getOwnerId(), main.getId());
        assertEquals(join2.getEmbeddedId(), e2.getId());
    }

    @Test
    @Order(2)
    void loadOptional() {
        MainEntity main = per.loadOptional(1).orElseThrow();
        assertEquals("stringColumn", main.getStringField());
        assertEquals(1, main.getEmbeddedList().size());
        assertEquals(1, main.getEmbeddedListDefault().size());
    }

    @Test
    @Order(3)
    void refresh() throws DAOException {
        MainEntity main = MainEntity.builder().id(1).build();
        per.refresh(main);
        assertEquals("stringColumn", main.getStringField());
        assertEquals(1, main.getEmbeddedList().size());
        assertEquals(1, main.getEmbeddedListDefault().size());

        main.setId(2);
        assertEquals(DAOException.ErrorCode.RECORD_NOT_FOUND,
                assertThrowsExactly(DAOException.class, () -> per.refresh(main))
                        .getErrorCode());
    }

    @Test
    @Order(4)
    void update() throws DAOException {
        MainEntity main = MainEntity.builder()
                .stringField("stringColumn_for_Update")
                .stringUniqueField("Unique_update")
                .stringDefaultField("DEFAULT")
                .doubleField(123.123)
                .floatField(345.345F)
                .booleanField(true)
                .build();
        per.save(main);

        EmbeddedEntity e3 = EmbeddedEntity.builder().firstField("Third field").build();
        main.setUnAnnotatedField("UnAnnotatedField");
        main.setEmbeddedList(List.of(e3));
        MainEntity forTest = main;
        //e3 is not persisted
        assertThrowsExactly(IllegalArgumentException.class, () -> per.update(forTest));

        crudEmbed.add(e3);
        main = per.loadOptional(1).orElseThrow();
        main.setEmbeddedList(new ArrayList<>(main.getEmbeddedList()));
        main.getEmbeddedList().add(e3);
        per.update(main);

        main = per.loadOptional(main.getId()).orElseThrow();
        assertEquals(1, main.getEmbeddedList().stream()
                .map(EmbeddedEntity::getId)
                .filter(id -> Objects.equals(id, e3.getId()))
                .count());
    }

    @Test
    void persist() {
        MainEntity main = MainEntity.builder()
                .stringField("stringColumn_for_persist")
                .stringUniqueField("Unique_persist")
                .stringDefaultField("DEFAULT")
                .doubleField(123.123)
                .floatField(345.345F)
                .booleanField(true)
                .unAnnotatedField("forPersist")
                .build();
        per.persist(main);
        assertEquals(per.getCRUD().getOptional(main.getId()).orElseThrow(), main);

        EmbeddedEntity e1 = EmbeddedEntity.builder().firstField("First embedded").build();
        EmbeddedEntity e2 = EmbeddedEntity.builder().firstField("Second embedded").build();
        main.setEmbeddedList(List.of(e1, e2));

        per.persist(main);
        main.setEmbeddedListDefault(List.of());
        assertEquals(per.loadOptional(main.getId()).orElseThrow(), main);
    }

    @Test
    void persist_nested() {
        NestedEntity child111 = NestedEntity.builder()
                .name("Child111")
                .build();
        NestedEntity child11 = NestedEntity.builder()
                .name("Child11")
                .nestedChild(List.of(child111))
                .build();
        NestedEntity child1 = NestedEntity.builder()
                .name("Child1")
                .nestedChild(List.of(child11))
                .build();
        NestedEntity main = NestedEntity.builder()
                .name("Parent")
                .nestedChild(List.of(child1))
                .build();

        perNest.persist(main);
        assertNotNull(main.getId());
        assertNotNull(child1.getId());
        assertNotNull(child11.getId());
        assertNotNull(child111.getId());
        perNest.getCRUD().getAll().forEach(System.out::println);
        System.out.println(perNest.loadOptional(1).orElseThrow());

    }


}