package org.mikesoft.orm;

import org.junit.jupiter.api.Test;
import org.mikesoft.orm.testdata.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mikesoft.orm.EntityProfileFactory.createAndSetJoinTableProfile;

class EntityProfileTest {
    @Test
    void noEntityAnnotation() {
        assertTrue(assertThrows(RuntimeException.class, () -> EntityProfileFactory.createProfile(NoEntityAnnotation.class))
                .getMessage().contains("@Entity annotation not found for"));
    }

    @Test
    void noColumnAnnotationsFound() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> EntityProfileFactory.createProfile(NoColumnAnnotations.class));
        assertTrue(e.getMessage().contains("No persist columns found in"));
    }

    @Test
    void noArgsConstructorNotFound() {
        assertTrue(assertThrowsExactly(IllegalArgumentException.class, () -> EntityProfileFactory.createProfile(NoArgsConstructorNotFound.class))
                .getMessage().contains("NoArgs declared constructor not found for"));
    }

    @Test
    void finalFieldsFound() {
        assertTrue(assertThrows(RuntimeException.class, () -> EntityProfileFactory.createProfile(FinalFieldsProhibited.class))
                .getMessage().contains("Fields declared as final should be marked @Transient"));
    }


    /**
     * <a href=https://jakarta.ee/specifications/persistence/2.2/apidocs/javax/persistence/column#columnDefinition()>@Column</a>
     */
    @Test
    void default_column_values() {
        EntityProfile p = EntityProfileFactory.createProfile(DefaultValuesEntity.class);
        assertEquals(3, p.getColumns().size());
        assertEquals("DefaultValuesEntity", p.getTableName());
        //@Id
        assertEquals("idField", p.getIdColumn().getColumnName());
        // @Column defaults of:
        EntityProfile.Column c = p.getColumnByField("stringField");
        assertEquals("stringField", c.getColumnName());
        assertEquals("", c.getColumnDefinition());
        assertTrue(c.isInsertable());
        assertEquals(255, c.getLength());
        assertTrue(c.isNullable());
        //c.getPrecision() not implemented
        //c.getScale() not implemented
        //c.getTable() not implemented
        assertFalse(c.isUnique());
        assertTrue(c.isUpdatable());

    }
    /**
     * <a href=https://jakarta.ee/specifications/persistence/2.2/apidocs/javax/persistence/column#columnDefinition()>@Column</a>
     */
    @Test
    void custom_column_values() {
        EntityProfile p = EntityProfileFactory.createProfile(CustomValuesEntity.class);
        assertEquals(3, p.getColumns().size());
        //@Table(name = "CustomTable", uniqueConstraints = {@UniqueConstraint(columnNames = {"stringField", "stringDefaultColumn"})})
        assertEquals("CustomTable", p.getTableName());
        assertEquals(1, p.getUniqueConstraints().size());
        //Id
        //@Column(name = "IDColumn")
        assertEquals("IDColumn", p.getIdColumn().getColumnName());
        //@Column(name = "String")
        EntityProfile.Column c = p.getColumnByField("stringField");
        assertEquals("String", c.getColumnName());
        assertEquals("DEFAULT 0", c.getColumnDefinition());
        assertFalse(c.isInsertable());
        assertEquals(10, c.getLength());
        assertFalse(c.isNullable());
        //c.getPrecision() not implemented
        //c.getScale() not implemented
        //c.getTable() not implemented
        assertTrue(c.isUnique());
        assertFalse(c.isUpdatable());

    }


//        assertEquals(8, ep.getInsertablePrimitiveColumns().count());
//        assertEquals(7, ep.getUpdatablePrimitiveColumns().count());
//        assertEquals(2, ep.getManyToManyColumns().count());
//        assertEquals(255, ep.getColumnsByField().get("unAnnotatedField").getLength());

    @Test
    public void joinTableTest() {
        EntityProfile owner = EntityProfileFactory.createProfile(MainTestEntity.class);
        EntityProfile embed = EntityProfileFactory.createProfile(EmbeddedEntity.class);
        //default join table
        createAndSetJoinTableProfile(owner.getColumnByField("embeddedListDefault"), owner, embed);
        EntityProfile def = (owner.getColumnByField("embeddedListDefault").getJoinTableProfile());
        assertEquals("test_table_name_tbl_embedded", def.getTableName());
        assertEquals("test_table_name_Id", def.getColumnsByField().get("ownerId").getColumnName());
        assertEquals("tbl_embedded_Id", def.getColumnsByField().get("embeddedId").getColumnName());
        assertEquals(2, def.getForeignKeys().size());

        //specified join table
        createAndSetJoinTableProfile(owner.getColumnByField("embeddedList"), owner, embed);
        EntityProfile spec = (owner.getColumnByField("embeddedList").getJoinTableProfile());
        assertEquals("tbl_joined", spec.getTableName());
        assertEquals("OWNER_ID", spec.getColumnsByField().get("ownerId").getColumnName());
        assertEquals("EMBEDDED_ID", spec.getColumnsByField().get("embeddedId").getColumnName());
        assertEquals(2, spec.getForeignKeys().size());
    }

    @Test
    void many2manyTest() {
        EntityProfile owner = EntityProfileFactory.createProfile(MainTestEntity.class);
        EntityProfile.Column c = owner.getColumnByField("embeddedListDefault");
        System.out.println(c);

    }
}