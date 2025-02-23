package org.mikesoft.orm;

import org.junit.jupiter.api.Test;

import static org.dazlib.database.orm.EntityProfileFactory.createAndSetJoinTableProfile;
import static org.junit.jupiter.api.Assertions.*;

class StatementBuilderTest {
    private static EntityProfile ep = EntityProfileFactory.createProfile(MainTestEntity.class);
    private static EntityProfile embed = EntityProfileFactory.createProfile(EmbeddedEntity.class);
    static {
        createAndSetJoinTableProfile(ep.getColumnByField("embeddedListDefault"), ep, embed);
    }


    @Test
    void buildInsertStatement() {
        String st = StatementBuilder.buildInsertStatement(ep);
//        System.out.println(st);
        assertEquals(ep.getInsertablePrimitiveColumns().count(), st.chars().filter(ch -> ch == '?').count());
    }

    @Test
    void buildCreateTableStatement() {
        String st = StatementBuilder.buildCreateStatement(ep, true);
        System.out.println(st);
        assertTrue(st.contains("UNIQUE(\"stringField\",\"stringDefaultColumn\""));
    }

    @Test
    void buildCreateTableStatement_with_FOREIGNKEY() {
        String st = StatementBuilder.buildCreateStatement(ep.getColumnByField("embeddedListDefault").getJoinTableProfile(), true);
        System.out.println(st);
        assertTrue(st.contains("FOREIGN KEY (\"test_table_name_Id\") REFERENCES test_table_name (\"id\") ON DELETE CASCADE"));
        assertTrue(st.contains("FOREIGN KEY (\"tbl_embedded_Id\") REFERENCES tbl_embedded (\"id\") ON DELETE CASCADE"));
    }


    @Test
    void buildUpdateStatement() {
        String st = StatementBuilder.buildUpdateStatement(ep);
        System.out.println(st);
        assertFalse(st.contains("id"));
        assertEquals(ep.getUpdatablePrimitiveColumns().count(), st.chars().filter(ch -> ch == '?').count());
    }

    @Test
    void buildUpdateByIdStatement() {
        String st = StatementBuilder.buildUpdateByIdStatement(ep);
        System.out.println(st);
        assertEquals(ep.getUpdatablePrimitiveColumns().count()+1, st.chars().filter(ch -> ch == '?').count());
    }

    @Test
    void buildReadByEntityStatement() {
        String st = StatementBuilder.buildReadByEntityStatement(ep);
        System.out.println(st);
    }
}