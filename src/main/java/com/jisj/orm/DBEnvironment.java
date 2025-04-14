package com.jisj.orm;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import com.jisj.orm.entity.JoinTableEntityIntID;
import com.jisj.orm.repository.CRUDRepositoryImpl;
import com.jisj.orm.repository.OrmRepoContainer;
import com.jisj.orm.repository.PersistRepository;
import com.jisj.orm.repository.RepositoryFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings({"LombokSetterMayBeUsed", "LombokGetterMayBeUsed"})
@Log
public class DBEnvironment {
    private static DBEnvironment instance = null;
    private final DBDataSource dataSource;
    @Getter
    private StartMode startMode = StartMode.CREATE_IF_NOT_EXISTS;
    @Setter
    private boolean formattedSQLStatement = false;
    @Getter
    private final OrmRepoContainer global = new OrmRepoContainer();
    @Getter
    private final Map<Class<?>, PersistRepository<?, ?>> persistRepositoryMap = new HashMap<>();
    private final Map<Class<?>, Consumer<OrmRepoContainer>> onCreateActions = new HashMap<>();
    private final Map<Class<?>, Consumer<OrmRepoContainer>> onIntegrityCheckActions = new HashMap<>();


    private DBEnvironment(DBDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static DBEnvironment getInstance(DBDataSource dataSource) {
        if (instance==null) {
            instance = new DBEnvironment(dataSource);
            return instance;
        }
        log.warning("Object already instanced: " + dataSource.getUrl());
        return instance;
    }

    public static DBEnvironment getInstance() {
        if (instance == null)
            throw new IllegalStateException("DBEnvironment not instanced. Use getInstance(DBDatasource)");
        return instance;
    }

    public void close() {
        log.info("DB instance was closed: " + dataSource.getUrl());
        instance = null;
    }

    public DBDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets start mode for the database
     * @param startMode value of {@link StartMode} Default: {@link StartMode#CREATE_IF_NOT_EXISTS CREATE_IF_NOT_EXISTS}
     */
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
     * Sets an action that will be performed after everything entities initialization.
     * Setter should be applied BEFORE {@code initializeEntities()} call
     * @param entityClass table entity class
     * @param action action method
     */
    public void setOnIntegrityCheckActions(Class<?> entityClass, Consumer<OrmRepoContainer> action) {
        onIntegrityCheckActions.put(entityClass, action);
    }

    /**
     * Creates entities DAO and repositories
     * @param entities variants of entity classes
     * @see #initializeEntities(List)
     */
    public final void initializeEntities(Class<?>... entities) {
        initializeEntities(Arrays.stream(entities).toList());
    }

    /**
     * Creates entities DAO and repositories
     * @param entities list of entity classes
     * @see #initializeEntities(Class[])
     */
    public void initializeEntities(List<Class<?>> entities) {
        entities.forEach(clazz -> global.add(DAOFactory.createDAO(dataSource, clazz)));
        initializeJoinTables();
        initEnvironment();
        performIntegrityCheck();
    }

    private void initializeJoinTables() {
        global.getDaoSet()
                .forEach(dao -> dao.getProfile().getManyToManyColumns().forEach(column -> {
                    var joinDAO = global.getDao(column.getTargetJavaType());
                    if (joinDAO == null)
                        throw new IllegalArgumentException("DAO not found for joined " + column.getTargetJavaType());
                    //TODO Refactoring for exclude second pass
                    column.join(joinDAO.getProfile());

                    DAO<?, ?> joinTableDao = DAOFactory.createDAO(dataSource, column.getJoinTableProfile());
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

    private void onCreateTableAction(Class<?> entityClass) {
        Consumer<OrmRepoContainer> action = onCreateActions.get(entityClass);
        if (action!=null) {
            action.accept(global);
        }
    }

    private void performIntegrityCheck() {
        onIntegrityCheckActions.values()
                .forEach(action-> action.accept(global));
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
