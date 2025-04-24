package com.jisj.orm.repository;


import com.jisj.orm.DAOException;

import java.util.Optional;

public interface PersistRepository<T,ID> extends Repository<T,ID> {
    /**
     * Saves entity and everything embedded entities
     * @param entity for save entity object
     */
    void save(T entity) throws DAOException;
    Optional<T> load(ID id);
    /**
     * Updates by id the specified entity and join table for embedded entities. Embedded entities should be persisted, otherwise will be thrown IllegalArgumentException
     * @throws DAOException ErrorCode.RECORD_NOT_FOUND if main entity record not found
     * @throws IllegalArgumentException if embedded entity id is null
     */
    void update(T entity) throws DAOException;
    /**
     * Refreshes the entity from store. Refreshes also an embedding (nested) entities
     * @param entity The entity should have not-null ID
     * @throws DAOException with ErrorCode.RECORD_NOT_FOUND
     */
    @Deprecated(since = "use load(ID)")
    void refresh(T entity) throws DAOException;

    /**
     * Persists entity. Merges entity with store record. Also persists all embedded entities in ManyToMany fields with any nest level.
     * @param entity entity object
     * @throws IllegalArgumentException when try to add record in non-insertable or non-updatable column in embedded fields
     */
    void persist(T entity);

    /**
     *
     * @return CRUD repository for entity T with ID
     */
    CRUDRepository<T,ID> getCRUD();
}
