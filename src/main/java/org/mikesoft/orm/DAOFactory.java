package org.mikesoft.orm;



import javax.sql.DataSource;

public class DAOFactory {

    public static DAO<?,?> createDAO(DataSource dataSource, EntityProfile entityProfile) {
        return new DAOImpl<>(dataSource, entityProfile);
    }

    public static DAO<?,?> createDAO(DataSource dataSource, Class<?> entityClass) {
        return new DAOImpl<>(dataSource, entityClass);
    }

}
