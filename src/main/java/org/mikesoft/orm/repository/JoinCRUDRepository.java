package org.mikesoft.orm.repository;



import java.util.List;

public interface JoinCRUDRepository<T, ID> extends CRUDRepository<T,ID> {
    List<T> findAllEmbedded(ID ownerId);
    List<T> findAllOwners(ID embeddedId);
    int deleteAllEmbedded(ID ownerId);
    T createEntity(ID ownerId, ID embeddedId);
}
