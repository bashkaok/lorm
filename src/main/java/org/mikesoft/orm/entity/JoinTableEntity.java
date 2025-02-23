package org.mikesoft.orm.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.*;
import org.mikesoft.orm.EntityProfile;


/**
 * ORM inner class for a joining tables manipulated
 */
@NoArgsConstructor
@Setter
@Getter
@ToString(callSuper = true)
@Entity
@Table
public class JoinTableEntity<ID> extends AbstractEntity {
    ID ownerId;
    ID embeddedId;
    @Transient
    @ToString.Exclude
    private EntityProfile profile;

    public JoinTableEntity(ID ownerId, ID embeddedId) {
        this.ownerId = ownerId;
        this.embeddedId = embeddedId;
    }
}
