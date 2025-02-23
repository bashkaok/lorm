package org.mikesoft.orm.repository;


import org.mikesoft.orm.DAOException;
import org.mikesoft.orm.entity.AbstractEntity;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface CRUDRepository<T extends AbstractEntity, ID> extends Repository<T,ID> {
    void add(T entity) throws DAOException;
    void addAll(List<T> entity)  throws DAOException;
    Optional<T> get(ID id) throws DAOException;
    Optional<T> get(T entity);
    Stream<T> getAll();
    void update(T entity) throws DAOException;
    void addOrUpdate(T entity);
    void persist(T entity);
    void delete(ID id) throws DAOException;
    void deleteAll(String whereClause, Object...args);
    void refresh(T entity) throws DAOException;
    Optional<T> findByUnique(String columnName, Object value);
    Optional<T> findByUnique(String[] columnNames, Object... values);
    List<T> findAll(String whereClause, Object...args);
}
