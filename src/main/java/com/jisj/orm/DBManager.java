package com.jisj.orm;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.jisj.orm.StatementBuilder.buildCreateTableStatement;
import static com.jisj.orm.StatementBuilder.buildDropTableStatement;
import static com.jisj.orm.utils.sqlExWrap;
import static com.jisj.orm.utils.streamOf;

public class DBManager {
    protected static Logger log = Logger.getLogger(DBManager.class.getName());

    public static void createTableIfNotExists(DAO<?, ?> dao) {
        DAOImpl<?,?> baseDao = (DAOImpl<?,?>) dao;
        String createStatement = buildCreateTableStatement(dao.getProfile(), true);
        try {
            log.info("Create table: " + dao.getProfile().getTableName());
            baseDao.withConnection(connection -> {
                baseDao.doUpdate(connection, createStatement);
                return null;
            });
        } catch (SQLException e) {
            log.warning("Create table error: " + dao.getProfile().getTableName() + "\n" + createStatement);
            throw new RuntimeException(e);
        }
    }

    public static void dropTableIfExists(DAO<?, ?> dao) {
        DAOImpl<?,?> baseDao = (DAOImpl<?,?>) dao;
        String dropStatement = buildDropTableStatement(dao.getProfile(), true);
        try {
            log.info("Drop table: " + dao.getProfile().getTableName());
            baseDao.withConnection(connection ->
                baseDao.doUpdate(connection, dropStatement, ps -> {
                }, DAOImpl.RSWrapper::getGeneratedKeys)
            );
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
            fromDB = ((DAOImpl<?,?>)dao)
                    .withConnection(connection -> getCreateStatement(connection,dao.getProfile().getTableName()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String generated = StatementBuilder.buildCreateTableStatement(dao.getProfile(), false);
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


    public static boolean tableExists(DAOImpl<?,?> dao) {
        try {
            return dao.withConnection(connection -> tableExists(connection, dao.getProfile().getTableName()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
