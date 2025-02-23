package org.mikesoft.orm;

import lombok.*;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.dazlib.database.orm.entity.AbstractEntity;

import javax.persistence.*;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "test_table_name", uniqueConstraints = {@UniqueConstraint(columnNames = {"stringField", "stringDefaultColumn"})})
public class MainTestEntity extends AbstractEntity {
    @Transient
    private final String finalField = "prohibited";
    @Column
    private String stringField;
    @Builder.Default
    @Column(name = "stringDefaultColumn")
    private String stringDefaultField = "default";
    @Column(name = "UniqueField", unique = true)
    private String stringUniqueField;
    @Column
    private Double doubleField;
    @Column
    private float floatField;
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private boolean booleanField;
    private String unAnnotatedField;
    @ManyToMany
    @JoinTable(name = "tbl_joined",
            joinColumns = {@JoinColumn(name = "OWNER_ID", referencedColumnName = "id")},
//                    @JoinColumn(name = "refType", columnDefinition = "DEFAULT 1")},
            inverseJoinColumns = @JoinColumn(name = "EMBEDDED_ID", referencedColumnName = "id"))
    List<EmbeddedEntity> embeddedList;
    @ManyToMany
    @Column(insertable = false)
    List<EmbeddedEntity> embeddedListDefault;


}

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "tbl_embedded")
class EmbeddedEntity extends AbstractEntity {
    private String firstField;
}
