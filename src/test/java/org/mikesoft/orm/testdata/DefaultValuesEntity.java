package org.mikesoft.orm.testdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
public class DefaultValuesEntity {
    @Id
    private int idField;
    @Column
    private String stringField;
    @Column
    private int token;
    @Transient
    private final String finalField = "prohibited";


}