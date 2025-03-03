package org.mikesoft.orm.repository;

import org.mikesoft.orm.DAO;
import org.mikesoft.orm.entity.JoinTableEntity;
import org.mikesoft.orm.entity.JoinTableEntityIntID;


public class RepositoryFactory {
    public RepositoryFactory() {
    }

    public static CRUDRepository<?, ?> createCRUDRepository(DAO<?, ?> dao) {
        if (dao.getProfile().getEntityClass() == JoinTableEntity.class
                || dao.getProfile().getEntityClass() == JoinTableEntityIntID.class)
            return new JoinCRUDRepositoryImpl<>(dao);
        return new CRUDRepositoryImpl<>(dao);
    }

    public static PersistRepository<?, ?> createPersistRepository(DAO<?, ?> dao) {
        return new PersistRepositoryImpl<>(createCRUDRepository(dao));
    }

    public static PersistRepository<?, ?> createPersistRepository(CRUDRepository<?, ?> crud) {
        return new PersistRepositoryImpl<>(crud);
    }


}
