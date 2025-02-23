package org.mikesoft.orm;


import org.mikesoft.orm.entity.AbstractEntity;
import org.mikesoft.orm.function.ThrowingConsumer;
import org.mikesoft.orm.function.ThrowingFunction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings({"LombokSetterMayBeUsed", "LombokGetterMayBeUsed"})
public class DAOImpl<T extends AbstractEntity> implements DAO<T, Integer> {
    protected static Logger log = Logger.getLogger(DAOImpl.class.getName());
    public static final int UNDEF_INT = -1;
    protected final DataSource dataSource;
    protected final EntityProfile profile;
    private boolean formattedSQLStatement = false;

    protected DAOImpl(DataSource dataSource, Class<? extends T> entityClass) {
        this.dataSource = dataSource;
        this.profile = EntityProfileFactory.createProfile(entityClass);
    }

    protected DAOImpl(DataSource dataSource, EntityProfile entityProfile) {
        this.dataSource = dataSource;
        this.profile = entityProfile;
    }

    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void setFormattedSQLStatement(boolean formattedSQLStatement) {
        this.formattedSQLStatement = formattedSQLStatement;
    }

    /**
     * Try-resource wrapper for Connection
     */
    public <R> R withConnection(final ThrowingFunction<Connection, R, SQLException> function) throws SQLException {
        try (var connection = getConnection()) {
            return function.apply(connection);
        }
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public EntityProfile getProfile() {
        return profile;
    }

    public <R> R doQuery(Connection connection, String sql,
                         final ThrowingConsumer<PreparedStatement, SQLException> paramMapper,
                         final ThrowingFunction<RSWrapper, R, SQLException> resultMapper) {
        String sqlStatement = sql;
        try (var ps = connection.prepareStatement(sql)) {
            paramMapper.accept(ps);
            sqlStatement = "\n" + formatSQLStatement(ps.toString());
            log.fine(sqlStatement);
            ResultSet rs = ps.executeQuery();
            return resultMapper.apply(new RSWrapper(rs));
        } catch (SQLException e) {
            throw new RuntimeException(sqlStatement, e);
        }
    }

    public <R> R doUpdate(Connection connection, String sql,
                          final ThrowingConsumer<PreparedStatement, SQLException> paramMapper,
                          final ThrowingFunction<RSWrapper, R, SQLException> resultMapper) throws SQLException {
        String sqlStatement = sql;
        log.fine("DAO class: " + this);
        log.fine("Built statement: " + sql);
        try (var ps = connection.prepareStatement(sql)) {
            paramMapper.accept(ps);
            sqlStatement = "\n" + formatSQLStatement(ps.toString());
            int count = ps.executeUpdate();
            RSWrapper results = new RSWrapper(ps.getGeneratedKeys(), count);
            log.fine(sqlStatement + (results.getGeneratedKeys().isEmpty() ? "" : " -> ID=" + results.getGeneratedKeys()));
            return resultMapper.apply(results);
        } catch (SQLException e) {
            log.fine(sqlStatement);
            throw new SQLException(e);
        }
    }

    public void doUpdate(Connection connection, String sql) throws SQLException {
        if (sql == null || sql.isEmpty())
            throw new IllegalArgumentException("SQL statement is empty");

        try (var ps = connection.prepareStatement(sql)) {
            String sqlStatement = "\n" + formatSQLStatement(ps.toString());
            ps.executeUpdate();
            log.fine(sqlStatement);
        }
    }

    private String formatSQLStatement(String sql) {
        if (formattedSQLStatement) return sql.replaceAll("\n \n ", "\n");
        else return sql.replaceAll("\n", " ");
    }

    @Override
    public int create(T entity) throws SQLException {
        return withConnection(connection -> doUpdate(connection, getProfile().getStatements().get("INSERT"), ps ->
                        setPreparedStatement(ps, profile.getInsertablePrimitiveColumns().toList(), entity),
                r -> {
                    r.getId().ifPresent(id -> profile.setIdValue(entity, id));
                    return r.updateCount;
                })
        );
    }

    @Override
    public int createAll(List<T> entityList) throws SQLException {
        return withConnection(connection ->
                {
                    for (T entity : entityList) {
                        doUpdate(connection, getProfile().getStatements().get("INSERT"), ps ->
                                        setPreparedStatement(ps, profile.getInsertablePrimitiveColumns().toList(), entity),
                                r -> {
                                    r.getId().ifPresent(id -> profile.setIdValue(entity, id));
                                    return r.updateCount;
                                });
                    }
                    return entityList.size();
                }
        );
    }

    @Override
    public Optional<T> read(Integer id) throws SQLException {
        return withConnection(connection -> read(connection, id));
    }

    private Optional<T> read(Connection connection, Integer id) {
        final String statement = """
                SELECT * FROM %s
                WHERE %s=?
                """.formatted(profile.getTableName(), profile.getIdColumn().getColumnName());
        return read(connection, statement, id);
    }

    protected Optional<T> read(Connection connection, String sqlStatement, Integer id) {
        return doQuery(connection, sqlStatement, ps -> setPreparedStatementValue(ps, 1, id),
                rsWrapper -> rsWrapper.stream()
                        .findFirst()
                        .map(ignore -> toEntity(rsWrapper))
        );
    }

    @Override
    public Optional<T> read(T entity) throws SQLException {
        return withConnection(connection ->
                doQuery(connection, getProfile().getStatement("READ_BY_ENTITY"),
                        ps -> {
                            int i = 1;
                            for (var value : getProfile().getCreateTableColumns()
                                    .filter(column -> !column.isId())
                                    .map(column -> column.getValue(entity)).toList()) {
                                setPreparedStatementValue(ps, i, value);
                                i++;
                            }
                        },
                        rsWrapper -> rsWrapper.stream()
                                .findFirst()
                                .map(ignore -> toEntity(rsWrapper))
                )
        );
    }

    @Override
    public Stream<T> readAll() throws SQLException {
        final String statement = """
                SELECT * FROM %s
                """.formatted(profile.getTableName());

        Connection connection = getConnection();

        RSWrapper rw;
        PreparedStatement ps;
        try {
            ps = connection.prepareStatement(statement);
            ResultSet rs = ps.executeQuery();
            rw = new RSWrapper(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED) {
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                try {
                    if (rw.getResultSet() == null) {
                        ps.close();
                        connection.close();
                        return false;
                    }
                    if (!rw.getResultSet().next()) {
                        rw.getResultSet().close();
                        ps.close();
                        connection.close();
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                action.accept(toEntity(rw));
                return true;
            }
        }, false);
    }

    @SuppressWarnings("unchecked")
    private T toEntity(RSWrapper rsWrap) {
        Object entity = profile.newEntityInstance();
        int columnIndex = 1;
        for (var column : profile.getCreateTableColumns().toList()) {
            column.setValue(entity, rsWrap.getValue(column.getTargetJavaType(), columnIndex));
            columnIndex++;
        }
        return (T) entity;
    }

    @Override
    public int update(T entity) throws SQLException {
        return withConnection(connection -> doUpdate(connection, getProfile().getStatements().get("UPDATE_BY_ID"), ps -> {
                    setPreparedStatement(ps, profile.getUpdatablePrimitiveColumns().toList(), entity);
                    setPreparedStatementValue(ps, ps.getParameterMetaData().getParameterCount(), profile.getIdValue(entity));
                },
                r -> r.updateCount));
    }

    @Override
    public int updateField(Integer id, String fieldName, Object value) throws SQLException {
        final String STATEMENT = """
                UPDATE %s
                    SET %s = ?
                WHERE id = ?
                """.formatted(profile.getTableName(), getProfile().getColumnByField(fieldName).getColumnName());
        return withConnection(connection -> doUpdate(connection, STATEMENT,
                ps -> {
                    setPreparedStatementValue(ps, 1, value);
                    setPreparedStatementValue(ps, 2, id);
                }, r -> r.updateCount));
    }

    @Override
    public int delete(Integer id) throws SQLException {
        final String DELETE = """
                DELETE FROM %s WHERE id = ?
                """.formatted(profile.getTableName());
        return withConnection(connection ->
                doUpdate(connection, DELETE, ps -> setPreparedStatementValue(ps, 1, id),
                        RSWrapper::getUpdateCount));
    }

    @Override
    public int deleteAll(String whereClause, Object... args) throws SQLException {
        final String STATEMENT = """
                DELETE FROM %s WHERE %s""".formatted(getProfile().getTableName(), whereClause);
        return withConnection(connection -> doUpdate(connection, STATEMENT, ps -> {
                    for (int i = 0; i < args.length; i++) {
                        setPreparedStatementValue(ps, i + 1, args[i]);
                    }
                },
                RSWrapper::getUpdateCount));
    }

    @Override
    public int refresh(T entity) throws SQLException {
        return withConnection(connection ->
                read(connection, (Integer) profile.getIdValue(entity))
                        .map(found -> {
                            profile.copy(found, entity);
                            return 1;
                        })
                        .orElse(0)
        );
    }

    @Override
    public Optional<T> findByUnique(String columnName, Object value) throws SQLException {
        if (getProfile().getColumn(columnName) == null)
            throw new IllegalArgumentException("No such field: " + columnName);
        String whereClause = columnName + "=?";
        List<T> result = findAll(whereClause, value);
        if (result.size() > 1)
            throw new IllegalArgumentException("The column <" + columnName + "> is not unique: found " + result.size() + " records");
        return result.stream().findFirst();
    }

    @Override
    public Optional<T> findByUnique(String[] columnNames, Object... values) throws SQLException {
        if (columnNames.length != values.length)
            throw new IllegalArgumentException("Count of the columns and values should be equal");
        String whereClause = Arrays.stream(columnNames)
                .map(column -> column + "=?")
                .collect(Collectors.joining(" AND "));
        List<T> result = findAll(whereClause, values);
        if (result.size() > 1)
            throw new IllegalArgumentException("The columns <" + String.join(",", columnNames) + "> is not unique constraint: found " + result.size() + " records");
        return result.stream().findFirst();
    }

    @Override
    public List<T> findAll(String whereClause, Object... args) throws SQLException {
        final String STATEMENT = """
                SELECT * FROM %s
                WHERE %s
                """.formatted(profile.getTableName(), whereClause);
        return withConnection((connection ->
                doQuery(connection, STATEMENT,
                        ps -> {
                            for (int i = 0; i < args.length; i++)
                                setPreparedStatementValue(ps, i + 1, args[i]);
                        },
                        rsWrapper -> rsWrapper.stream()
                                .map(ignore -> toEntity(rsWrapper))
                                .toList()
                )
        ));
    }

    private static void setPreparedStatement(PreparedStatement ps, List<EntityProfile.Column> profile, Object entity) {
        for (int i = 0; i < profile.size(); i++) {
            try {
                setPreparedStatementValue(ps, i + 1, profile.get(i).getField().get(entity));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void setPreparedStatementValue(PreparedStatement ps, int parameterIndex, Object value) {
        try {
            switch (value) {
                case null -> {
                }
                case String str -> ps.setString(parameterIndex, str);
                case Integer integer -> ps.setInt(parameterIndex, integer);
                case Double dbl -> ps.setDouble(parameterIndex, dbl);
                case Float flt -> ps.setFloat(parameterIndex, flt);
                case Boolean bln -> ps.setBoolean(parameterIndex, bln);
                default -> throw new IllegalStateException("Unexpected value type: " + value.getClass().getTypeName());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static class RSWrapper {
        private final ResultSet resultSet;
        private final int updateCount;
        private final List<Object> generatedKeys = new ArrayList<>();

        public RSWrapper(ResultSet resultSet) {
            this.resultSet = resultSet;
            this.updateCount = UNDEF_INT;
        }

        public RSWrapper(ResultSet generatedKeys, int updateCount) {
            try {
                while (generatedKeys.next()) {
                    this.generatedKeys.add(generatedKeys.getObject(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            this.updateCount = updateCount;
            this.resultSet = null;
        }

        public ResultSet getResultSet() {
            return resultSet;
        }

        public int getUpdateCount() {
            return updateCount;
        }

        public List<Object> getGeneratedKeys() {
            return generatedKeys;
        }

        public Optional<Object> getId() {
            return getGeneratedKeys().stream().findFirst();
        }

        public Stream<ResultSet> stream() {
            return utils.streamOf(resultSet);
        }

        public Object getValue(Class<?> targetTypeClass, String columnName) {
            try {
                return getValue(targetTypeClass, resultSet.findColumn(columnName));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public Object getValue(Class<?> targetTypeClass, int columnIndex) {
            try {
                return switch (targetTypeClass.getSimpleName()) {
                    case "Integer", "int" -> resultSet.getInt(columnIndex);
                    case "Long", "long" -> resultSet.getLong(columnIndex);
                    case "float" -> resultSet.getFloat(columnIndex);
                    case "Double", "double" -> resultSet.getDouble(columnIndex);
                    case "boolean", "Boolean" -> resultSet.getBoolean(columnIndex);
                    case "String" -> resultSet.getString(columnIndex);
                    default -> throw new IllegalArgumentException("Unknown data type: " + targetTypeClass);
                };
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
