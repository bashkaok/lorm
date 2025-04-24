package com.jisj.orm;


import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface DAO<T, ID> {

    /**
     *
     * @return created count
     */
    int create(T entity) throws SQLException;
    int createAll(List<T> entityList) throws SQLException;

    Optional<T> read(ID id) throws SQLException;
    Optional<T> readByEntity(T entity) throws SQLException;
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
    @Deprecated
    int refresh(T entity) throws SQLException;
    Optional<T> findByUnique(String columnName, Object value) throws SQLException;
    Optional<T> findByUnique(String[] columnNames, Object... values) throws SQLException;
    List<T> findAll(String whereClause, Object...args) throws SQLException;

    /**
     * Performs random select SQL query to DB. In SELECT clause should be pointed all fields of the queried entity
     * @param sqlQuery plane SQL with params
     * @param args params list
     * @return Entity list
     * @throws SQLException SQL errors
     * @throws IllegalStateException when wrong SQL, wrong parameters number
     */
    List<T> query(String sqlQuery, Object...args) throws SQLException;

    //common methods
    DataSource getDataSource();
    EntityProfile getProfile();
}
