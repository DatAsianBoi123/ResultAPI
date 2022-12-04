import com.datasiqn.resultapi.Result;
import com.datasiqn.resultapi.UnwrapException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ResultTest {
    @Test
    public void testOkErr() {
        Assert.assertTrue(Result.ok().isOk());

        Assert.assertTrue(Result.error(0).isError());

        Assert.assertFalse(Result.error(1).isOk());

        Assert.assertFalse(Result.ok().isError());

        Assert.assertTrue(Result.ofNullable(12, 8).isOk());

        Assert.assertTrue(Result.ofNullable(null, 3).isError());

        Assert.assertTrue(Result.resolve(() -> Integer.parseInt("8"), exception -> 2).isOk());

        Assert.assertTrue(Result.resolve(() -> Double.parseDouble("hello"), exception -> 0).isError());
    }

    @Test
    public void testUnwrap() {
        Assert.assertNotNull(Result.ok().unwrap());

        Assert.assertThrows(UnwrapException.class, () -> Result.error(8).unwrap());
    }

    @Test
    public void testUnwrapOr() {
        Assert.assertEquals(5, (int) Result.ok(5).unwrapOr(2));

        Assert.assertEquals(8, (int) Result.error(2).unwrapOr(8));
    }

    @Test
    public void testUnwrapErr() {
        Assert.assertThrows(UnwrapException.class, () -> Result.ok().unwrapError());

        Assert.assertNotNull(Result.error(1).unwrapError());
    }

    @Test
    public void testIfs() {
        AtomicInteger int1 = new AtomicInteger(0);
        Result.ok().ifOk(none -> int1.set(9));
        Assert.assertEquals(9, int1.get());

        AtomicInteger int2 = new AtomicInteger(0);
        Result.error(8).ifError(int2::set);
        Assert.assertEquals(8, int2.get());
    }

    @Test
    public void testAnd() {
        // `ok` and `ok`
        Assert.assertEquals(Result.ok(2), Result.ok(1).and(Result.ok(2)));

        // `ok` and `err`
        Assert.assertEquals(Result.error(8), Result.ok(12).and(Result.error(8)));

        // `err` and `ok`
        Assert.assertEquals(Result.error(6), Result.error(6).and(Result.ok(2)));

        // `err` and `err`
        Assert.assertEquals(Result.error(2), Result.error(2).and(Result.error(8)));
    }

    @Test
    public void testAndThen() {
        // `ok` and `ok`
        Assert.assertEquals(Result.ok(3), Result.ok(1).andThen(num -> Result.ok(num + 2)));

        // `ok` and `err`
        Assert.assertEquals(Result.error(2), Result.ok(12).andThen(num -> Result.error(num - 10)));

        // `err` and `ok`
        Assert.assertEquals(Result.error(6), Result.<Integer, Integer>error(6).andThen(num -> Result.ok(num + 1)));

        // `err` and `err`
        Assert.assertEquals(Result.error(2), Result.<Integer, Integer>error(2).andThen(num -> Result.error(num - 3)));
    }

    @Test
    public void testOr() {
        // `ok` and `ok`
        Assert.assertEquals(Result.ok(1), Result.ok(1).or(Result.ok(2)));

        // `ok` and `err`
        Assert.assertEquals(Result.ok(12), Result.ok(12).or(Result.error(8)));

        // `err` and `ok`
        Assert.assertEquals(Result.ok(2), Result.error(6).or(Result.ok(2)));

        // `err` and `err`
        Assert.assertEquals(Result.error(8), Result.error(2).or(Result.error(8)));
    }

    @Test
    public void testOrElse() {
        // `ok` and `ok`
        Assert.assertEquals(Result.ok(1), Result.<Integer, Integer>ok(1).orElse(err -> Result.ok(err + 2)));

        // `ok` and `err`
        Assert.assertEquals(Result.ok(12), Result.<Integer, Integer>ok(12).orElse(err -> Result.error(err - 1)));

        // `err` and `ok`
        Assert.assertEquals(Result.ok(9), Result.error(6).orElse(err -> Result.ok(err + 3)));

        // `err` and `err`
        Assert.assertEquals(Result.error(0), Result.error(2).orElse(err -> Result.error(err - 2)));
    }

    @Test
    public void testMap() {
        // `ok` and `ok`
        Assert.assertEquals(Result.ok(2), Result.ok(1).map(val -> val + 1));

        // `ok` and `err`
        Assert.assertEquals(Result.ok(8), Result.ok(12).map(val -> val - 4));

        // `err` and `ok`
        Assert.assertEquals(Result.error(6), Result.<Integer, Integer>error(6).map(val -> val - 2));

        // `err` and `err`
        Assert.assertEquals(Result.error(2), Result.<Integer, Integer>error(2).map(val -> val + 2));
    }

    @Test
    public void testMapOr() {
        // `ok` and `ok`
        Assert.assertEquals(3, (int) Result.ok("hey").mapOr(String::length, 3));

        // `ok` and `err`
        Assert.assertEquals(6, (int) Result.ok("result").mapOr(String::length, 9));

        // `err` and `ok`
        Assert.assertEquals(3, (int) Result.<String, String>error("this is an error").mapOr(String::length, 3));

        // `err` and `err`
        Assert.assertEquals(2, (int) Result.<String, String>error("me when rust").mapOr(String::length, 2));
    }

    @Test
    public void testMapErr() {
        // `ok` and `ok`
        Assert.assertEquals(Result.ok("hey"), Result.<String, String>ok("hey").mapError(String::length));

        // `ok` and `err`
        Assert.assertEquals(Result.ok("result"), Result.<String, String>ok("result").mapError(String::length));

        // `err` and `ok`
        Assert.assertEquals(Result.error(16), Result.error("this is an error").mapError(String::length));

        // `err` and `err`
        Assert.assertEquals(Result.error(12), Result.<String, String>error("me when rust").mapError(String::length));
    }
}
