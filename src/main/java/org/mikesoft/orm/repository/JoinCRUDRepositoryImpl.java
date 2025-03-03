package org.mikesoft.orm.repository;


import org.mikesoft.orm.DAO;
import org.mikesoft.orm.entity.JoinTableEntity;

import java.sql.SQLException;
import java.util.List;

public class JoinCRUDRepositoryImpl<T, ID> extends CRUDRepositoryImpl<JoinTableEntity<ID>, ID> implements JoinCRUDRepository<JoinTableEntity<ID>,ID> {
    @SuppressWarnings("unchecked")
    public JoinCRUDRepositoryImpl(DAO<T, ID> dao) {
        super((DAO<JoinTableEntity<ID>, ID>) dao);
    }

    @Override
    public List<JoinTableEntity<ID>> findAllEmbedded(Object ownerId) {
        try {
            String columnName = dao.getProfile().getColumnByField("ownerId").getColumnName();
            return dao.findAll(columnName + "=?", ownerId).stream()
                    .toList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<JoinTableEntity<ID>> findAllOwners(ID embeddedId) {
        try {
            String columnName = dao.getProfile().getColumnByField("embeddedId").getColumnName();
            return dao.findAll(columnName + "=?", embeddedId).stream()
                    .toList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public int deleteAllEmbedded(ID ownerId) {
        try {
            return dao.deleteAll(dao.getProfile().getColumnByField("ownerId").getColumnName() + "=?", ownerId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JoinTableEntity<ID> createEntity(ID ownerId, ID embeddedId) {
        JoinTableEntity<ID> entity = new JoinTableEntity<>(ownerId, embeddedId);
        entity.setProfile(getDao().getProfile());
        return entity;
    }
}
