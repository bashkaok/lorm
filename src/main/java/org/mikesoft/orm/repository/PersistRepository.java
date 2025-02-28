package org.mikesoft.orm.repository;


import org.mikesoft.orm.DAOException;

import java.util.Optional;

public interface PersistRepository<T,ID> extends Repository<T,ID> {
    /**
     * Saves entity and everything embedded entities
     */
    void save(T entity) throws DAOException;
    void saveOrUpdate(T entity);
    Optional<T> load(ID id);
    void update(T entity) throws DAOException;
    void refresh(T entity);
    void persist(T entity);
}
