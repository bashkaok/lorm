package org.mikesoft.orm.entity;

import jakarta.persistence.Entity;

@Entity
public class JoinTableEntityIntID extends JoinTableEntity<Integer> {
    public JoinTableEntityIntID() {
    }

    public JoinTableEntityIntID(Object ownerId, Object embeddedId) {
        super(ownerId, embeddedId);
    }
}
