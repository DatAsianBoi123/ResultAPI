package com.datasiqn.resultapi;

/**
 * Represents no value
 */
public final class None {
    @SuppressWarnings("InstantiationOfUtilityClass")
    public static final None NONE = new None();

    private static boolean instantiated = false;

    private None() {
        if (instantiated) throw new IllegalStateException("Do not instantiate None");

        instantiated = true;
    }
}
