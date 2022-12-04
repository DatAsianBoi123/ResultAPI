package com.datasiqn.resultapi;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a result of a method
 * @param <V> The type of the {@code Ok} value
 * @param <E> The type of the {@code Error} value
 */
public class Result<V, E> {
    private final V value;
    private final E error;
    private final boolean caughtError;

    private Result(V value, E error) {
        this.value = value;
        this.error = error;
        caughtError = error != null;
    }

    public void match(Consumer<V> okConsumer, Consumer<E> errorConsumer) {
        if (isOk()) okConsumer.accept(value);
        else errorConsumer.accept(error);
    }

    public <T> T matchResult(Function<V, T> okFunction, Function<E, T> errorFunction) {
        if (isOk()) return okFunction.apply(value);
        return errorFunction.apply(error);
    }

    public boolean isOk() {
        return !caughtError;
    }

    public boolean isError() {
        return caughtError;
    }

    public V unwrap() {
        if (isOk()) return value;
        throw new UnwrapException("unwrap", "err");
    }

    public V unwrapOr(V defaultValue) {
        return matchResult(value -> this.value, error -> defaultValue);
    }

    public E unwrapError() {
        if (isError()) return error;
        throw new UnwrapException("unwrapError", "ok");
    }

    public void ifOk(Consumer<V> consumer) {
        match(consumer, error -> {});
    }

    public void ifError(Consumer<E> consumer) {
        match(value -> {}, consumer);
    }

    public <N> Result<N, E> and(Result<N, E> result) {
        return matchResult(value -> result, Result::error);
    }

    public <N> Result<N, E> andThen(Function<V, Result<N, E>> function) {
        return matchResult(function, Result::error);
    }

    public <N> Result<V, N> or(Result<V, N> result) {
        return matchResult(Result::ok, error -> result);
    }

    public <N> Result<V, N> orElse(Function<E, Result<V, N>> function) {
        return matchResult(Result::ok, function);
    }

    public <N> Result<N, E> map(Function<V, N> mapper) {
        return matchResult(value -> ok(mapper.apply(value)), Result::error);
    }

    public <N> N mapOr(Function<V, N> mapper, N defaultValue) {
        return matchResult(mapper, error -> defaultValue);
    }

    public <N> Result<V, N> mapError(Function<E, N> mapper) {
        return matchResult(Result::ok, error -> error(mapper.apply(error)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Result<?, ?> result = (Result<?, ?>) o;

        if (caughtError != result.caughtError) return false;
        if (caughtError) return error.equals(result.error);
        else return value.equals(result.value);
    }

    public static <E> @NotNull Result<None, E> ok() {
        return ok(None.NONE);
    }
    @Contract(value = "_ -> new", pure = true)
    public static <V, E> @NotNull Result<V, E> ok(V value) {
        return new Result<>(value, null);
    }

    @Contract(value = "_ -> new", pure = true)
    public static <V, E> @NotNull Result<V, E> error(E error) {
        return new Result<>(null, error);
    }

    public static <V, E> @NotNull Result<V, E> ofNullable(@Nullable V value, E error) {
        if (value == null) return error(error);
        return ok(value);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static <V, E> Result<V, E> resolve(ValueSupplier<V> supplier, Function<Exception, E> errorMapper) {
        try {
            return ok(supplier.getValue());
        } catch (Exception e) {
            return error(errorMapper.apply(e));
        }
    }

    @FunctionalInterface
    public interface ValueSupplier<T> {
        T getValue() throws Exception;
    }
}
