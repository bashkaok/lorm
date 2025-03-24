package com.jisj.orm.entity;

import jakarta.persistence.*;
import lombok.*;
import com.jisj.orm.EntityProfile;


/**
 * ORM inner class for a joining tables manipulated
 */
@SuppressWarnings({"LombokGetterMayBeUsed", "LombokSetterMayBeUsed"})
@NoArgsConstructor
@Setter
@ToString(callSuper = true)
@Entity
@Table
public class JoinTableEntity<ID> {
    @Column(unique = true, updatable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private ID id; //TODO replace by Long if need or ?
    private Object ownerId;
    private Object embeddedId;
    @Transient
    @ToString.Exclude
    private EntityProfile profile;

    public JoinTableEntity(Object ownerId, Object embeddedId) {
        this.ownerId = ownerId;
        this.embeddedId = embeddedId;
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    public EntityProfile getProfile() {
        return profile;
    }

    public Object getOwnerId() {
        return ownerId;
    }

    public Object getEmbeddedId() {
        return embeddedId;
    }
}
