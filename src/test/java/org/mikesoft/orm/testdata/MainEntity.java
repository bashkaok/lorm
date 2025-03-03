package org.mikesoft.orm.testdata;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode
@ToString
@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "MainTable", uniqueConstraints = {@UniqueConstraint(columnNames = {"stringField", "stringDefaultColumn"})})
public class MainEntity {
    @Column(unique = true, updatable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
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
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "join_MainTable_with_EmbeddedTable",
            joinColumns = {@JoinColumn(name = "OWNER_ID", referencedColumnName = "id")},
            inverseJoinColumns = @JoinColumn(name = "EMBEDDED_ID", referencedColumnName = "id"))
    List<EmbeddedEntity> embeddedList;
    @ManyToMany(fetch = FetchType.EAGER)
    List<EmbeddedEntity> embeddedListDefault;


}