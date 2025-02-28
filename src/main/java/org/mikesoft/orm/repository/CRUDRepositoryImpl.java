package org.mikesoft.orm.repository;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.mikesoft.orm.DAO;
import org.mikesoft.orm.DAOException;

import jakarta.persistence.UniqueConstraint;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mikesoft.orm.DAOException.onSQLError;


@Getter
@Setter
@Log
public class CRUDRepositoryImpl<T,ID> implements CRUDRepository<T, ID> {
    protected final DAO<T, ID> dao;
    protected OrmRepoContainer global = null;

    public CRUDRepositoryImpl(DAO<T, ID> dao) {
        this.dao = dao;
    }

    @Override
    public void add(T entity) throws DAOException {
        try {
            dao.create(entity);
        } catch (SQLException e) {
            throw onSQLError(e, entity, log);
        }
    }

    @Override
    public void addAll(List<T> entity) throws DAOException {
        try {
            dao.createAll(entity);
        } catch (SQLException e) {
            throw onSQLError(e, entity, log);
        }
    }

    @Override
    public Optional<T> get(ID id) {
        try {
            return dao.read(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<T> getByEntity(T entity) {
        try {
            return dao.readByEntity(entity);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Stream<T> getAll() {
        try {
            return dao.readAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(T entity) throws DAOException {
//        if (entity.getId() == null)
        if (dao.getProfile().getIdValue(entity) == null)
            throw new IllegalArgumentException("Wrong ID for update. Expected not null ID for entity " + entity);
        try {
            dao.update(entity);
        } catch (SQLException e) {
            throw onSQLError(e, entity, log);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addOrUpdate(T entity) throws DAOException {
//        if (entity.getId() == null) {
        if (dao.getProfile().getIdValue(entity) == null) {
            add(entity);
            return;
        }
        if (get((ID) dao.getProfile().getIdValue(entity)).isEmpty()) add(entity);
        else update(entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void merge(T entity) {
        if (dao.getProfile().getIdValue(entity) != null) {
            try {
                Optional<T> found = get((ID) dao.getProfile().getIdValue(entity));
                if (found.isPresent()) {
                    dao.getProfile().enrich(entity, found.get()); //TODO replace on UPDATE of not-null fields
                    update(entity);
                    return;
                }
            } catch (DAOException e) {
                if (e.getErrorCode() != DAOException.ErrorCode.RECORD_NOT_FOUND)
                    throw new RuntimeException(e);
            }
        }

        //searching by unique key fields
        T found = dao.getProfile().getUniquePrimitiveColumns()
                .filter(column -> !column.isId())
                .filter(column -> column.getValue(entity) != null)
                .map(column -> findByUnique(column.getColumnName(), column.getValue(entity)))
                .filter(Optional::isPresent)
                .map(Optional::get)
//                .peek(r-> System.out.println("Found unique field: " + r))
                .findAny()
                //searching by unique constraints
                .orElseGet(() -> dao.getProfile().getUniqueConstraints().stream()
                                .map(UniqueConstraint::columnNames)
                                .map(columns -> findByUnique(columns, dao.getProfile().getValues(columns, entity)))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
//                        .peek(r-> System.out.println("Found unique constraint: " + r))
                                .findAny().orElse(null)
                );

        try {
            if (found == null) add(entity);
            else {
//                entity.setId(found.getId());
                dao.getProfile().setIdValue(entity, dao.getProfile().getIdValue(found));
                dao.getProfile().enrich(entity, found);
                update(entity);
            }
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(ID id) throws DAOException {
        try {
            dao.delete(id);
        } catch (SQLException e) {
            throw onSQLError(e, null, log);
        }
    }

    @Override
    public void deleteAll(String whereClause, Object... args) {
        try {
            dao.deleteAll(whereClause, args);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void refresh(T entity) throws DAOException {
        try {
            if (dao.refresh(entity) == 0)
                throw new DAOException("Record not found", null, DAOException.ErrorCode.RECORD_NOT_FOUND, entity);
        } catch (SQLException e) {
            throw onSQLError(e, null, log);
        }
    }

    @Override
    public Optional<T> findByUnique(String columnName, Object value) {
        try {
            return dao.findByUnique(columnName, value);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<T> findByUnique(String[] columnNames, Object... values) {
        try {
            return dao.findByUnique(columnNames, values);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<T> findAll(String whereClause, Object... args) {
        try {
            return dao.findAll(whereClause, args);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
