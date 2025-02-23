package org.mikesoft.orm.function;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
}
