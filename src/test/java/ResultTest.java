import com.datasiqn.resultapi.ExpectException;
import com.datasiqn.resultapi.Result;
import com.datasiqn.resultapi.UnwrapException;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.*;

public class ResultTest {
    @Test
    public void testOkErr() {
        assertTrue(Result.ok().isOk());

        assertTrue(Result.error().isError());

        assertFalse(Result.error(1).isOk());

        assertFalse(Result.ok().isError());

        assertTrue(Result.ofNullable(12, 8).isOk());

        assertTrue(Result.ofNullable(null, 3).isError());

        assertTrue(Result.resolve(() -> Integer.parseInt("8"), exception -> 2).isOk());

        assertTrue(Result.resolve(() -> Double.parseDouble("hello"), exception -> 0).isError());

        assertTrue(Result.fromOptional(Optional.of("hello")).isOk());

        assertTrue(Result.fromOptional(Optional.empty()).isError());

        assertTrue(Result.fromErrorOptional(Optional.of(12)).isError());

        assertTrue(Result.fromErrorOptional(Optional.empty()).isOk());
    }

    @Test
    public void testAll() {
        assertEquals(Result.all(Result.ok("1"), Result.error("error"), Result.ok("2"), Result.ok("3"), Result.error("error")).size(), 3);

        assertEquals(Result.all(Result.error("error"), Result.error("another error")).size(), 0);
    }

    @Test
    public void testErrors() {
        assertEquals(Result.errors(Result.error("1"), Result.ok(), Result.ok(12), Result.error("2"), Result.error("3"), Result.error("4")).size(), 4);

        assertEquals(Result.errors(Result.ok(), Result.ok(10)).size(), 0);
    }

    @Test
    public void testExpect() {
        assertEquals(10, (int) Result.ok(10).expect(""));

        assertThrows("invalid: aaa", ExpectException.class, () -> Result.error("aaa").expect("invalid"));
    }

    @Test
    public void testExpectError() {
        assertThrows("shouldn't be ok: 10", ExpectException.class, () -> Result.ok(10).expectError("shouldn't be ok"));

        assertEquals("aaa", Result.error("aaa").expectError("invalid"));
    }

    @Test
    public void testUnwrap() {
        assertNotNull(Result.ok().unwrap());

        assertThrows("Attempted to call `Result#unwrap` on an Ok value", UnwrapException.class, () -> Result.error(8).unwrap());
    }

    @Test
    public void testUnwrapOr() {
        assertEquals(5, (int) Result.ok(5).unwrapOr(2));

        assertEquals(8, (int) Result.error(2).unwrapOr(8));
    }

    @Test
    public void testUnwrapOrError() {
        assertEquals(9, (long) Result.ok(9).unwrapOrThrow(new IllegalStateException("should be Ok")));

        assertThrows(IllegalArgumentException.class, () -> Result.error("he").unwrapOrThrow(new IllegalArgumentException()));

        assertEquals(9, (long) Result.ok(9).unwrapOrThrow((Function<Object, IllegalStateException>) err -> new IllegalStateException("should be Ok")));

        assertThrows(IllegalArgumentException.class, () -> Result.error("he").unwrapOrThrow((Function<String, IllegalArgumentException>) IllegalArgumentException::new));
    }

    @Test
    public void testUnwrapErr() {
        assertThrows("Attempted to call `Result#unwrapError` on an Error value", UnwrapException.class, () -> Result.ok().unwrapError());

        assertNotNull(Result.error(1).unwrapError());
    }

    @Test
    public void testIfs() {
        AtomicInteger int1 = new AtomicInteger(0);
        Result.ok().ifOk(none -> int1.set(9));
        assertEquals(9, int1.get());

        AtomicInteger int2 = new AtomicInteger(0);
        Result.error(8).ifError(int2::set);
        assertEquals(8, int2.get());
    }

    @Test
    public void testAnd() {
        // `ok` and `ok`
        assertEquals(Result.ok(2), Result.ok(1).and(Result.ok(2)));

        // `ok` and `err`
        assertEquals(Result.error(8), Result.ok(12).and(Result.error(8)));

        // `err` and `ok`
        assertEquals(Result.error(6), Result.error(6).and(Result.ok(2)));

        // `err` and `err`
        assertEquals(Result.error(2), Result.error(2).and(Result.error(8)));
    }

    @Test
    public void testAndThen() {
        // `ok` and `ok`
        assertEquals(Result.ok(3), Result.ok(1).andThen(num -> Result.ok(num + 2)));

        // `ok` and `err`
        assertEquals(Result.error(2), Result.ok(12).andThen(num -> Result.error(num - 10)));

        // `err` and `ok`
        assertEquals(Result.error(6), Result.<Integer, Integer>error(6).andThen(num -> Result.ok(num + 1)));

        // `err` and `err`
        assertEquals(Result.error(2), Result.<Integer, Integer>error(2).andThen(num -> Result.error(num - 3)));
    }

    @Test
    public void testOr() {
        // `ok` and `ok`
        assertEquals(Result.ok(1), Result.ok(1).or(Result.ok(2)));

        // `ok` and `err`
        assertEquals(Result.ok(12), Result.ok(12).or(Result.error(8)));

        // `err` and `ok`
        assertEquals(Result.ok(2), Result.error(6).or(Result.ok(2)));

        // `err` and `err`
        assertEquals(Result.error(8), Result.error(2).or(Result.error(8)));
    }

    @Test
    public void testOrElse() {
        // `ok` and `ok`
        assertEquals(Result.ok(1), Result.<Integer, Integer>ok(1).orElse(err -> Result.ok(err + 2)));

        // `ok` and `err`
        assertEquals(Result.ok(12), Result.<Integer, Integer>ok(12).orElse(err -> Result.error(err - 1)));

        // `err` and `ok`
        assertEquals(Result.ok(9), Result.error(6).orElse(err -> Result.ok(err + 3)));

        // `err` and `err`
        assertEquals(Result.error(0), Result.error(2).orElse(err -> Result.error(err - 2)));
    }

    @Test
    public void testMap() {
        // `ok` and `ok`
        assertEquals(Result.ok(2), Result.ok(1).map(val -> val + 1));

        // `ok` and `err`
        assertEquals(Result.ok(8), Result.ok(12).map(val -> val - 4));

        // `err` and `ok`
        assertEquals(Result.error(6), Result.<Integer, Integer>error(6).map(val -> val - 2));

        // `err` and `err`
        assertEquals(Result.error(2), Result.<Integer, Integer>error(2).map(val -> val + 2));
    }

    @Test
    public void testMapOr() {
        // `ok` and `ok`
        assertEquals(3, (int) Result.ok("hey").mapOr(String::length, 3));

        // `ok` and `err`
        assertEquals(6, (int) Result.ok("result").mapOr(String::length, 9));

        // `err` and `ok`
        assertEquals(3, (int) Result.<String, String>error("this is an error").mapOr(String::length, 3));

        // `err` and `err`
        assertEquals(2, (int) Result.<String, String>error("me when rust").mapOr(String::length, 2));
    }

    @Test
    public void testMapErr() {
        // `ok` and `ok`
        assertEquals(Result.ok("hey"), Result.<String, String>ok("hey").mapError(String::length));

        // `ok` and `err`
        assertEquals(Result.ok("result"), Result.<String, String>ok("result").mapError(String::length));

        // `err` and `ok`
        assertEquals(Result.error(16), Result.error("this is an error").mapError(String::length));

        // `err` and `err`
        assertEquals(Result.error(12), Result.<String, String>error("me when rust").mapError(String::length));
    }

    @Test
    public void testToOptional() {
        assertEquals(Optional.of("thing"), Result.ok("thing").toOptional());

        assertEquals(Optional.empty(), Result.error("error").toOptional());

        assertEquals(Optional.of("error"), Result.error("error").toErrorOptional());

        assertEquals(Optional.empty(), Result.ok().toErrorOptional());
    }
}
