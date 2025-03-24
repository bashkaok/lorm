package com.jisj.orm.repository;

import com.jisj.orm.DAO;
import com.jisj.orm.entity.JoinTableEntity;
import com.jisj.orm.entity.JoinTableEntityIntID;


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
