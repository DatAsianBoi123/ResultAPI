package com.datasiqn.resultapi;

/**
 * Represents an exception that occurs when an illegal use of {@link Result#unwrap()} or {@link Result#unwrapError()} is called.
 * <br><br>
 * Throws "Attempted to call `Result#unwrap` on an err value" if you are calling {@link Result#unwrap()} on an {@code Error} result
 * <br>
 * Throws "Attempted to call `Result#unwrapError` on an ok value" if you are calling {@link Result#unwrapError()} on an {@code Ok} result
 */
public class UnwrapException extends RuntimeException {
    public UnwrapException(String call, String valType) {
        super(String.format("Attempted to call `Result#%s` on an %s value", call, valType));
    }
}