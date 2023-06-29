package com.datasiqn.resultapi;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
     * Returns the contained {@code Ok} value if this is {@code Ok}, otherwise throws an {@link ExpectException}
     * <pre>
     *     {@code
     *
     *     String string = Result.ok("hello").expect("Result should be ok");
     *     // `string` is "hello"
     *
     *     int integer = Result.error("asdfasdf").expect("String should be valid integer");
     *     // throws ExpectException: "String should be a valid integer: asdfasdf"
     *
     *     }
     * </pre>
     * @param message The error message to be displayed if this is {@code Error}
     * @return The contained {@code Ok} value
     * @throws ExpectException If this is {@code Error}, with the message `{@code message}: {@code Error}`
     */
    public V expect(String message) {
        if (isError()) throw new ExpectException(message, error);
        return value;
    }

    /**
     * Returns the contained {@code Error} value if this is {@code Error}, otherwise throws an {@link ExpectException}
     * <pre>
     *     {@code
     *
     *     String string = Result.ok("hello").expectError("Result should be error");
     *     // throws ExpectException: "Result should be error: hello"
     *
     *     String string = Result.error("asdfasdf").expectError("String should be invalid integer");
     *     // `string` is "asdfasdf"
     *
     *     }
     * </pre>
     * @param message The error message to be displayed if this is {@code Ok}
     * @return The contained {@code Error} value
     * @throws ExpectException If this is {@code Ok}, with the message `{@code message}: {@code Ok}`
     */
    public E expectError(String message) {
        if (isOk()) throw new ExpectException(message, value);
        return error;
    }

    /**
     * Returns the contained {@code Ok} value. In most cases you should check to make sure this is {@code Ok}.
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
     * Returns the contained {@code Error} value. In most cases you should check to make sure this is {@code Error}.
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

    /**
     * Turns this into an {@code Optional}. If this is {@code Ok}, then this will return a new {@code Optional} containing the contained {@code Ok} value.
     * If this is {@code Error}, then this will return an empty {@code Optional}.
     * <pre>
     *     {@code
     *
     *     Optional<String> optional = Result.ok("thing").toOptional();
     *     // `optional` is Optional[thing]
     *
     *
     *     Optional<String> optional = Result.error("uh oh").toOptional());
     *     // `optional` is Optional.empty
     *
     *     }
     * </pre>
     * @return The newly constructed {@code Optional} instance
     */
    @Contract(pure = true)
    public Optional<V> toOptional() {
        if (isOk()) return Optional.of(value);
        return Optional.empty();
    }

    /**
     * Turns this into an {@code Optional}. If this is {@code Ok}, then this will return an empty {@code Optional}.
     * If this is {@code Error}, then this will return a new {@code Optional} containing the contained {@code Error} value.
     * <pre>
     *     {@code
     *
     *     Optional<String> optional = Result.ok("ok").toErrorOptional();
     *     // `optional` is Optional.empty
     *
     *     Optional<String> optional = Result.error("this is an error").toErrorOptional();
     *     // `optional` is Optional[this is an error]
     *
     *     }
     * </pre>
     * @return The newly constructed {@code Optional} instance
     */
    public Optional<E> toErrorOptional() {
        if (isError()) return Optional.of(error);
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "Result(" + (isOk() ? "Ok" : "Error") + ") = " + (isOk() ? unwrap() : unwrapError());
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
     * Creates a new {@code Result} with an {@code Ok} value of {@code None}
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
    public static <V, E> @NotNull Result<V, E> ok(@NotNull V value) {
        return new Result<>(value, null);
    }

    /**
     * Creates a new {@code Result} with an {@code Error} value of {@code None}
     * @return The created {@code Result} instance
     * @param <V> The type of the {@code Ok} value
     */
    public static <V> @NotNull Result<V, None> error() {
        return error(None.NONE);
    }

    /**
     * Creates a new {@code Result} with an {@code Error} value of {@code error}
     * @param error The {@code Error} value of the {@code Result}
     * @return The created {@code Result} instance
     * @param <V> The type of the {@code Ok} value
     * @param <E> The type of the {@code Error} value
     */
    @Contract(value = "_ -> new", pure = true)
    public static <V, E> @NotNull Result<V, E> error(@NotNull E error) {
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
    public static <V, E> @NotNull Result<V, E> ofNullable(@Nullable V value, @NotNull E error) {
        if (value == null) return error(error);
        return ok(value);
    }

    /**
     * Creates a new {@code Result} with an {@code Ok} value containing the inner value of {@code optional} if {@code optional} contains a value,
     * otherwise creates a new {@code Result} with an {@code Error} value of {@code None}
     * @param optional The optional value
     * @return The newly created {@code Result} instance
     * @param <V> The type of the {@code Ok} value
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <V> @NotNull Result<V, None> fromOptional(@NotNull Optional<V> optional) {
        return optional.<Result<V, None>>map(Result::ok).orElse(Result.error());
    }

    /**
     * Creates a new {@code Result} with an {@code Error} value containing the inner value of {@code optional} if {@code optional} contains a value,
     * otherwise creates a new {@code Result} with an {@code Ok} value of {@code None}
     * @param optional The optional value
     * @return The newly created {@code Result} instance
     * @param <E> The type of the {@code Error} value
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <E> @NotNull Result<None, E> fromErrorOptional(@NotNull Optional<E> optional) {
        return optional.<Result<None, E>>map(Result::error).orElse(Result.ok());
    }

    /**
     * Creates a new {@code Result} with an {@code Ok} value of {@code supplier}. If {@code supplier} throws an exception, it creates a new {@code Result} with an {@code Error} value of the result of {@code errorMapper}
     * @param supplier The supplier to resolve
     * @param errorMapper The function to map the exception that {@code supplier} may throw
     * @return The newly created {@code Result} instance
     * @param <V> The type of the {@code Ok} value
     * @param <E> The type of the {@code Error} value
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static <V, E> Result<V, E> resolve(ValueSupplier<V> supplier, Function<Throwable, E> errorMapper) {
        try {
            return ok(supplier.getValue());
        } catch (Throwable e) {
            return error(errorMapper.apply(e));
        }
    }

    /**
     * Creates a new {@code Result} with an {@code Ok} value of {@code supplier}. If {@code supplier} throws an exception, it creates a new {@code Result} with an {@code Error} value of {@code None}
     * @param supplier The supplier to resolve
     * @return The newly created {@code Result} value
     * @param <V> The type of the {@code Ok} value
     */
    public static <V> Result<V, None> resolve(ValueSupplier<V> supplier) {
        try {
            return ok(supplier.getValue());
        } catch (Throwable e) {
            return error();
        }
    }

    /**
     * Returns all {@code Ok} values in {@code results}
     * <pre>
     *     {@code
     *
     *     Collection<String> all = Result.all(Result.ok("hey"), Result.ok("there"), Result.error("bad result"), Result.ok("guys"));
     *     // `all` is "hey", "there", "guys"
     *
     *     }
     * </pre>
     * @param results The results to loop through
     * @return All {@code Ok} values in {@code results}
     * @param <V> The {@code Ok} type
     */
    @SafeVarargs
    public static <V> @NotNull Collection<V> all(Result<V, ?> @NotNull ... results) {
        List<V> values = new ArrayList<>();
        for (Result<V, ?> result : results) result.match(values::add, e -> {});
        return values;
    }

    /**
     * Returns all {@code Error} values in {@code results}
     * <pre>
     *     {@code
     *
     *     Collection<String> errors = Result.errors(Result.error("error"), Result.ok(10), Result.error("three"), Result.error("hello"));
     *     // `errors` is "error", "three", "hello
     *
     *     }
     * </pre>
     * @param results The results to loop through
     * @return All {@code Error} values in {@code results}
     * @param <E> The {@code Error} type
     */
    @SafeVarargs
    public static <E> @NotNull Collection<E> errors(Result<?, E> @NotNull ... results) {
        List<E> errors = new ArrayList<>();
        for (Result<?, E> result : results) {
            if (result.isOk()) continue;
            errors.add(result.unwrapError());
        }
        return errors;
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
         * @throws Throwable If an exception occurs
         */
        T getValue() throws Throwable;
    }
}
