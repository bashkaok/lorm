package org.mikesoft.orm.testdata;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

@Entity
public class NoColumnAnnotations {
    @Transient
    private final String stringField = "final";
    @Transient
    private int intField;
}
