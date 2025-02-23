package org.mikesoft.orm.testdata;

import jakarta.persistence.*;

@Entity
@Table(name = "CustomTable", uniqueConstraints = {@UniqueConstraint(columnNames = {"stringField", "token"})})
public class CustomValuesEntity {
    @Id
    @Column(name = "IDColumn")
    private int idField;
    @Column(name = "String", columnDefinition = "DEFAULT 0", insertable = false, length = 10, nullable = false, unique = true, updatable = false)
    private String stringField;
    @Column
    private int token;
}