package org.mikesoft.orm.repository;


import org.mikesoft.orm.entity.AbstractEntity;
import org.mikesoft.orm.entity.JoinTableEntity;

import java.util.List;

public interface JoinCRUDRepository<ID> extends CRUDRepository<AbstractEntity,ID> {
    List<JoinTableEntity<ID>> findAllEmbedded(ID ownerId);
    List<JoinTableEntity<ID>> findAllOwners(ID embeddedId);
    int deleteAllEmbedded(ID ownerId);
    JoinTableEntity<ID> createEntity(ID ownerId, ID embeddedId);
}
