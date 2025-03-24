package com.jisj.orm;


import com.jisj.orm.function.ThrowingSupplier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class utils {
    public static Stream<ResultSet> streamOf(ResultSet resultSet) {
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED) {
            @Override
            public boolean tryAdvance(Consumer<? super ResultSet> action) {
                try {
                    if (!resultSet.next()) {
                        resultSet.close();
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                action.accept(resultSet);
                return true;
            }
        }, false);
    }

    public static <T> T sqlExWrap(final ThrowingSupplier<T, SQLException> supplier) {
        try {
            return supplier.get();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
