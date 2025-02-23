package org.mikesoft.orm.repository;

import org.mikesoft.orm.DAO;
import org.mikesoft.orm.entity.AbstractEntity;
import org.mikesoft.orm.entity.JoinTableEntity;


public class RepositoryFactory {
    public RepositoryFactory() {
    }

    @SuppressWarnings("unchecked")
    public static CRUDRepository<?,?> createCRUDRepository(DAO<?,?> dao) {
        if (dao.getProfile().getEntityClass() == JoinTableEntity.class)
            return new JoinCRUDRepositoryImpl<>((DAO<AbstractEntity, ?>)dao);
        return new CRUDRepositoryImpl<>(dao);
    }

    public static PersistRepository<?,?> createPersistRepository(DAO<?,?> dao) {
        return new PersistRepositoryImpl<>(createCRUDRepository(dao));
    }

    public static PersistRepository<?,?> createPersistRepository(CRUDRepository<?,?> crud) {
        return new PersistRepositoryImpl<>(crud);
    }


}
