package org.mikesoft.orm.repository;

import lombok.Getter;
import org.mikesoft.orm.DAO;
import org.mikesoft.orm.DAOException;
import org.mikesoft.orm.EntityProfile;
import org.mikesoft.orm.entity.AbstractEntity;
import org.mikesoft.orm.entity.JoinTableEntity;

import java.util.*;

@Getter
public class PersistRepositoryImpl<T extends AbstractEntity, ID> implements PersistRepository<T, ID> {
    private final DAO<?, ?> dao;
    private final CRUDRepository<T, ID> crud;
    private OrmRepoContainer global;

    public PersistRepositoryImpl(CRUDRepository<T, ID> crud) {
        this.crud = crud;
        this.dao = ((CRUDRepositoryImpl<?, ?>) crud).getDao();
        this.global = ((CRUDRepositoryImpl<?, ?>) crud).getGlobal();
    }

    @Override
    public void save(T entity) throws DAOException {
        crud.add(entity);
        dao.getProfile().getManyToManyColumns().forEach(column -> {
            if (column.isCollection()) {
                CRUDRepository<AbstractEntity, ?> embedCrud = (CRUDRepository<AbstractEntity, ?>) global.getCrudRepository(column.getTargetJavaType());
                CRUDRepository<AbstractEntity, ?> joinCrud = (CRUDRepository<AbstractEntity, ?>) global.getCrudRepository(column.getJoinTableProfile().getTableName());

                ((Collection<?>) column.getValue(entity))
                        .forEach(embeddedEntity -> {
                            if (column.isInsertable())
                                saveEmbeddedEntity(embedCrud, (AbstractEntity) embeddedEntity);
                            JoinTableEntity<?> joinEntity = createJoinTableEntity(entity, (AbstractEntity) embeddedEntity,
                                    ((JoinCRUDRepositoryImpl<?>) joinCrud).getDao().getProfile());
                            addJoinTableEntity(joinCrud, joinEntity);
                        });
            }
        });
    }

    private void saveEmbeddedEntity(CRUDRepository<AbstractEntity, ?> crud, AbstractEntity entity) {
        try {
            crud.add(entity);
        } catch (DAOException e) {
            throw new RuntimeException(e.getMessage() + ": " + e.getCauseEntity(), e);
        }
    }

    private JoinTableEntity<?> createJoinTableEntity(AbstractEntity ownerEntity, AbstractEntity embeddedEntity, EntityProfile entityProfile) {
        Object ownerJoinFieldValue = dao.getProfile().getColumnByField("id").getValue(ownerEntity);
        Object embedJoinFieldValue = dao.getProfile().getColumnByField("id").getValue(embeddedEntity);
        if (embedJoinFieldValue == null)
            throw new IllegalArgumentException("Unexpected ID=null in embedded entity " + embeddedEntity.getClass());
        JoinTableEntity<?> joinTableEntity = new JoinTableEntity<>(ownerJoinFieldValue, embedJoinFieldValue);
        //for DAOException message
        joinTableEntity.setProfile(entityProfile);
        return joinTableEntity;
    }

    private static void addJoinTableEntity(CRUDRepository<AbstractEntity, ?> joinCrud, JoinTableEntity<?> joinTableEntity) {
        try {
            joinCrud.add(joinTableEntity);
        } catch (DAOException e) {
            if (e.getErrorCode() == DAOException.ErrorCode.RECORD_EXISTS) return;
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveOrUpdate(T entity) {
        try {
            if (entity.getId() == null) {
                save(entity);
            } else update(entity);
        } catch (DAOException e) {
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

    private void loadEmbedded(T entity) {
        dao.getProfile().getManyToManyColumns().forEach(column -> {
            if (column.isCollection() && column.isFetchEager()) {
                CRUDRepository<?, ID> embedCrud = (CRUDRepository<?, ID>) global.getCrudRepository(column.getTargetJavaType());
                JoinCRUDRepository<ID> joinCrud = (JoinCRUDRepository<ID>) global.getCrudRepository(column.getJoinTableProfile().getTableName());
                try {
                    List<JoinTableEntity<ID>> joined = joinCrud.findAllEmbedded((ID) entity.getId());
                    List<?> embedded = joined.stream()
                            .map(joinEntity -> embedCrud.get(joinEntity.getEmbeddedId()))
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

    @Override
    public void update(T entity) throws DAOException {
        crud.update(entity);
        for (EntityProfile.Column column : dao.getProfile().getManyToManyColumns().toList()) {
            if (column.isCollection()) {
                CRUDRepository<AbstractEntity, ?> embedCrud = (CRUDRepository<AbstractEntity, ?>) global.getCrudRepository(column.getTargetJavaType());
                JoinCRUDRepository<ID> joinCrud = (JoinCRUDRepository<ID>) global.getCrudRepository(column.getJoinTableProfile().getTableName());

                joinCrud.deleteAllEmbedded((ID) entity.getId());
                for (Object embeddedEntity : ((Collection<?>) column.getValue(entity))) {
                    if (column.isUpdatable())
                        embedCrud.update((AbstractEntity) embeddedEntity);
                    JoinTableEntity<?> joinEntity = createJoinTableEntity(entity, (AbstractEntity) embeddedEntity,
                            ((JoinCRUDRepositoryImpl<?>) joinCrud).getDao().getProfile());
                    addJoinTableEntity(joinCrud, joinEntity);
                }
            }
        }
    }

    @Override
    public void refresh(T entity) {
        try {
            crud.refresh(entity);
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }
        loadEmbedded(entity);
    }

    @Override
    public void persist(T entity) {
        crud.merge(entity);
        dao.getProfile().getManyToManyColumns().forEach(column -> {
            persistColumn(entity, column);
        });
    }

    private void persistColumn(T entity, EntityProfile.Column column) {
        if (column.isCollection()) {
            CRUDRepository<AbstractEntity, ?> embedCrud = (CRUDRepository<AbstractEntity, ?>) global.getCrudRepository(column.getTargetJavaType());
            CRUDRepository<AbstractEntity, ?> joinCrud = (CRUDRepository<AbstractEntity, ?>) global.getCrudRepository(column.getJoinTableProfile().getTableName());

            ((Collection<?>) column.getValue(entity))
                    .forEach(embeddedEntity -> {
                        persistEmbeddedEntity(column, embedCrud, (AbstractEntity) embeddedEntity);
                        JoinTableEntity<?> joinTableEntity = createJoinTableEntity(entity,
                                (AbstractEntity) embeddedEntity,
                                ((JoinCRUDRepositoryImpl<?>) joinCrud).getDao().getProfile());
                        addJoinTableEntity(joinCrud, joinTableEntity);
                    });
        }
    }

    private static void persistEmbeddedEntity(EntityProfile.Column column,
                                              CRUDRepository<AbstractEntity, ?> embedCrud,
                                              AbstractEntity embeddedEntity) {

        if (!column.isInsertable() && embeddedEntity.getId() == null) {
            throw new IllegalArgumentException("Try add record in non-insertable column: " + column.getColumnName());
        }
        if (!column.isUpdatable() && embeddedEntity.getId() == null) {
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
