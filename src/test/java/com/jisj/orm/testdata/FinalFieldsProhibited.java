package com.jisj.orm.testdata;

import jakarta.persistence.Entity;

@Entity
public class FinalFieldsProhibited {
    private final String finalField = "prohibited";
    private String stringField;
}
