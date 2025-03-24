package com.jisj.orm;

/**
 * @see <a href=https://www.sqlite.org/datatype3.html#affinity>SQLite types</a>
 */
public class SQLiteDialect extends Dialect{
    @Override
    public String getDataType(String javaTypeName) {
        return switch (javaTypeName) {
            case "java.lang.String"             -> "TEXT";
            case "java.lang.Integer", "int",
                 "java.lang.Long", "long",
                 "java.lang.Boolean", "boolean" -> "INTEGER";
            case "java.lang.Double", "double",
                 "java.lang.Float", "float"     -> "REAL";
            default -> throw new IllegalArgumentException("Unexpected value: " + javaTypeName);
        };
    }
}
