package org.mikesoft.orm;

import jakarta.persistence.*;
import org.mikesoft.orm.entity.JoinTableEntity;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityProfileFactory {
    public static EntityProfile createProfile(Class<?> entityClass) {
        EntityProfile ep = buildProfile(entityClass);
        buildStatements(ep);
        return ep;
    }

    private static EntityProfile buildProfile(Class<?> entityClass) {
        if (entityClass.getDeclaredAnnotation(Entity.class) == null)
            throw new IllegalArgumentException("@Entity annotation not found for " + entityClass);

        EntityProfile ep = new EntityProfile(entityClass);
        parsePersistenceTable(ep, entityClass);
        createColumns(ep, entityClass);

        if (ep.getIdColumn() == null)
            throw new IllegalArgumentException("ID column not found");
        return ep;
    }

    public static void parseColumnAnnotation(EntityProfile.Column column) {
        assertOnFinal(column.getField());
        Column annotation = column.getField().getDeclaredAnnotation(Column.class);
        if (annotation == null) {
            column.setColumnAnnotation(new EntityProfileFactory.ColumnAnnotation(column.getFieldName()));
            return;
        }
        column.setColumnAnnotation(new EntityProfileFactory.ColumnAnnotation(annotation.columnDefinition(),
                annotation.insertable(),
                annotation.length(),
                getOrElse(annotation.name(), column.getFieldName()),
                annotation.nullable(),
                annotation.precision(),
                annotation.scale(),
                annotation.table(),
                annotation.unique(),
                annotation.updatable()));
    }

    private static void assertOnFinal(Field field) {
        if(field.accessFlags().contains(AccessFlag.FINAL) &&
                        field.getDeclaredAnnotation(Transient.class) == null)
            throw new IllegalArgumentException("The field declared as final should be marked by @Transient " + field.getName());
    }

    /**
     *
     * @return value, if value is not null and not empty(String), else - default value
     */
    static <T> T getOrElse(T value, T defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof String && ((String) value).isEmpty()) return defaultValue;
        return value;
    }


    private static void parsePersistenceTable(EntityProfile ep, Class<?> entityClass) {
        Table jpaTable = entityClass.getDeclaredAnnotation(Table.class);
        if (jpaTable == null) {
            ep.setTableName(entityClass.getSimpleName());
            ep.setUniqueConstraints(List.of());
            return;
        }
        ep.setTableName(jpaTable.name().isEmpty() ? entityClass.getSimpleName() : jpaTable.name());
        ep.setUniqueConstraints(List.of(jpaTable.uniqueConstraints()));
    }

    private static void createColumns(EntityProfile ep, Class<?> entityClass) {
        AtomicInteger order = new AtomicInteger(0);
        extractColumnsFromClass(entityClass).stream()
                .map(EntityProfile.Column::new)
                .filter(column -> !column.isTransientColumn())
                .peek(column -> {
                    if (column.isId()) ep.setIdColumn(column);
                    column.getField().setAccessible(true); //TODO Replace by handler
                    column.setOrder(order.incrementAndGet());
                })
                .forEach(column -> {
                    ep.getColumnsByField().put(column.getFieldName(), column);
                    ep.getColumns().put(column.getColumnName(), column);
                });
        if (ep.getColumnsByField().isEmpty())
            throw new IllegalArgumentException("No persist columns found in " + entityClass);
    }


    public static Constructor<?> findNoArgsConstructor(Class<?> entityClass) {
        try {
            return entityClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("NoArgs declared constructor not found for " + entityClass, e);
        }
    }

    public static void buildStatements(EntityProfile profile) {
        profile.getStatements().put("UPDATE_BY_ID", StatementBuilder.buildUpdateByIdStatement(profile));
        profile.getStatements().put("INSERT", StatementBuilder.buildInsertStatement(profile));
        profile.getStatements().put("READ_BY_ENTITY", StatementBuilder.buildReadByEntityStatement(profile));
    }


    public static List<Field> extractColumnsFromClass(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        extractColumnsFromSuperClass(entityClass, fields);
        List<Field> nonTransient = fields.stream().filter(field -> field.accessFlags().contains(AccessFlag.FINAL) &&
                        field.getDeclaredAnnotation(Transient.class) == null)
                .toList();
        if (!nonTransient.isEmpty())
            throw new IllegalArgumentException("Fields declared as final should be marked @Transient " + nonTransient + " in " + entityClass);
        return fields;
    }

    private static void extractColumnsFromSuperClass(Class<?> entityClass, List<Field> fields) {
        if (entityClass.getSuperclass() != null) extractColumnsFromSuperClass(entityClass.getSuperclass(), fields);
        fields.addAll(List.of(entityClass.getDeclaredFields()));
    }

    public static EntityProfile createJoinTableProfile(EntityProfile.Column ownerColumn,
                                                       EntityProfile ownerProfile,
                                                       EntityProfile embeddedProfile) {
        EntityProfile jp = buildProfile(JoinTableEntity.class);
        //replace generic types of fields by the type of ID
        jp.getCreateTableColumns()
                .filter(f -> (f.getField().getGenericType() != f.getField().getType()))
                .forEach(column -> column.setTargetJavaType(jp.getIdColumn().getTargetJavaType()));

        PersistenceJoinTableParser jtParser =
                new PersistenceJoinTableParser(ownerColumn.getField().getDeclaredAnnotation(JoinTable.class));
        jp.setTableName(jtParser.getNameOrElse(ownerProfile.getTableName() + "_" + embeddedProfile.getTableName()));

        EntityProfile.Column joinOwnerColumn = jp.getColumnByField("ownerId");
        joinOwnerColumn.setColumnName(jtParser.getJoinColumnNameOrElse(ownerProfile.getTableName() + "_Id"));
        jp.getForeignKeys().add(new EntityProfile.ForeignKey(ownerProfile.getTableName(),
                new String[]{joinOwnerColumn.getColumnName()},
                new String[]{jtParser.getJoinColumnReferenceNameOrElse(ownerProfile.getIdColumn().getColumnName())}));

        EntityProfile.Column joinEmbeddedColumn = jp.getColumnByField("embeddedId");
        joinEmbeddedColumn.setColumnName(jtParser.getInverseJoinColumnNameOrElse(embeddedProfile.getTableName() + "_Id"));
        jp.getForeignKeys().add(new EntityProfile.ForeignKey(embeddedProfile.getTableName(),
                new String[]{joinEmbeddedColumn.getColumnName()},
                new String[]{jtParser.getInverseJoinReferenceNameOrElse(embeddedProfile.getIdColumn().getColumnName())}));
        jp.setUniqueConstraints(List.of(customUniqueConstraint(joinOwnerColumn.getColumnName(), joinEmbeddedColumn.getColumnName())));
        jp.getColumns().put(joinOwnerColumn.getColumnName(), joinOwnerColumn);
        jp.getColumns().put(joinEmbeddedColumn.getColumnName(), joinEmbeddedColumn);

        buildStatements(jp);

        return jp;
    }

    private static UniqueConstraint customUniqueConstraint(String... columnNames) {
        return new UniqueConstraint() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return UniqueConstraint.class;
            }

            @Override
            public String name() {
                return "UniqueConstraint";
            }

            @Override
            public String[] columnNames() {
                return columnNames;
            }

            @Override
            public String options() {
                return "";
            }
        };
    }

    public static void createAndSetJoinTableProfile(EntityProfile.Column ownerColumn,
                                                    EntityProfile ownerProfile,
                                                    EntityProfile embeddedProfile) {
        ownerColumn.setJoinTableProfile(createJoinTableProfile(ownerColumn, ownerProfile, embeddedProfile));
    }


    private record PersistenceJoinTableParser(JoinTable joinTable) {
        public String getNameOrElse(String elseName) {
            return joinTable == null ? elseName : joinTable().name();
        }

        public String getJoinColumnNameOrElse(String fieldName) {
            return (joinTable == null || joinTable.joinColumns().length == 0) ? fieldName
                    : joinTable.joinColumns()[0].name();
        }

        public String getJoinColumnReferenceNameOrElse(String fieldName) {
            return (joinTable == null || joinTable.joinColumns().length == 0) ? fieldName
                    : joinTable.joinColumns()[0].referencedColumnName();
        }

        public String getInverseJoinColumnNameOrElse(String fieldName) {
            return (joinTable == null || joinTable.inverseJoinColumns().length == 0) ? fieldName
                    : joinTable.inverseJoinColumns()[0].name();
        }

        public String getInverseJoinReferenceNameOrElse(String fieldName) {
            return (joinTable == null || joinTable.inverseJoinColumns().length == 0) ? fieldName
                    : joinTable.inverseJoinColumns()[0].referencedColumnName();
        }


    }

    public record ColumnAnnotation(String columnDefinition,
                                   boolean insertable,
                                   int length,
                                   String name,
                                   boolean nullable,
                                   int precision,
                                   int scale,
                                   String table,
                                   boolean unique,
                                   boolean updatable) {

        public ColumnAnnotation(String name) {
            this("", true, 255, name, true, 0, 0, "", false, true);
        }

        public static ColumnAnnotation getDefault() {
            return new ColumnAnnotation("", true, 255, "", true, 0, 0, "", false, true);
        }

    }
}
