package com.jisj.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class StatementBuilder {
    private static final Dialect dialect = new SQLiteDialect();

    public static String buildUpdateStatement(EntityProfile profile) {
        StringBuilder builder = new StringBuilder("UPDATE ");
        builder.append(profile.getTableName());
        builder.append(" SET (");
        AtomicInteger count = new AtomicInteger(0);
        builder.append(profile.getUpdatablePrimitiveColumns()
                .map(EntityProfile.Column::getColumnName)
                .peek(field -> count.getAndIncrement())
                .collect(Collectors.joining(",")));
        builder.append(")=(");
        builder.repeat("?,", count.get());
        builder.delete(builder.length() - 1, builder.length());
        builder.append(")");
        return builder.toString();
    }

    public static String buildUpdateByIdStatement(EntityProfile profile) {
        return buildUpdateStatement(profile) + "\nWHERE " +
                profile.getIdColumn().getColumnName() +
                "=?";
    }

    public static String buildInsertStatement(EntityProfile profile) {
        StringBuilder builder = new StringBuilder("INSERT INTO ");
        builder.append(profile.getTableName());
        builder.append(" (");
        AtomicInteger count = new AtomicInteger(0);
        builder.append(profile.getInsertablePrimitiveColumns()
                .map(EntityProfile.Column::getColumnName)
                .peek(field -> count.getAndIncrement())
                .collect(Collectors.joining(","))
        );
        builder.append(") VALUES (");
        builder.repeat("?,", count.get());
        builder.delete(builder.length() - 1, builder.length());
        builder.append(")");
        return builder.toString();
    }

    /**
     * @see <a href=https://www.sqlite.org/lang_createtable.html>SQLite CREATE</a>
     */
    public static String buildCreateTableStatement(EntityProfile profile, boolean ifNotExists) {
        List<String> statement = new ArrayList<>();
        String header = "CREATE TABLE " + (ifNotExists ? "IF NOT EXISTS " : "") + profile.getTableName();
        profile.getCreateTableColumns().forEach(column -> statement.add(buildField(column)));
        profile.getUniqueConstraints()
                .forEach(constraint -> statement.add("UNIQUE(" +
                        Arrays.stream(constraint.columnNames())
                                .map(StatementBuilder::inQuotes)
                                .collect(Collectors.joining(","))
                        + ")"));
        profile.getForeignKeys().forEach(fk -> statement.add(
                "FOREIGN KEY (" + Arrays.stream(fk.columns())
                        .map(StatementBuilder::inQuotes).collect(Collectors.joining(",")) +
                        ") REFERENCES " + fk.referenceTable() + " (" + Arrays.stream(fk.referenceColumns())
                        .map(StatementBuilder::inQuotes).collect(Collectors.joining(",")) + ")"
                        + " ON DELETE CASCADE")
        );
        return header + "(\n" + String.join(",\n", statement) + "\n)";
    }

    public static String buildDropTableStatement(EntityProfile profile, boolean ifExists) {
        return "DROP TABLE " +
                (ifExists ? "IF EXISTS " : "") +
                profile.getTableName();
    }

    private static String inQuotes(String str) {
        return "\"" + str + "\"";
    }

    private static String buildField(EntityProfile.Column column) {
//        System.out.println(column.getFieldName() + " : " + column.getTargetJavaType().getTypeName());
        StringBuilder builder = new StringBuilder();
        builder.append('"').append(column.getColumnName()).append('"').append("\t");
        if (column.getColumnAnnotation().columnDefinition().isEmpty()) {
            builder.append(dialect.getDataType(column.getTargetJavaType().getTypeName()));
        } else builder.append(column.getColumnAnnotation().columnDefinition());
        builder.append(column.isId() ? " PRIMARY KEY" : "");
        builder.append(column.getGenerationType() != null ? " AUTOINCREMENT" : "");
        builder.append(column.isNullable() ? "" : " NOT NULL");
        builder.append(column.isUnique() ? " UNIQUE" : "");
        return builder.toString();
    }

    public static String buildReadByEntityStatement(EntityProfile profile) {
        return "SELECT * FROM " + profile.getTableName() +
                "\nWHERE " +
                profile.getCreateTableColumns()
                        .filter(column -> !column.isId())
                        .map(column -> column.getColumnName() + "=?")
                        .collect(Collectors.joining(" AND "));
    }

    public static String buildReadByEntityStatement(EntityProfile profile, Object entityValue) {
        return "SELECT * FROM " + profile.getTableName() +
                "\nWHERE " +
                profile.getCreateTableColumns()
                        .filter(column -> !column.isId())
                        .map(column -> column.getColumnName() + (column.getValue(entityValue) == null ? " IS NULL" : "=?"))
                        .collect(Collectors.joining(" AND "));
    }

}
