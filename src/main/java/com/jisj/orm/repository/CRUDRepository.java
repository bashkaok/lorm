package com.jisj.orm.repository;


import com.jisj.orm.DAOException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface CRUDRepository<T, ID> extends Repository<T, ID> {
    void add(T entity) throws DAOException;

    void addAll(List<T> entity) throws DAOException;

    Optional<T> get(ID id);

    Optional<T> getByEntity(T entity);

    Stream<T> getAll();

    void update(T entity) throws DAOException;

    void addOrUpdate(T entity) throws DAOException;

    /**
     * Updates the store record by not-null fields from entity. The specified entity has priority if field is not-null.
     * Specified entity will be filled by merged record
     * If specified entity id is null the searching will be done by unique fields
     */
    void merge(T entity);

    void delete(ID id) throws DAOException;

    void deleteAll(String whereClause, Object... args);

    void refresh(T entity) throws DAOException;

    Optional<T> findByUnique(String columnName, Object value);

    Optional<T> findByUnique(String[] columnNames, Object... values);

    List<T> findAll(String whereClause, Object... args);

    /**
     * Performs random select SQL query to DB. In SELECT clause should be pointed all fields of the queried entity
     *
     * @param sqlQuery plane SQL with params
     * @param args     params list
     * @return List of Entity
     * @throws IllegalArgumentException when wrong SQL, wrong parameters number
     */
    List<T> query(String sqlQuery, Object... args);

}
