package org.mikesoft.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class StatementBuilder {

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
    public static String buildCreateStatement(EntityProfile profile, boolean ifNotExists) {
        List<String> statement = new ArrayList<>();
        String header = "CREATE TABLE " + (ifNotExists ? "IF NOT EXISTS " : "") + profile.getTableName();
        profile.getCreateTableColumns().forEach(column -> statement.add(buildField(column)));
        profile.getUniqueConstraints()
                .forEach(constraint -> statement.add("UNIQUE(" +
                        Arrays.stream(constraint.columnNames()).map(StatementBuilder::inQuotes).collect(Collectors.joining(","))
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

    private static String inQuotes(String str) {
        return "\"" + str + "\"";
    }

    private static String buildField(EntityProfile.Column column) {
        StringBuilder builder = new StringBuilder();
        builder.append('"').append(column.getColumnName()).append('"').append("\t");
        if (column.getColumnDefinition().isEmpty()) {
            builder.append(typeJava2Sqlite(column.getTargetJavaType().getSimpleName()));
        } else builder.append(column.getColumnDefinition());
        builder.append(column.isId() ? " PRIMARY KEY" : "");
        builder.append(column.getGenerationType() != null ? " AUTOINCREMENT" : "");
        builder.append(column.isNullable() ? "" : " NOT NULL");
        builder.append(column.isUnique() ? " UNIQUE" : "");
        return builder.toString();
    }

    /**
     * @see <a href=https://www.sqlite.org/datatype3.html#affinity>SQLite types</a>
     */
    private static String typeJava2Sqlite(String type) {
        return
                switch (type) {
                    case "String" -> "TEXT";
                    case "int", "Integer", "long", "Long", "boolean", "Boolean" -> "INTEGER";
                    case "double", "float" -> "REAL";
                    default -> "";
                };
    }

    public static String buildReadByEntityStatement(EntityProfile profile) {
        return "SELECT * FROM " + profile.getTableName() +
                "\nWHERE " +
                profile.getCreateTableColumns()
                        .filter(column -> !column.isId())
                        .map(column -> column.getColumnName() + "=?")
                        .collect(Collectors.joining(" AND "));
    }
}
