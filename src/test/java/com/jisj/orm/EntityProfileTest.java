package com.jisj.orm;

import com.jisj.orm.testdata.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(4, p.getColumns().size());
        assertEquals("DefaultValuesEntity", p.getTableName());
        //@Id
        assertEquals("idField", p.getIdColumn().getColumnName());
        assertEquals(int.class, p.getIdColumn().getTargetJavaType());

        // @Column defaults
        EntityProfile.Column c = p.getColumnByField("stringField");
        assertEquals("stringField", c.getColumnName());
        assertEquals("", c.getColumnAnnotation().columnDefinition());
        assertTrue(c.isInsertable());
        assertEquals(255, c.getColumnAnnotation().length());
        assertTrue(c.isNullable());
        //c.getPrecision() not implemented
        //c.getScale() not implemented
        //c.getTable() not implemented
        assertFalse(c.isUnique());
        assertTrue(c.isUpdatable());
        assertEquals(new EntityProfileFactory.ColumnAnnotation("stringField"), c.getColumnAnnotation());

        //@Transient
        assertNull(p.getColumnByField("finalField"));

        //UnAnnotated
        assertEquals(new EntityProfileFactory.ColumnAnnotation("unAnnotatedField"), p.getColumn("unAnnotatedField").getColumnAnnotation());

    }

    @Test
    void generic_entity() {
        EntityProfile p = EntityProfileFactory.createProfile(GenericEntity.class);
        assertEquals(3, p.getColumns().size());
        assertEquals(Object.class, p.getIdColumn().getTargetJavaType());
        assertEquals(Object.class, p.getColumnByField("ownerId").getTargetJavaType());
        assertEquals(Object.class, p.getColumnByField("embeddedId").getTargetJavaType());
    }

    @Test
    void generic_entity_subclass() {
        GenericEntitySubClass g = new GenericEntitySubClass(1,1);
        EntityProfile p = EntityProfileFactory.createProfile(GenericEntitySubClass.class);
        assertEquals(3, p.getColumns().size());
        assertEquals(Object.class, p.getIdColumn().getTargetJavaType());
        assertEquals(Object.class, p.getColumnByField("ownerId").getTargetJavaType());
        assertEquals(Object.class, p.getColumnByField("embeddedId").getTargetJavaType());
    }


    @Test
    void default_column_values_from_subclass() {
        EntityProfile p = EntityProfileFactory.createProfile(DefaultValuesEntitySubClass.class);
        assertEquals(4, p.getColumns().size());
        assertEquals("DefaultValuesEntitySubClass", p.getTableName());
        //@Id
        assertEquals("idField", p.getIdColumn().getColumnName());
        assertEquals(int.class, p.getIdColumn().getTargetJavaType());

        // @Column defaults
        EntityProfile.Column c = p.getColumnByField("stringField");
        assertEquals("stringField", c.getColumnName());
        assertEquals("", c.getColumnAnnotation().columnDefinition());
        assertTrue(c.isInsertable());
        assertEquals(255, c.getColumnAnnotation().length());
        assertTrue(c.isNullable());
        //c.getPrecision() not implemented
        //c.getScale() not implemented
        //c.getTable() not implemented
        assertFalse(c.isUnique());
        assertTrue(c.isUpdatable());
        assertEquals(new EntityProfileFactory.ColumnAnnotation("stringField"), c.getColumnAnnotation());

        //@Transient
        assertNull(p.getColumnByField("finalField"));

        //UnAnnotated
        assertEquals(new EntityProfileFactory.ColumnAnnotation("unAnnotatedField"), p.getColumn("unAnnotatedField").getColumnAnnotation());

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
        assertEquals("DEFAULT 0", c.getColumnAnnotation().columnDefinition());
        assertFalse(c.isInsertable());
        assertEquals(10, c.getColumnAnnotation().length());
        assertFalse(c.isNullable());
        //c.getPrecision() not implemented
        //c.getScale() not implemented
        //c.getTable() not implemented
        assertTrue(c.isUnique());
        assertFalse(c.isUpdatable());
    }

    @Test
    void manyToMany_default_join() {
        EntityProfile owner = EntityProfileFactory.createProfile(MainEntity.class);
        EntityProfile embed = EntityProfileFactory.createProfile(EmbeddedEntity.class);
        owner.getColumnByField("embeddedListDefault").join(embed);
        EntityProfile join = (owner.getColumnByField("embeddedListDefault").getJoinTableProfile());

        assertEquals("MainTable_EmbeddedTable", join.getTableName());
        assertEquals("MainTable_Id", join.getColumnsByField().get("ownerId").getColumnName());
        assertEquals("EmbeddedTable_Id", join.getColumnsByField().get("embeddedId").getColumnName());
        assertEquals(2, join.getForeignKeys().size());

    }

    @Test
    void manyToMany_specified_join() {
        EntityProfile owner = EntityProfileFactory.createProfile(MainEntity.class);
        EntityProfile embed = EntityProfileFactory.createProfile(EmbeddedEntity.class);
        owner.getColumnByField("embeddedList").join(embed);
        EntityProfile spec = (owner.getColumnByField("embeddedList").getJoinTableProfile());

        assertEquals("join_MainTable_with_EmbeddedTable", spec.getTableName());
        assertEquals("OWNER_ID", spec.getColumnsByField().get("ownerId").getColumnName());
        assertEquals("EMBEDDED_ID", spec.getColumnsByField().get("embeddedId").getColumnName());
        assertEquals(2, spec.getForeignKeys().size());
    }

}