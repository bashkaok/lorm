package org.mikesoft.orm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteDialectTest {
    Dialect d = new SQLiteDialect();

    @Test
    void getDataType() {
        assertEquals("TEXT", d.getDataType(String.class.getTypeName()));
        assertEquals("INTEGER", d.getDataType(Integer.class.getTypeName()));
        assertEquals("INTEGER", d.getDataType(int.class.getTypeName()));
        assertEquals("INTEGER", d.getDataType(Long.class.getTypeName()));
        assertEquals("INTEGER", d.getDataType(long.class.getTypeName()));
        assertEquals("INTEGER", d.getDataType(Boolean.class.getTypeName()));
        assertEquals("INTEGER", d.getDataType(boolean.class.getTypeName()));
        assertEquals("REAL", d.getDataType(Double.class.getTypeName()));
        assertEquals("REAL", d.getDataType(double.class.getTypeName()));
        assertEquals("REAL", d.getDataType(Float.class.getTypeName()));
        assertEquals("REAL", d.getDataType(float.class.getTypeName()));
        assertThrowsExactly(IllegalArgumentException.class, () -> d.getDataType(Object.class.getTypeName()));
    }
}