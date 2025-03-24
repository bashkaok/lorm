package com.jisj.orm;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import com.jisj.orm.entity.JoinTableEntityIntID;
import com.jisj.orm.repository.CRUDRepositoryImpl;
import com.jisj.orm.repository.OrmRepoContainer;
import com.jisj.orm.repository.PersistRepository;
import com.jisj.orm.repository.RepositoryFactory;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


@SuppressWarnings("LombokSetterMayBeUsed")
@Log
public class DBEnvironment {
    @Getter
    private static DBEnvironment instance = null;
    @Getter
    private final SQLiteConnectionPoolDataSource dataSource;
    @Getter
    private StartMode startMode = StartMode.CREATE_IF_NOT_EXISTS;
    @Getter
    private String currentPath = "target/test-data/testdb.sqlite";
    @Setter
    private boolean formattedSQLStatement = false;
    @Getter
    private final OrmRepoContainer global = new OrmRepoContainer();
    @Getter
    private final Map<Class<?>, PersistRepository<?, ?>> persistRepositoryMap = new HashMap<>();
    private final Map<Class<?>, Consumer<OrmRepoContainer>> onCreateActions = new HashMap<>();

    public DBEnvironment() {
        if (instance != null)
            throw new IllegalStateException("DBEnvironment instance already exists. Use getInstance()");
        dataSource = new SQLiteConnectionPoolDataSource();
        org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
        config.enableCaseSensitiveLike(false);
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

    public void setStartMode(StartMode startMode) {
        this.startMode = startMode;
    }

    /**
     * Sets an action that will be performed after table creating, for example: to fill initial data
     * @param entityClass table entity class
     * @param action action method or lambda
     */
    public void setOnCreateActions(Class<?> entityClass, Consumer<OrmRepoContainer> action) {
        onCreateActions.put(entityClass, action);
    }

    /**
     * Creates entities DAO and repositories
     * @param entities list of entity classes
     */
    public void initializeEntities(List<Class<?>> entities) {
        entities.forEach(clazz -> global.add(DAOFactory.createDAO(getDataSource(), clazz)));
        initializeJoinTables();
        initEnvironment();
    }

    /**
     * Creates entities DAO and repositories
     * @param entities list of entity classes
     */
    public final void initializeEntities(Class<?>... entities) {
        initializeEntities(Arrays.stream(entities).toList());
    }

    private void onCreateTableAction(Class<?> entityClass) {
        Consumer<OrmRepoContainer> action = onCreateActions.get(entityClass);
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
            if (startMode != StartMode.OPEN) {
                if (!DBManager.tableExists((DAOImpl<?,?>) dao)) {
                    DBManager.createTableIfNotExists(dao);
                    onCreateTableAction(dao.getProfile().entityClass);
                }
            }
        });
    }

    @Getter
    public enum StandardConnection {
        /**
         * DB will be created in memory
         */
        MEMORY("jdbc:sqlite::memory:"),
        /**
         * DB will be created in memory common to all connections
         */
        MEMORY_CACHE("jdbc:sqlite:memory:?cache=shared"),
        FILE_MEMORY_MODE("jdbc:sqlite:file:?mode=memory&cache=shared"),
        /**
         * Connection string sets at {@link #setCurrentPath(String)}}
         */
        FILE_CURRENT_PATH("jdbc:sqlite:file:");

        private final String url;

        StandardConnection(String url) {
            this.url = url;
        }
    }

    /**
     * Database start modes
     */
    public enum StartMode {
        /**
         * Creates tables if not exist
         */
        CREATE_IF_NOT_EXISTS,
        /**
         * Drops all tables and create again
         */
        DROP_AND_CREATE,
        /**
         * Just opens DB
         */
        OPEN
    }
}
