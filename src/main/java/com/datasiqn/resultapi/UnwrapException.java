package com.datasiqn.resultapi;

public class UnwrapException extends Exception {
    public UnwrapException(String call, String valType) {
        super(String.format("Attempted to call `Result::%s` on an %s value", call, valType));
    }
}