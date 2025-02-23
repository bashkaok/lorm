package org.mikesoft.orm.repository;


import org.mikesoft.orm.DAO;
import org.mikesoft.orm.entity.AbstractEntity;
import org.mikesoft.orm.entity.JoinTableEntity;

import java.sql.SQLException;
import java.util.List;

public class JoinCRUDRepositoryImpl<ID> extends CRUDRepositoryImpl<AbstractEntity,ID> implements JoinCRUDRepository<ID> {
    public JoinCRUDRepositoryImpl(DAO<AbstractEntity, ID> dao) {
        super(dao);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<JoinTableEntity<ID>> findAllEmbedded(ID ownerId) {
        try {
            String columnName = dao.getProfile().getColumnByField("ownerId").getColumnName();
            return dao.findAll(columnName + "=?", ownerId).stream()
                    .map(e->(JoinTableEntity<ID>)e).toList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<JoinTableEntity<ID>> findAllOwners(ID embeddedId) {
        try {
            String columnName = dao.getProfile().getColumnByField("embeddedId").getColumnName();
            return dao.findAll(columnName + "=?", embeddedId).stream()
                    .map(e->(JoinTableEntity<ID>)e).toList();
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
