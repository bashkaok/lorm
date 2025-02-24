package org.mikesoft.orm;

abstract public class Dialect {
    public String getDataType(String javaTypeName) {
        return "";
    }

    /**
     * Operator for comparing NULL operands
     */
    public String getEqualNULLOperator() {
        return "=";
    }

    /**
     * Operator for comparing NULL operands
     */
    public String getNotEqualNULLOperator() {
        return "!=";
    }

}
