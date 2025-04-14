package com.jisj.orm;

import com.jisj.orm.repository.CRUDRepository;
import com.jisj.orm.testdata.NestedEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DBEnvironmentTest {

    @BeforeAll
    static void setUp() throws IOException {
        InputStream ins = DBEnvironmentTest.class.getClassLoader().getResourceAsStream("log-test.properties");
        LogManager.getLogManager().readConfiguration(ins);

    }

    @SuppressWarnings("unchecked")
    @Test
    void setOnIntegrityCheckActions() {
        DBEnvironment db = DBEnvironment.getInstance(DBDataSource.newDataSource(DBDataSource.StandardConnection.MEMORY_CACHE));
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(Level.FINE);
        db.setStartMode(DBEnvironment.StartMode.DROP_AND_CREATE);
        db.setOnIntegrityCheckActions(NestedEntity.class, NestedEntity::checkIntegrity);
        db.initializeEntities(NestedEntity.class);
        CRUDRepository<NestedEntity, Integer> crud = (CRUDRepository<NestedEntity, Integer>) db.getGlobal().getCrudRepository(NestedEntity.class);
        List<NestedEntity> result = crud.getAll().toList();
        assertEquals(1, result.size());
        assertEquals("System record", result.getFirst().getName());
        db.close();
    }
}