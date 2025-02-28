package org.mikesoft.orm.testdata;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class GenericEntitySubClass extends GenericEntity<Long> {
    public GenericEntitySubClass(Object ownerId, Object embeddedId) {
        super(ownerId, embeddedId);
    }
}
