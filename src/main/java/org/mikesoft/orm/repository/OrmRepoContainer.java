package org.mikesoft.orm.repository;


import org.mikesoft.orm.DAO;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrmRepoContainer {
    private final Map<Class<?>, DAO<?, ?>> daoClassMap = new HashMap<>();
    private final Map<String, DAO<?, ?>> daoStringMap = new HashMap<>();
    private final Map<Class<?>, CRUDRepository<?, ?>> crudRepositoryClassMap = new HashMap<>();
    private final Map<String, CRUDRepository<?, ?>> crudRepositoryStringMap = new HashMap<>();
    private final Map<Class<?>, PersistRepository<?, ?>> persistRepositoryMap = new HashMap<>();

    public void add(DAO<?,?> dao) {
        assertArguments(dao);
        daoClassMap.put(dao.getProfile().getEntityClass(), dao);
        daoStringMap.put(dao.getProfile().getTableName(), dao);
    }

    public DAO<?,?> getDao(Class<?> entityClass) {
        return daoClassMap.get(entityClass);
    }

    public DAO<?,?> getDao(String tableName) {
        return daoStringMap.get(tableName);
    }


    public Set<DAO<?,?>> getDaoSet() {
        Set<DAO<?,?>> result = new HashSet<>(daoClassMap.values());
        result.addAll(daoStringMap.values());
        return result;
    }

    public void add(CRUDRepository<?,?> crud) {
        crudRepositoryClassMap.put(((CRUDRepositoryImpl<?,?>)crud).getDao().getProfile().getEntityClass(), crud);
        crudRepositoryStringMap.put(((CRUDRepositoryImpl<?,?>)crud).getDao().getProfile().getTableName(), crud);
    }

    public void add(PersistRepository<?,?> persist) {
        persistRepositoryMap.put(((PersistRepositoryImpl<?,?>)persist).getDao().getProfile().getEntityClass(), persist);
    }


    public CRUDRepository<?,?> getCrudRepository(Class<?> entityClass) {
        return crudRepositoryClassMap.get(entityClass);
    }

    public CRUDRepository<?,?> getCrudRepository(String tableName) {
        return crudRepositoryStringMap.get(tableName);
    }

    public PersistRepository<?,?> getPersistRepository(Class<?> entityClass) {
        return persistRepositoryMap.get(entityClass);
    }

    private void assertArguments(Object object) {
        if (object == null)
            throw new IllegalArgumentException("Repository element is null");
    }
}
