package org.mikesoft.orm;


import org.mikesoft.orm.entity.AbstractEntity;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface DAO<T extends AbstractEntity, ID> {

    /**
     *
     * @return created count
     */
    int create(T entity) throws SQLException;
    int createAll(List<T> entityList) throws SQLException;

    Optional<T> read(ID id) throws SQLException;
    Optional<T> read(T entity) throws SQLException;
    Stream<T> readAll() throws SQLException;
    /**
     *
     * @return updated count
     */
    int update(T entity) throws SQLException;
    int updateField(ID id, String fieldName, Object value) throws SQLException;

    /**
     *
     * @return deleted count
     */
    int delete(ID id) throws SQLException;
    int deleteAll(String whereClause, Object...args) throws SQLException;
    int refresh(T entity) throws SQLException;
    Optional<T> findByUnique(String columnName, Object value) throws SQLException;
    Optional<T> findByUnique(String[] columnNames, Object... values) throws SQLException;
    List<T> findAll(String whereClause, Object...args) throws SQLException;

    //common methods
    DataSource getDataSource();
    EntityProfile getProfile();
}
