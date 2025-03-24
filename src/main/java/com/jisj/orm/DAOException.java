package com.jisj.orm;

import lombok.Getter;
import com.jisj.orm.entity.JoinTableEntity;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.sql.SQLException;
import java.util.logging.Logger;

@Getter
public class DAOException extends Exception {
    private Object causeEntity;
    private ErrorCode errorCode;

    public DAOException(String message, SQLException causeException, ErrorCode errorCode, Object causeEntity) {
        super(message, causeException);
        this.causeEntity = causeEntity;
        this.errorCode = errorCode;
    }

    public static DAOException onSQLError(SQLException sqlException, Object causeEntity, Logger logger) {
        DAOException newException = buildException(sqlException, causeEntity);
        if (logger !=null)
            logger.fine("\nSQLException: Error code=" + sqlException.getErrorCode() + " Message=" + sqlException.getMessage() +
                    "\nDAOException: " + newException.getErrorCode() + " on " + causeEntity);
        return newException;
    }

    private static DAOException buildException(SQLException sqlException, Object causeEntity) {
        if (!(sqlException.getCause() instanceof SQLiteException)) throw new RuntimeException("Unexpected non SQLiteException:" + sqlException.getCause());
        SQLiteErrorCode errorCode = ((SQLiteException)sqlException.getCause()).getResultCode();
        return switch (errorCode) {
            case SQLITE_CONSTRAINT_PRIMARYKEY, SQLITE_CONSTRAINT_UNIQUE -> new DAOException("Entity already exists", sqlException, ErrorCode.RECORD_EXISTS, causeEntity);
            case SQLITE_CONSTRAINT_FOREIGNKEY ->
                    new DAOException("Foreign key error for " + causeEntity +
                            ((causeEntity instanceof JoinTableEntity<?> joinTable)? "(" + joinTable.getProfile().getTableName() + ")":""),
                            sqlException, ErrorCode.FOREIGN_KEY, causeEntity);
            default -> throw new RuntimeException("Unknown error: " + sqlException.getErrorCode() + " : " + causeEntity, sqlException);
        };
    }

    public enum ErrorCode {
        RECORD_EXISTS,
        FOREIGN_KEY,
        RECORD_NOT_FOUND
    }
}
