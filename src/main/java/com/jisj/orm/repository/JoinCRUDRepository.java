package com.jisj.orm.repository;



import java.util.List;

public interface JoinCRUDRepository<T, ID> extends CRUDRepository<T, ID> {
    List<T> findAllEmbedded(Object ownerId);
    List<T> findAllOwners(ID embeddedId);
    int deleteAllEmbedded(ID ownerId);
    T createEntity(ID ownerId, ID embeddedId);
}
