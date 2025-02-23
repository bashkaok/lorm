package org.mikesoft.orm;



import org.mikesoft.orm.entity.AbstractEntity;

import javax.sql.DataSource;

public class DAOFactory {

    public static DAO<?,?> createDAO(DataSource dataSource, EntityProfile entityProfile) {
        return new DAOImpl<>(dataSource, entityProfile);
    }

    public static DAO<?,?> createDAO(DataSource dataSource, Class<? extends AbstractEntity> entityClass) {
        return new DAOImpl<>(dataSource, entityClass);
    }

}
