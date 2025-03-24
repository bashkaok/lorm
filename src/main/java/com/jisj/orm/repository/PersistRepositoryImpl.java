package com.jisj.orm.repository;

import com.jisj.orm.DAO;
import com.jisj.orm.DAOException;
import com.jisj.orm.EntityProfile;
import com.jisj.orm.entity.JoinTableEntity;

import java.util.*;

@SuppressWarnings("LombokGetterMayBeUsed")
public class PersistRepositoryImpl<T, ID> implements PersistRepository<T, ID> {
    private final DAO<?, ?> dao;
    private final CRUDRepository<T, ID> crud;
    private OrmRepoContainer global;

    public PersistRepositoryImpl(CRUDRepository<T, ID> crud) {
        this.crud = crud;
        this.dao = ((CRUDRepositoryImpl<?, ?>) crud).getDao();
        this.global = ((CRUDRepositoryImpl<?, ?>) crud).getGlobal();
    }

    public DAO<?, ?> getDao() {
        return dao;
    }

    public OrmRepoContainer getGlobal() {
        return global;
    }

    @Override
    public CRUDRepository<T, ID> getCRUD() {
        return crud;
    }

    @Override
    public void save(T entity) throws DAOException {
        crud.add(entity);
        dao.getProfile().getManyToManyColumns().forEach(column -> {
            if (column.isCollection() && column.getValue(entity) != null) {
                CRUDRepository<?, ?> embedCrud = global.getCrudRepository(column.getTargetJavaType());
                JoinCRUDRepositoryImpl<?, ?> joinCrud = (JoinCRUDRepositoryImpl<?, ?>) global.getCrudRepository(column.getJoinTableProfile().getTableName());

                ((Collection<?>) column.getValue(entity))
                        .forEach(embeddedEntity -> {
                            if (column.isInsertable())
                                saveEmbeddedEntity(embedCrud, embeddedEntity);
                            JoinTableEntity<?> joinEntity = createJoinTableEntity(entity, embeddedEntity,
                                    ((CRUDRepositoryImpl<?, ?>) embedCrud).getDao().getProfile(),
                                    joinCrud.getDao().getProfile());
                            addJoinTableEntity(joinCrud, joinEntity);
                        });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void saveEmbeddedEntity(CRUDRepository<?, ?> crud, Object entity) {
        try {
            ((CRUDRepositoryImpl<Object, ?>) crud).add(entity);
        } catch (DAOException e) {
            throw new RuntimeException(e.getMessage() + ": " + e.getCauseEntity(), e);
        }
    }

    private JoinTableEntity<?> createJoinTableEntity(Object ownerEntity, Object embeddedEntity, EntityProfile embeddedProfile, EntityProfile joinProfile) {
        Object ownerJoinFieldValue = dao.getProfile().getColumnByField("id").getValue(ownerEntity);
        Object embedJoinFieldValue = embeddedProfile.getColumnByField("id").getValue(embeddedEntity);
        if (embedJoinFieldValue == null)
            throw new IllegalArgumentException("Unexpected ID=null in embedded entity " + embeddedEntity.getClass());
        JoinTableEntity<?> joinTableEntity = new JoinTableEntity<>(ownerJoinFieldValue, embedJoinFieldValue);
        //for DAOException message
        joinTableEntity.setProfile(joinProfile);
        return joinTableEntity;
    }

    @SuppressWarnings("unchecked")
    private static void addJoinTableEntity(CRUDRepository<?, ?> joinCrud, JoinTableEntity<?> joinTableEntity) {
        try {
            ((JoinCRUDRepository<JoinTableEntity<?>, ?>) joinCrud).add(joinTableEntity);
        } catch (DAOException e) {
            if (e.getErrorCode() == DAOException.ErrorCode.RECORD_EXISTS) return;
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<T> load(ID id) {
        T entity;
        Optional<T> result = crud.get(id);
        if (result.isEmpty()) return result;
        entity = result.get();
        loadEmbedded(entity);
        return Optional.of(entity);
    }

    @SuppressWarnings("unchecked")
    private void loadEmbedded(T entity) {
        dao.getProfile().getManyToManyColumns().forEach(column -> {
            if (column.isCollection() && column.isFetchEager()) {
                CRUDRepository<?, Object> embedCrud = (CRUDRepository<?, Object>) global.getCrudRepository(column.getTargetJavaType());
                JoinCRUDRepository<?, ?> joinCrud = (JoinCRUDRepository<?, ?>) global.getCrudRepository(column.getJoinTableProfile().getTableName());
                try {
                    List<?> joined = joinCrud.findAllEmbedded(dao.getProfile().getIdValue(entity));
                    List<?> embedded = joined.stream()
                            .map(joinEntity -> embedCrud.get(((JoinTableEntity<?>) joinEntity).getEmbeddedId()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList();
                    if (column.getField().getType().isAssignableFrom(Set.class)) {
                        column.setValue(entity, new HashSet<>(embedded));
                    } else column.setValue(entity, embedded);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(T entity) throws DAOException {
        crud.update(entity);
        for (EntityProfile.Column column : dao.getProfile().getManyToManyColumns().toList()) {
            if (column.isCollection()) {
                CRUDRepository<Object, ?> embedCrud = (CRUDRepository<Object, ?>) global.getCrudRepository(column.getTargetJavaType());
                JoinCRUDRepositoryImpl<?, Object> joinCrud = (JoinCRUDRepositoryImpl<?, Object>) global.getCrudRepository(column.getJoinTableProfile().getTableName());

                joinCrud.deleteAllEmbedded(dao.getProfile().getIdValue(entity));
                for (Object embeddedEntity : ((Collection<?>) column.getValue(entity))) {
                    if (column.isUpdatable())
                        embedCrud.update(embeddedEntity);
                    JoinTableEntity<?> joinEntity = createJoinTableEntity(entity, embeddedEntity,
                            ((CRUDRepositoryImpl<?, ?>) embedCrud).getDao().getProfile(),
                            joinCrud.getDao().getProfile());
                    addJoinTableEntity(joinCrud, joinEntity);
                }
            }
        }
    }

    @Override
    public void refresh(T entity) throws DAOException {
        crud.refresh(entity);
        loadEmbedded(entity);
    }

    @Override
    public void persist(T entity) {
        crud.merge(entity);
        dao.getProfile().getManyToManyColumns().forEach(column -> persistColumn(entity, column));
    }

    @SuppressWarnings("unchecked")
    private void persistColumn(T entity, EntityProfile.Column column) {
        if (column.isCollection() && column.getValue(entity)!=null) {
            CRUDRepository<Object, ?> embedCrud = (CRUDRepository<Object, ?>) global.getCrudRepository(column.getTargetJavaType());
            CRUDRepository<?, ?> joinCrud = global.getCrudRepository(column.getJoinTableProfile().getTableName());

            ((Collection<?>) column.getValue(entity))
                    .forEach(embeddedEntity -> {
                        persistEmbeddedEntity(column, embedCrud, embeddedEntity);
                        JoinTableEntity<?> joinTableEntity = createJoinTableEntity(
                                entity,
                                embeddedEntity,
                                ((CRUDRepositoryImpl<?,?>)embedCrud).getDao().getProfile(),
                                ((JoinCRUDRepositoryImpl<?,?>) joinCrud).getDao().getProfile());
                        addJoinTableEntity(joinCrud, joinTableEntity);
                    });
        }
    }

    private static void persistEmbeddedEntity(EntityProfile.Column column,
                                              CRUDRepository<Object, ?> embedCrud,
                                              Object embeddedEntity) {//TODO Make container for entity value and EntityProfile from DAO
        if (!column.isInsertable() &&
                ((CRUDRepositoryImpl<?,?>)embedCrud).getDao().getProfile().getIdValue(embeddedEntity) == null) {
            throw new IllegalArgumentException("Try add record in non-insertable column: " + column.getColumnName());
        }
        if (!column.isUpdatable() &&
                ((CRUDRepositoryImpl<?,?>)embedCrud).getDao().getProfile().getIdValue(embeddedEntity) == null) {
            throw new IllegalArgumentException("Try update record in non-updatable column: " + column.getColumnName());
        }
        if (column.isInsertable() && column.isUpdatable())
            embedCrud.merge(embeddedEntity);
        else {
            try {
                embedCrud.refresh(embeddedEntity);
            } catch (DAOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
