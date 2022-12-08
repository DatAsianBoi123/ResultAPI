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

    /**
     * Matches an {@code Ok} value or an {@code Error} value
     * <pre>
     *     {@code
     *
     *     Result.ok("world").match(val -> System.out.println("hello " + val), err -> System.out.println("I got an error"));
     *     // Prints out `hello world`
     *
     *     Result.error("uh oh!").match(val -> System.out.println("hello " + val), err -> System.out.println("I got an error"));
     *     // Prints out `I got an error`
     *
     *     }
     * </pre>
     * @param okConsumer The consumer to be called if this is {@code Ok}
     * @param errorConsumer The consumer to be called if this is {@code Error}
     */
    public void match(Consumer<V> okConsumer, Consumer<E> errorConsumer) {
        if (isOk()) okConsumer.accept(value);
        else errorConsumer.accept(error);
    }

    /**
     * Matches an {@code Ok} value or an {@code Error} value and returns the result of that function
     * <pre>
     *     {@code
     *
     *     int value = Result.ok("bob").matchResult(String::length, err -> 8);
     *     // `value` is `3`
     *
     *     int value = Result.<String, String>error("hello").matchResult(String::length, err -> 2);
     *     // `value` is 2
     *
     *     }
     * </pre>
     * @param okFunction The function to be called if this is {@code Ok}
     * @param errorFunction The function to be called if this is {@code Error}
     * @return The result of the called function
     * @param <T> The return type
     */
    public <T> T matchResult(Function<V, T> okFunction, Function<E, T> errorFunction) {
        if (isOk()) return okFunction.apply(value);
        return errorFunction.apply(error);
    }

    /**
     * Returns if this is {@code Ok}
     * @return True if this is {@code Ok}, false otherwise
     */
    public boolean isOk() {
        return !caughtError;
    }

    /**
     * Returns if this is {@code Error}
     * @return True if this is {@code Error}, false otherwise
     */
    public boolean isError() {
        return caughtError;
    }

    /**
     * Returns the contained {@code Ok} value
     * @return The {@code Ok} value
     * @throws UnwrapException If this is {@code Error}
     */
    public V unwrap() {
        if (isOk()) return value;
        throw new UnwrapException("unwrap", "err");
    }

    /**
     * Returns the contained {@code Ok} value, or {@code defaultValue} if this is {@code Error}
     * @param defaultValue The default value if this is {@code Error}
     * @return The contained {@code Ok} value, or {@code defaultValue} if this is {@code Error}
     */
    public V unwrapOr(V defaultValue) {
        return matchResult(value -> this.value, error -> defaultValue);
    }

    /**
     * Returns the contained {@code Error} value
     * @return The {@code Error} value
     * @throws UnwrapException If this is {@code Ok}
     */
    public E unwrapError() {
        if (isError()) return error;
        throw new UnwrapException("unwrapError", "ok");
    }

    /**
     * Calls {@code consumer} if this is {@code Ok}
     * @param consumer The consumer to be called if this is {@code Ok}
     */
    public void ifOk(Consumer<V> consumer) {
        match(consumer, error -> {});
    }

    /**
     * Calls {@code consumer} if this is {@code Error}
     * @param consumer The consumer to be called if this is {@code Error}
     */
    public void ifError(Consumer<E> consumer) {
        match(value -> {}, consumer);
    }

    /**
     * Returns {@code result} if this is {@code Ok}, otherwise returns {@code this}
     * <pre>
     *     {@code
     *
     *      Result<None, Object> result = Result.ok().and(Result.error("yes"));
     *      // `result` is Result.error("yes");
     *
     *      Result<None, String> result = Result.error("no").and(Result.ok());
     *      // `result` is Result.error("no");
     *
     *     }
     * </pre>
     * @param result The result to be returned if this is {@code Ok}
     * @return {@code result} if this is {@code Ok}, otherwise {@code this}
     * @param <N> The type of the new {@code Ok} value
     */
    public <N> Result<N, E> and(Result<N, E> result) {
        return matchResult(value -> result, Result::error);
    }

    /**
     * Returns the result of {@code function} if this is {@code Ok}, otherwise returns {@code this}
     * <pre>
     *     {@code
     *
     *      Result<Integer, Object> result = Result.ok("hello").andThen(val -> Result.error(val.length()));
     *      // `result` is Result.error(5);
     *
     *      Result<Integer, String> result = Result.<String, String>error("world").andThen(val -> Result.ok(val.length()));
     *      // `result` is Result.error("world");
     *     }
     * </pre>
     * @param function The function to be called if this is {@code Ok}
     * @return The result of {@code function} if this is {@code Ok}, otherwise returns {@code this}
     * @param <N> The type of the new {@code Ok} value
     */
    public <N> Result<N, E> andThen(Function<V, Result<N, E>> function) {
        return matchResult(function, Result::error);
    }

    /**
     * Returns {@code result} if this is {@code Error}, otherwise returns {@code this}
     * <pre>
     *     {@code
     *
     *     Result<String, Integer> result = Result.ok("results").or(Result.ok("new result"));
     *     // `result` is Result.ok("results");
     *
     *     Result<Object, String> result = Result.error(12).or(Result.error("new error"));
     *     // `result` is Result.error("new error");
     *
     *     }
     * </pre>
     * @param result The result to be returned if this is {@code Error}
     * @return {@code result} if this is {@code Error}, otherwise returns {@code this}
     * @param <N> The type of the new {@code Error} value
     */
    public <N> Result<V, N> or(Result<V, N> result) {
        return matchResult(Result::ok, error -> result);
    }

    /**
     * Returns the result of {@code function} if this is {@code Error}, otherwise returns {@code this}
     * <pre>
     *     {@code
     *
     *     Result<String, Integer> result = Result.<String, String>ok("this is ok").orElse(err -> Result.error(err.length()));
     *     // `result` is Result.ok("this is ok");
     *
     *     Result<Character, Integer> result = Result.<Character, Integer>error(65).orElse(err -> Result.ok((char) (int) err));
     *     // `result` is Result.ok('A');
     *
     *     }
     * </pre>
     * @param function The function to be called if this is {@code Error}
     * @return The result of {@code function} if this is {@code Error}, otherwise returns {@code this}
     * @param <N> The type of the new {@code Error} value
     */
    public <N> Result<V, N> orElse(Function<E, Result<V, N>> function) {
        return matchResult(Result::ok, function);
    }

    /**
     * Maps this by applying {@code mapper} to the contained {@code Ok} value
     * <pre>
     *     {@code
     *
     *     String strings = "1,2,joe,4";
     *
     *     for (String string : strings.split(",")) {
     *         Result.resolve(() -> Integer.valueOf(string), e -> e).map(num -> num * 2).ifOk(System.out::println);
     *     }
     *     // Prints out "2" "4" "8"
     *
     *     }
     * </pre>
     * @param mapper The mapper
     * @return An {@code Ok} {@code Result} containing the result of {@code mapper} if this is {@code Ok}, otherwise returns {@code this}
     * @param <N> The type of the new {@code Ok} value
     */
    public <N> Result<N, E> map(Function<V, N> mapper) {
        return matchResult(value -> ok(mapper.apply(value)), Result::error);
    }

    /**
     * Maps this by applying {@code mapper} to the container {@code Ok} value if this is {@code Ok}, otherwise returns {@code defaultValue}
     * <pre>
     *     {@code
     *
     *     String strings = "3,2,joe,7";
     *
     *     for (String string : strings.split(",")) {
     *         System.out.println(Result.resolve(() -> Integer.valueOf(string), e -> e).mapOr(num -> num + 2, 0));
     *     }
     *     // Prints out "5" "4" "0" "9"
     *
     *     }
     * </pre>
     * @param mapper The mapper
     * @param defaultValue The value to return if this is {@code Error}
     * @return The result of {@code mapper} if this is {@code Ok}, otherwise returns {@code defaultValue}
     * @param <N> The type of the mapped value
     */
    public <N> N mapOr(Function<V, N> mapper, N defaultValue) {
        return matchResult(mapper, error -> defaultValue);
    }

    /**
     * Maps the contained {@code Error} value by applying {@code mapper} to the contained {@code Error} value
     * <pre>
     *     {@code
     *
     *     Function<Integer, String> formatError = err -> "Error code: " + err;
     *
     *     Result<String, String> result = Result.<String, Integer>ok("yes").mapError(formatError);
     *     // `result` is `Result.ok("yes");
     *
     *     Result<String, String> result = Result.<String, Integer>error(12).mapError(formatError);
     *     // `result` is `Result.error("Error code: 12")`
     *
     *     }
     * </pre>
     * @param mapper The mapper
     * @return An {@code Error} {@code Result} containing the result of {@code mapper} if this is {@code Error}, otherwise returns {@code this}
     * @param <N> The type of the new {@code Error} value
     */
    public <N> Result<V, N> mapError(Function<E, N> mapper) {
        return matchResult(Result::ok, error -> error(mapper.apply(error)));
    }

    @Override
    public String toString() {
        return "Result(" + (isOk() ? "Ok" : "Error") + ") " + (isOk() ? unwrap() : unwrapError());
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

    /**
     * Creates a new {@code Result} with an {@code Ok} value of NONE
     * @return The created {@code Result} instance
     * @param <E> The type of the {@code Error} value
     */
    public static <E> @NotNull Result<None, E> ok() {
        return ok(None.NONE);
    }

    /**
     * Creates a new {@code Result} with an {@code Ok} value of {@code value}
     * @param value The {@code Ok} value of the {@code Result}
     * @return The created {@code Result} instance
     * @param <V> The type of the {@code Ok} value
     * @param <E> The type of the {@code Error} value
     */
    @Contract(value = "_ -> new", pure = true)
    public static <V, E> @NotNull Result<V, E> ok(V value) {
        return new Result<>(value, null);
    }

    /**
     * Creates a new {@code Result} with an {@code Error} value of {@code error}
     * @param error The {@code Error} value of the {@code Result}
     * @return The created {@code Result} instance
     * @param <V> The type of the {@code Ok} value
     * @param <E> The type of the {@code Error} value
     */
    @Contract(value = "_ -> new", pure = true)
    public static <V, E> @NotNull Result<V, E> error(E error) {
        return new Result<>(null, error);
    }

    /**
     * Creates a new {@code Result} with an {@code Ok} value if {@code value} is not null, otherwise creates a new {@code Result} with an {@code Error} value of {@code error}
     * @param value The value to test
     * @param error The error value to return if {@code value} is null
     * @return The created {@code Result} instance
     * @param <V> The type of the {@code Ok} value
     * @param <E> The type of the {@code Error} value
     */
    public static <V, E> @NotNull Result<V, E> ofNullable(@Nullable V value, E error) {
        if (value == null) return error(error);
        return ok(value);
    }

    /**
     * Creates a new {@code Result} with the result of {@code supplier}. If {@code supplier} throws an exception, it creates a new {@code Result} with an {@code Error} value of the result of {@code errorMapper}
     * @param supplier The supplier to resolve
     * @param errorMapper The function to map the exception that {@code supplier} may throw
     * @return The created {@code Result} instance
     * @param <V> The type of the {@code Ok} value
     * @param <E> The type of the {@code Error} value
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static <V, E> Result<V, E> resolve(ValueSupplier<V> supplier, Function<Exception, E> errorMapper) {
        try {
            return ok(supplier.getValue());
        } catch (Exception e) {
            return error(errorMapper.apply(e));
        }
    }

    /**
     * A supplier that throws an exception
     * @param <T> The type of the return value
     */
    @FunctionalInterface
    public interface ValueSupplier<T> {
        /**
         * Gets the value
         * @return The value
         * @throws Exception If an exception occurs
         */
        T getValue() throws Exception;
    }
}
