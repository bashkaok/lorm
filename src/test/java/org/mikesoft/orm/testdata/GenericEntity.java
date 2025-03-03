package org.mikesoft.orm.testdata;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Setter;


@SuppressWarnings({"LombokGetterMayBeUsed", "LombokSetterMayBeUsed"})
@NoArgsConstructor
@Setter
@Entity
@Table
public class GenericEntity<ID> {
    @Column(unique = true, updatable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private ID id;
    private Object ownerId;
    private Object embeddedId;

    public GenericEntity(Object ownerId, Object embeddedId) {
        this.ownerId = ownerId;
        this.embeddedId = embeddedId;
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }
}
