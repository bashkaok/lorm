package org.mikesoft.orm.testdata;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.mikesoft.orm.entity.AbstractEntity;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "EmbeddedTable")
public class EmbeddedEntity extends AbstractEntity {
    private String firstField;
}
