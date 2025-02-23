package org.mikesoft.orm.repository;

import org.dazlib.database.entity.PackageEntity;
import org.dazlib.database.orm.DAOException;
import org.dazlib.database.orm.DAOFactory;
import org.dazlib.database.orm.DBManager;
import org.dazlib.database.orm.MainTestEntity;
import org.junit.jupiter.api.*;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.LogManager;

import static org.dazlib.config.Prefs.LOG_PROPERTIES_NAME;
import static org.junit.jupiter.api.Assertions.*;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class CRUDRepositoryImplTest {
    private static CRUDRepositoryImpl<PackageEntity, Integer> repo;
    private static CRUDRepositoryImpl<MainTestEntity, Integer> crud;

    @BeforeAll
    public static void setUp() throws IOException {
        InputStream ins = CRUDRepositoryImplTest.class.getClassLoader().getResourceAsStream(LOG_PROPERTIES_NAME.defValue);
        LogManager.getLogManager().readConfiguration(ins);

        SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
        dataSource.setUrl("jdbc:sqlite:memory:&cache=shared");
        var packageDAO = DAOFactory.createDAO(dataSource, PackageEntity.class);
        repo = (CRUDRepositoryImpl<PackageEntity, Integer>) new CRUDRepositoryImpl<>(packageDAO);
        var entityDAO = DAOFactory.createDAO(dataSource, MainTestEntity.class);
        crud = (CRUDRepositoryImpl<MainTestEntity, Integer>) new CRUDRepositoryImpl<>(entityDAO);
        assertNotNull(packageDAO);
        DBManager.dropTableIfExists(packageDAO);
        DBManager.createTableIfNotExists(packageDAO);
        DBManager.dropTableIfExists(entityDAO);
        DBManager.createTableIfNotExists(entityDAO);

    }


    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    class PackageEntities {
        static PackageEntity expected = PackageEntity.builder()
                .fileName("FileName")
                .filePath("FilePath")
                .installed(true)
                .productId(123456)
                .build();

        @Test
        @Order(1)
        public void addPackage() throws DAOException {
            repo.add(expected);
            assertNotEquals(-1, expected.getId());
            assertEquals("FileName", expected.getFileName());
            assertEquals("FilePath", expected.getFilePath());
            assertEquals(true, expected.isInstalled());
            assertEquals(123456, expected.getProductId());

            assertEquals(DAOException.ErrorCode.RECORD_EXISTS,
                    assertThrowsExactly(DAOException.class, () -> repo.add(expected)).getErrorCode());
        }

        @Test
        @Order(2)
        public void updatePackage() throws DAOException {
            PackageEntity entity1 = PackageEntity.builder()
                    .fileName("FileName1_update")
                    .filePath("FilePath_path1")
                    .installed(false)
                    .productId(100)
                    .build();

            PackageEntity entity2 = PackageEntity.builder()
                    .fileName("FileName2_update")
                    .filePath("FilePath_path2")
                    .installed(true)
                    .productId(200)
                    .build();

            repo.add(entity1);
            assertNotEquals(-1, entity1.getId());
            repo.add(entity2);
            assertNotEquals(-1, entity2.getId());

            entity1.setFileName("FileName1_updated");
            entity1.setFilePath("FilePath_updated");
            entity1.setInstalled(true);
            entity1.setProductId(150);
            repo.update(entity1);
            assertEquals(entity1, repo.get(entity1.getId()).orElseThrow());

            var entity1persist = repo.get(entity1.getId()).orElseThrow();
            entity1.setFileName("FileName2_update");
            assertEquals(DAOException.ErrorCode.RECORD_EXISTS,
                    assertThrowsExactly(DAOException.class, () -> repo.update(entity1)).getErrorCode());
            assertEquals(entity1persist, repo.get(entity1.getId()).orElseThrow());
        }

        @Test
        @Order(3)
        public void getPackage() throws DAOException {
            assertEquals(expected, repo.get(expected.getId()).orElseThrow());
        }

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
    }


    @Test
    void persist() throws DAOException {
        MainTestEntity entity = MainTestEntity.builder()
                .id(456)
                .stringUniqueField("Unique1")
                .stringField("Field1")
                .stringDefaultField("DEFAULT")
                .doubleField(123.123)
                .build();
        crud.persist(entity);
        assertEquals(entity, crud.get(456).orElseThrow());

        //find by unique stringUniqueField
        entity.setId(null);
        crud.persist(entity);
        assertEquals(entity, crud.get(456).orElseThrow());

        //find by constraint stringField:stringDefaultField
        MainTestEntity entity1 = MainTestEntity.builder()
                .stringUniqueField(null)
                .stringField("Field1")
                .stringDefaultField("DEFAULT")
                .build();
        crud.persist(entity1);

        entity1.setDoubleField(null);
        assertNotEquals(entity, entity1);
        crud.persist(entity1);
        assertEquals(entity, entity1);



    }
}