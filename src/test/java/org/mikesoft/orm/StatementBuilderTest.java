package org.mikesoft.orm;

import org.junit.jupiter.api.Test;
import org.mikesoft.orm.testdata.EmbeddedEntity;
import org.mikesoft.orm.testdata.MainEntity;

import static org.junit.jupiter.api.Assertions.*;

class StatementBuilderTest {
    private static final EntityProfile ep = EntityProfileFactory.createProfile(MainEntity.class);
    private static final EntityProfile embed = EntityProfileFactory.createProfile(EmbeddedEntity.class);
    static {
        ep.getColumnByField("embeddedListDefault").join(embed);
    }


    @Test
    void buildInsertStatement() {
        String st = StatementBuilder.buildInsertStatement(ep);
//        System.out.println(st);
        assertEquals(ep.getInsertablePrimitiveColumns().count(), st.chars().filter(ch -> ch == '?').count());
    }

    @Test
    void buildCreateTableStatement() {
        String st = StatementBuilder.buildCreateTableStatement(ep, true);
//        System.out.println(st);
        assertTrue(st.contains("UNIQUE(\"stringField\",\"stringDefaultColumn\""));
    }

    @Test
    void buildCreateTableStatement_with_FOREIGNKEY() {
//        System.out.println(ep.getColumnByField("embeddedListDefault").getJoinTableProfile());
        String st = StatementBuilder.buildCreateTableStatement(ep.getColumnByField("embeddedListDefault").getJoinTableProfile(), true);
//        System.out.println(st);
        assertTrue(st.contains("FOREIGN KEY (\"MainTable_Id\") REFERENCES MainTable (\"id\") ON DELETE CASCADE"));
        assertTrue(st.contains("FOREIGN KEY (\"EmbeddedTable_Id\") REFERENCES EmbeddedTable (\"id\") ON DELETE CASCADE"));
    }


    @Test
    void buildUpdateStatement() {
        String st = StatementBuilder.buildUpdateStatement(ep);
//        System.out.println(st);
        assertFalse(st.contains("id"));
        assertEquals(ep.getUpdatablePrimitiveColumns().count(), st.chars().filter(ch -> ch == '?').count());
    }

    @Test
    void buildUpdateByIdStatement() {
        String st = StatementBuilder.buildUpdateByIdStatement(ep);
//        System.out.println(st);
        assertEquals(ep.getUpdatablePrimitiveColumns().count()+1, st.chars().filter(ch -> ch == '?').count());
    }

    @Test
    void buildReadByEntityStatement() {
        String st = StatementBuilder.buildReadByEntityStatement(ep);
//        System.out.println(st);
        ep.getCreateTableColumns()
                .filter(column-> !column.isId())
                .map(EntityProfile.Column::getColumnName)
//                .peek(System.out::println)
                .forEach(columnName-> assertTrue(st.contains(columnName)));
    }

    @Test
    void buildDropTableStatement() {
        assertEquals("DROP TABLE MainTable", StatementBuilder.buildDropTableStatement(ep, false));
        assertEquals("DROP TABLE IF EXISTS MainTable", StatementBuilder.buildDropTableStatement(ep, true));
    }
}