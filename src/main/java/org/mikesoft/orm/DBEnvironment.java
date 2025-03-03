package org.mikesoft.orm;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.mikesoft.orm.entity.JoinTableEntityIntID;
import org.mikesoft.orm.repository.CRUDRepositoryImpl;
import org.mikesoft.orm.repository.OrmRepoContainer;
import org.mikesoft.orm.repository.PersistRepository;
import org.mikesoft.orm.repository.RepositoryFactory;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


@Getter
@Setter
@Log
public class DBEnvironment {
    @Getter
    private static DBEnvironment instance = null;
    private final SQLiteConnectionPoolDataSource dataSource;

    private StartMode startMode = StartMode.CREATE_IF_NOT_EXISTS;
    private String currentPath = "target/test-data/testdb.sqlite";
    @Setter
    private boolean formattedSQLStatement = false;
    private final OrmRepoContainer global = new OrmRepoContainer();
    private final Map<Class<?>, PersistRepository<?, ?>> persistRepositoryMap = new HashMap<>();
    private final Map<Class<?>, Consumer<OrmRepoContainer>> onCreateAction = new HashMap<>();

    public DBEnvironment() {
        if (instance != null)
            throw new IllegalStateException("DBEnvironment instance already exists. Use getInstance()");
        dataSource = new SQLiteConnectionPoolDataSource();
        org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
        config.enableCaseSensitiveLike(true);
        config.enforceForeignKeys(true);
        dataSource.setConfig(config);
        instance = this;
    }

    public DBEnvironment(StandardConnection connection) {
        this();
        setConnection(connection);
    }

    public void close() {
        log.info("DB instance was closed: " + getInstance().getDataSource().getUrl());
        instance = null;
    }

    public void setConnection(String url) {
        dataSource.setUrl(url);
        log.info("Connection with: " + dataSource.getUrl());
    }

    public void setConnection(StandardConnection connection) {
        setConnection(connection == StandardConnection.FILE_CURRENT_PATH ? connection.getUrl() + currentPath : connection.getUrl());
    }

    public void setCurrentPath(String path) {
        try {
            Files.createDirectories(Path.of(path).getParent());
            currentPath = path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeEntities(List<Class<?>> entities) {
        entities.forEach(clazz -> global.add(DAOFactory.createDAO(getDataSource(), clazz)));
        initializeJoinTables();
        initEnvironment();
    }

    public final void initializeEntities(Class<?>... entities) {
        initializeEntities(Arrays.stream(entities).toList());
    }

    private void onCreateTableAction(Class<?> entityClass) {
        Consumer<OrmRepoContainer> action = onCreateAction.get(entityClass);
        if (action!=null) {
            action.accept(global);
        }
    }

    private void initializeJoinTables() {
        global.getDaoSet()
                .forEach(dao -> dao.getProfile().getManyToManyColumns().forEach(column -> {
                    var joinDAO = global.getDao(column.getTargetJavaType());
                    if (joinDAO == null)
                        throw new IllegalArgumentException("DAO not found for joined " + column.getTargetJavaType());
                    //TODO Refactoring for exclude second pass
                    column.join(joinDAO.getProfile());

                    DAO<?, ?> joinTableDao = DAOFactory.createDAO(getDataSource(), column.getJoinTableProfile());
                    global.add(joinTableDao);
                }));
    }

    private void initEnvironment() {
        global.getDaoSet().forEach(dao -> {
            ((DAOImpl<?,?>)dao).setFormattedSQLStatement(formattedSQLStatement);
            CRUDRepositoryImpl<?, ?> crud = (CRUDRepositoryImpl<?, ?>) RepositoryFactory.createCRUDRepository(dao);
            crud.setGlobal(global);
            global.add(crud);
            if (dao.getProfile().getEntityClass() != JoinTableEntityIntID.class)
                global.add(RepositoryFactory.createPersistRepository(crud));

            if (startMode == StartMode.DROP_AND_CREATE) DBManager.dropTableIfExists(dao);
            if (startMode != StartMode.AS_IT_IS) {
                if (!DBManager.tableExists((DAOImpl<?,?>) dao)) {
                    DBManager.createTableIfNotExists(dao);
                    onCreateTableAction(dao.getProfile().entityClass);
                }
            }
        });
    }

    @Getter
    public enum StandardConnection {
        MEMORY("jdbc:sqlite::memory:"),
        MEMORY_CACHE("jdbc:sqlite:memory:?cache=shared"),
        FILE_MEMORY_MODE("jdbc:sqlite:file:?mode=memory&cache=shared"),
        FILE_CURRENT_PATH("jdbc:sqlite:file:");

        private final String url;

        StandardConnection(String url) {
            this.url = url;
        }
    }

    public enum StartMode {
        CREATE_IF_NOT_EXISTS,
        DROP_AND_CREATE,
        AS_IT_IS
    }
}
