package com.jisj.orm.testdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class NoArgsConstructorNotFound {
    @Column
    private String stringField;

    public NoArgsConstructorNotFound(String field) {
        stringField = field;
    }
}
