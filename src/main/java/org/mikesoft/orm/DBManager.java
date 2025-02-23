package org.mikesoft.orm;

import lombok.extern.java.Log;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.mikesoft.orm.utils.sqlExWrap;
import static org.mikesoft.orm.utils.streamOf;

@Log
public class DBManager {
    public static boolean createTableIfNotExists(DAO<?, ?> dao) {
        DAOImpl<?> baseDao = (DAOImpl<?>) dao;
        String createStatement = buildStatement(dao.getProfile(), StatementType.CREATE, true);
        try {
            log.info("Create table: " + dao.getProfile().getTableName());
            return baseDao.withConnection(connection -> {
                log.fine(createStatement);
                baseDao.doUpdate(connection, createStatement);
                return tableExists(connection, dao.getProfile().getTableName());
            });
        } catch (SQLException e) {
            log.warning("Create table error: " + dao.getProfile().getTableName() + "\n" + createStatement);
            throw new RuntimeException(e);
        }
    }

    public static boolean dropTableIfExists(DAO<?, ?> dao) {
        DAOImpl<?> baseDao = (DAOImpl<?>) dao;
        String dropStatement = buildStatement(dao.getProfile(), StatementType.DROP, true);
        try {
            log.info("Drop table: " + dao.getProfile().getTableName());
            return baseDao.withConnection(connection -> {
                baseDao.doUpdate(connection, dropStatement, ps -> {
                }, DAOImpl.RSWrapper::getGeneratedKeys);
                return !tableExists(connection, dao.getProfile().getTableName());
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static DatabaseMetaData getMetaData(Connection connection) throws SQLException {
        return connection.getMetaData();
    }

    public static Stream<ResultSet> getTables(Connection connection) throws SQLException {
        return streamOf(getMetaData(connection).getTables("", "", "", null));
    }

    public static boolean tableExists(Connection connection, String tableName) throws SQLException {
        return getTables(connection)
                .anyMatch(table -> sqlExWrap(() -> table.getString(3).equalsIgnoreCase(tableName)));
    }

    /**
     * Checks equals of the creation statement from database and generated creation statement from the entity
     */
    public static boolean tableEquals(DAO<?,?> dao) {
        String fromDB;
        try {
            fromDB = ((DAOImpl<?>)dao)
                    .withConnection(connection -> getCreateStatement(connection,dao.getProfile().getTableName()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String generated = StatementBuilder.buildCreateStatement(dao.getProfile(), false);
        log.fine("\nFrom DB:\n" + fromDB + "\nGenerated:\n" + generated);
        return fromDB.equalsIgnoreCase(generated);

    }

    /**
     * Gets CREATE statement from DB
     */
    public static String getCreateStatement(Connection connection, String tableName) throws SQLException {
        try (var st = connection.createStatement()) {
            return streamOf(st.executeQuery("SELECT sql FROM sqlite_schema WHERE name='%s'".formatted(tableName)))
                    .findAny().map(rs -> sqlExWrap(() -> rs.getString(1))).orElse("");
        }
    }

    public static String buildStatement(EntityProfile profile, StatementType type, boolean ifNotExists) {
        return switch (type) {
            case CREATE -> StatementBuilder.buildCreateStatement(profile, ifNotExists);
            case DROP -> "DROP TABLE IF EXISTS %s".formatted(profile.getTableName());
        };
    }

    public static boolean tableExists(DAOImpl<?> dao) {
        try {
            return dao.withConnection(connection -> tableExists(connection, dao.getProfile().getTableName()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public enum StatementType {
        CREATE, DROP
    }
}
