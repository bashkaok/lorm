package org.mikesoft.orm;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Stream;

import static org.mikesoft.orm.EntityProfileFactory.*;


@Getter
public class EntityProfile {
    protected final Class<?> entityClass;
    private final Constructor<?> noArgsConstructor;
    //persistence.Table
    @Setter
    private String tableName;
    @Setter
    private List<UniqueConstraint> uniqueConstraints = List.of();

    private final Map<String, Column> columnsByField = new HashMap<>();
    private final Map<String, Column> columns = new HashMap<>();
    @Setter
    private Column idColumn;
    private final List<ForeignKey> foreignKeys = new ArrayList<>();
    private final Map<String, String> statements = new HashMap<>();

    public EntityProfile(Class<?> entityClass) {
        this.entityClass = entityClass;
        this.noArgsConstructor = findNoArgsConstructor(entityClass);
    }

    public Object newEntityInstance() {
        try {
            Constructor<?> constructor = entityClass.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No args constructor not found", e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setIdValue(Object entity, Object value) {
        getIdColumn().setValue(entity, value);
    }

    public Object getIdValue(Object entity) {
        return getIdColumn().getValue(entity);
    }

    public void copy(Object source, Object destination) {
        if (source.getClass() != destination.getClass())
            throw new IllegalArgumentException("Wrong class types: " + source.getClass() + " and " + destination.getClass());
        getCreateTableColumns()
                .forEach(column -> {
                    try {
                        column.getField().set(destination, column.getField().get(source));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void enrich(Object entity, Object fromEntity) {
        if (entity.getClass() != fromEntity.getClass())
            throw new IllegalArgumentException("Wrong class types: " + entity.getClass() + " and " + fromEntity.getClass());
        getCreateTableColumns()
                .filter(column -> column.getValue(entity) == null)
                .forEach(column -> column.setValue(entity, column.getValue(fromEntity)));
    }

    public Stream<Column> getCreateTableColumns() {
        return getColumnsByField().values().stream()
                .filter(column -> !column.isManyToManyOwner())
                .sorted(Comparator.comparing(Column::getOrder));
    }

    public Stream<Column> getInsertablePrimitiveColumns() {
        return getColumnsByField().values().stream()
                .filter(Column::isPrimitive)
                .filter(Column::isInsertable)
                .sorted(Comparator.comparing(Column::getOrder));
    }

    public Stream<Column> getUpdatablePrimitiveColumns() {
        return getColumnsByField().values().stream()
                .filter(Column::isPrimitive)
                .filter(Column::isUpdatable)
                .sorted(Comparator.comparing(Column::getOrder));
    }

    public Stream<Column> getUniquePrimitiveColumns() {
        return getColumnsByField().values().stream()
                .filter(Column::isPrimitive)
                .filter(Column::isUnique)
                .sorted(Comparator.comparing(Column::getOrder));
    }

    public Stream<Column> getManyToManyColumns() {
        return getColumnsByField().values().stream()
                .filter(Column::isManyToManyOwner)
                .sorted(Comparator.comparing(Column::getOrder));
    }

    public Column getColumnByField(String fieldName) {
        return getColumnsByField().get(fieldName);
    }

    public Column getColumn(String columnName) {
        return getColumns().get(columnName);
    }

    public Object[] getValues(String[] columns, Object entity) {
        Object[] result = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            result[i] = getColumn(columns[i]).getValue(entity);
        }
        return result;
    }

    public String getStatement(String key) {
        return getStatements().get(key);
    }

    @Override
    public String toString() {
        return "EntityProfile{" +
                "tableName='" + tableName + '\'' +
                ", idColumn=" + idColumn +
                '}';
    }

    //TODO Refactoring: move annotation parsers to EntityProfileFactory
    @SuppressWarnings({"LombokGetterMayBeUsed", "LombokSetterMayBeUsed"})
    @Getter
    @Setter
    public static class Column {
        private final Field field;
        private EntityProfile parent;

        private Class<?> targetJavaType;
        private int order;
        private EntityProfile joinTableProfile;
        private boolean primitive = true;
        //javax.persistence.Transient
        private boolean transientColumn = false;
        //javax.persistence.Id
        private boolean id = false;
        //javax.persistence.GeneratedValue
        private GenerationType generationType;
        //javax.persistence.ManyToMany
        private boolean manyToManyOwner = false;
        private boolean fetchEager = false;
        //javax.persistence.Column
        private EntityProfileFactory.ColumnAnnotation columnAnnotation;
        private String columnName;

        public Column(Field field) {
            this.field = field;
            this.targetJavaType = field.getType();
            parseAnnotations();
            parseColumnAnnotation(this);
        }

        private void parseAnnotations() {
            for (Annotation a : field.getDeclaredAnnotations()) {
                switch (a) {
                    case Transient ignored -> transientColumn = true;
                    case Id ignored -> {
                        id = true;
                        generationType = GenerationType.AUTO;
                    }
                    case GeneratedValue value -> generationType = value.strategy();
                    case ManyToMany manyToMany -> parsePersistenceManyToMany(manyToMany);
                    default -> {}
                }
            }
        }

        @Override
        public String toString() {
            return "Column{" +
                    "field=" + getFieldName() +
                    ", columnName='" + columnName + '\'' +
                    ", targetJavaType=" + targetJavaType +
                    '}';
        }

        private void parsePersistenceManyToMany(ManyToMany manyToMany) {
            manyToManyOwner = manyToMany.mappedBy().isEmpty();
            fetchEager = manyToMany.fetch() == FetchType.EAGER;
            if (Collection.class.isAssignableFrom(field.getType())) {
                this.targetJavaType = (Class<?>) ((ParameterizedType) field.getAnnotatedType().getType()).getActualTypeArguments()[0];
            }
            primitive = false;
        }

        public String getColumnName() {
            return getOrElse(columnName, columnAnnotation.name());
        }

        public String getFieldName() {
            return field.getName();
        }

        public void setValue(Object entity, Object value) {
            try {
                getField().set(entity, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public Object getValue(Object entity) {
            try {
                return getField().get(entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean isCollection() {
            return Collection.class.isAssignableFrom(getField().getType());
        }

        public void setColumnAnnotation(EntityProfileFactory.ColumnAnnotation columnAnnotation) {
            this.columnAnnotation = columnAnnotation;
        }

        public EntityProfileFactory.ColumnAnnotation getColumnAnnotation() {
            return columnAnnotation;
        }

        public boolean isInsertable() {
            return getColumnAnnotation().insertable();
        }

        public boolean isNullable() {
            return getColumnAnnotation().nullable();
        }

        public boolean isUnique() {
            return getColumnAnnotation().unique();
        }

        public boolean isUpdatable() {
            return getColumnAnnotation().updatable();
        }

        public void join(EntityProfile embed) {
            joinTableProfile = createJoinTableProfile(this, getParent(), embed);
        }
    }

    public record ForeignKey(String referenceTable,
                             String[] columns,
                             String[] referenceColumns,
                             String keyDefinition) {
        public ForeignKey(String referenceTable,
                          String[] columns,
                          String[] referenceColumns) {
            this(referenceTable, columns, referenceColumns, "");
        }
    }

}

