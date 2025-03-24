package com.jisj.orm.jdbc;

import java.sql.PreparedStatement;

//TODO
public class PrepStatementWrapper {
    private final PreparedStatement statement;

    public PrepStatementWrapper(PreparedStatement statement) {
        this.statement = statement;
    }
}
