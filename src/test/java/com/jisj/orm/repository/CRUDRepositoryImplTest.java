package com.jisj.orm.repository;

import com.jisj.orm.DAOException;
import com.jisj.orm.DBDataSource;
import com.jisj.orm.DBEnvironment;
import org.junit.jupiter.api.*;
import com.jisj.orm.testdata.EmbeddedEntity;
import com.jisj.orm.testdata.MainEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class CRUDRepositoryImplTest {
    private static DBEnvironment db;
    private static CRUDRepositoryImpl<MainEntity, Integer> crud;

    @SuppressWarnings("unchecked")
    @BeforeAll
    public static void setUp() throws IOException {
        InputStream ins = CRUDRepositoryImplTest.class.getClassLoader().getResourceAsStream("log-test.properties");
        LogManager.getLogManager().readConfiguration(ins);

        db = DBEnvironment.getInstance(DBDataSource.newPooledDataSource(DBDataSource.StandardConnection.MEMORY_CACHE));
        db.setStartMode(DBEnvironment.StartMode.DROP_AND_CREATE);
        db.initializeEntities(MainEntity.class, EmbeddedEntity.class);
        crud = (CRUDRepositoryImpl<MainEntity, Integer>) db.getGlobal().getCrudRepository(MainEntity.class);
        assertNotNull(crud);
    }

    @AfterAll
    static void close() {
        db.close();
    }


    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    class PackageEntities {
        static MainEntity expected = MainEntity.builder()
                .stringField("stringColumn")
                .stringUniqueField("Unique")
                .stringDefaultField("DEFAULT")
                .doubleField(123.123)
                .floatField(345.345F)
                .booleanField(true)
                .build();

        @Test
        @Order(1)
        void add() throws DAOException {
            crud.add(expected);
            assertTrue(expected.getId() > 0);
            assertEquals("stringColumn", expected.getStringField());
            assertEquals("Unique", expected.getStringUniqueField());
            assertEquals("DEFAULT", expected.getStringDefaultField());
            assertEquals(123.123, expected.getDoubleField());
            assertEquals(345.345F, expected.getFloatField());
            assertTrue(expected.isBooleanField());

            assertEquals(DAOException.ErrorCode.RECORD_EXISTS,
                    assertThrowsExactly(DAOException.class, () -> crud.add(expected)).getErrorCode());
        }

        @Test
        @Order(2)
        void addAll() throws DAOException {
            MainEntity e1 = MainEntity.builder()
                    .stringUniqueField("addAll1")
                    .build();
            MainEntity e2 = MainEntity.builder()
                    .stringUniqueField("addAll2")
                    .build();

            crud.addAll(List.of(e1, e2));
            assertTrue(e1.getId() > 0);
            assertTrue(e2.getId() > 0);
            assertEquals(DAOException.ErrorCode.RECORD_EXISTS,
                    assertThrowsExactly(DAOException.class, () -> crud.addAll(List.of(e1, e2))).getErrorCode());
        }

        @Test
        @Order(3)
        void get_by_ID() {
            assertEquals(expected, crud.get(expected.getId()).orElseThrow());
        }

        @Test
        @Order(4)
        void get_by_entity() {
            assertEquals(expected, crud.getByEntity(expected).orElse(null));
        }

        @Test
        @Order(5)
        void getAll() {
            assertEquals(3, crud.getAll().count());
        }


        @Test
        @Order(6)
        public void update() throws DAOException {
            MainEntity e1 = MainEntity.builder()
                    .stringField("stringColumn6")
                    .stringUniqueField("UpdateTest")
                    .stringDefaultField("DEFAULT")
                    .doubleField(123.123)
                    .floatField(345.345F)
                    .booleanField(true)
                    .build();
            crud.add(e1);
            assertTrue(e1.getId() > 0);

            e1.setStringDefaultField("default");
            e1.setUnAnnotatedField("unAnnotated");
            crud.update(e1);

            assertEquals(e1, crud.get(e1.getId()).orElseThrow());
        }

        @Test
        @Order(7)
        public void addOrUpdate() throws DAOException {
            MainEntity e1 = MainEntity.builder()
                    .stringField("stringColumn7")
                    .stringUniqueField("addOrUpdateTest")
                    .stringDefaultField("DEFAULT")
                    .doubleField(123.123)
                    .floatField(345.345F)
                    .booleanField(true)
                    .build();
            crud.addOrUpdate(e1);
            assertTrue(e1.getId() > 0);

            e1.setStringDefaultField("default");
            e1.setUnAnnotatedField("unAnnotated");
            crud.addOrUpdate(e1);
            assertEquals(e1, crud.get(e1.getId()).orElseThrow());

            MainEntity e2 = MainEntity.builder()
                    .id(e1.getId())
                    .stringField("stringColumn7")
                    .stringUniqueField("addOrUpdateTest")
                    .stringDefaultField("DEFAULT")
                    .doubleField(123.123)
                    .floatField(345.345F)
                    .booleanField(true)
                    .build();
            crud.addOrUpdate(e2);
            assertEquals(e1.getId(), e2.getId());
            assertNotEquals(e1, e2);
        }

        @Test
        void merge() throws DAOException {
            MainEntity entity = MainEntity.builder()
                    .stringField("Field_merge")
                    .stringUniqueField("mergeTest")
                    .stringDefaultField("default")
                    .doubleField(123.123)
                    .build();
            crud.add(entity);
            assertTrue(entity.getId() > 0);

            MainEntity merge = MainEntity.builder()
                    .id(entity.getId())
                    .unAnnotatedField("UnAnnotated")
                    .build();
            crud.merge(merge);
            assertEquals(merge.getUnAnnotatedField(), crud.get(entity.getId()).orElseThrow().getUnAnnotatedField());
            assertEquals(entity.getStringDefaultField(), crud.get(entity.getId()).orElseThrow().getStringDefaultField()); //TODO incorrect default field from builder

        }
    }

    @Test
    void query(){
        final String sql_no_DB = """
                SELECT * FROM MainTable_not_exist
                WHERE stringDefaultColumn LIKE ?
                """;
        assertThrowsExactly(IllegalStateException.class, ()-> crud.query(sql_no_DB));

        final String sql_wrong_params = """
                SELECT * FROM MainTable
                WHERE stringDefaultColumn LIKE ?
                """;
        assertThrowsExactly(IllegalStateException.class, ()-> crud.query(sql_wrong_params));

        final String sql = """
                SELECT * FROM MainTable
                WHERE stringDefaultColumn LIKE ?
                """;
        assertEquals(0, crud.query(sql, "dff").size());
    }

}
