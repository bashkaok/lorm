package org.mikesoft.orm.repository;

import org.junit.jupiter.api.*;
import org.mikesoft.orm.*;
import org.mikesoft.orm.testdata.EmbeddedEntity;
import org.mikesoft.orm.testdata.MainEntity;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

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
        InputStream ins = CRUDRepositoryImplTest.class.getClassLoader().getResourceAsStream("log.properties");
        LogManager.getLogManager().readConfiguration(ins);

        SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
        dataSource.setUrl("jdbc:sqlite:memory:&cache=shared");
        db = new DBEnvironment(DBEnvironment.StandardConnection.MEMORY_CACHE);
//        db = new DBEnvironment(DBEnvironment.StandardConnection.FILE_CURRENT_PATH);
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

            crud.addAll(List.of(e1,e2));
            assertTrue(e1.getId() > 0);
            assertTrue(e2.getId() > 0);
            assertEquals(DAOException.ErrorCode.RECORD_EXISTS,
                    assertThrowsExactly(DAOException.class, () -> crud.addAll(List.of(e1,e2))).getErrorCode());
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
            assertEquals(e1.getId(),e2.getId());
            assertNotEquals(e1,e2);
        }

        @Test
        void merge() throws DAOException {
            MainEntity entity = MainEntity.builder()
                    .stringField("Field_merge")
                    .stringUniqueField("mergeTest")
                    .stringDefaultField("DEFAULT-FIELD")
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

/*

        @Test
        @Order(4)
        public void refreshPackage() throws DAOException {
            PackageEntity refresh = PackageEntity.builder().id(expected.getId()).build();
            repo.refresh(refresh);
            assertEquals(expected, refresh);
        }

        @Test
        @Order(5)
        public void findByUnique() {
            assertEquals(expected, repo.findByUnique("fileName", "FileName").orElseThrow());
            assertEquals(Optional.empty(), repo.findByUnique("fileName", "FileNameNoSuchValue"));
            assertEquals("No such field: fileNameNoSuchField",
                    assertThrowsExactly(IllegalArgumentException.class,
                            ()-> repo.findByUnique("fileNameNoSuchField", "FileName")).getMessage());
        }

        @Test
        @Order(6)
        public void delete() throws DAOException {
            int id = expected.getId();
            repo.delete(id);
            assertEquals(Optional.empty(), repo.get(id));
        }
*/
    }
/*
    @Test
    void merge() throws DAOException {

        //find by unique stringUniqueField
        entity.setId(null);
        crud.merge(entity);
        assertEquals(entity, crud.get(456).orElseThrow());

        //find by constraint stringField:stringDefaultField
        MainEntity entity1 = MainEntity.builder()
                .stringUniqueField(null)
                .stringField("Field1")
                .stringDefaultField("DEFAULT")
                .build();
        crud.merge(entity1);

        entity1.setDoubleField(null);
        assertNotEquals(entity, entity1);
        crud.merge(entity1);
        assertEquals(entity, entity1);



    }

 */
}
