/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.descriptive;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * A mutable 128-bit unsigned integer.
 *
 * <p>This is a specialised class to implement an accumulator of {@code long} values
 * generated by squaring {@code int} values.
 *
 * @since 1.1
 */
final class UInt128 {
    /** Mask for the lower 32-bits of a long. */
    private static final long MASK32 = 0xffff_ffffL;

    // Data is stored using integers to allow efficient sum-with-carry addition

    /** bits 32-1 (low 32-bits). */
    private int d;
    /** bits 64-33. */
    private int c;
    /** bits 128-65. */
    private long ab;

    /**
     * Create an instance.
     */
    private UInt128() {
        // No-op
    }

    /**
     * Create an instance using a direct binary representation.
     *
     * @param hi High 64-bits.
     * @param mid Middle 32-bits.
     * @param lo Low 32-bits.
     */
    private UInt128(long hi, int mid, int lo) {
        this.d = lo;
        this.c = mid;
        this.ab = hi;
    }

    /**
     * Create an instance using a direct binary representation.
     * This is package-private for testing.
     *
     * @param hi High 64-bits.
     * @param lo Low 64-bits.
     */
    UInt128(long hi, long lo) {
        this.d = (int) lo;
        this.c = (int) (lo >>> Integer.SIZE);
        this.ab = hi;
    }

    /**
     * Create an instance. The initial value is zero.
     *
     * @return the instance
     */
    static UInt128 create() {
        return new UInt128();
    }

    /**
     * Create an instance of the {@code UInt96} value.
     *
     * @param x Value.
     * @return the instance
     */
    static UInt128 of(UInt96 x) {
        final int lo = x.lo32();
        final long hi = x.hi64();
        final UInt128 y = new UInt128();
        y.d = lo;
        y.c = (int) hi;
        y.ab = hi >>> Integer.SIZE;
        return y;
    }

    /**
     * Adds the value in place. It is assumed to be positive, for example the square of an
     * {@code int} value. However no check is performed for a negative value.
     *
     * <p>Note: This addition handles {@value Long#MIN_VALUE} as an unsigned
     * value of 2^63.
     *
     * @param x Value.
     */
    void addPositive(long x) {
        // Sum with carry.
        // Assuming x is positive then x + lo will not overflow 64-bits
        // so we do not have to split x into upper and lower 32-bit values.
        long s = x + (d & MASK32);
        d = (int) s;
        s = (s >>> Integer.SIZE) + (c & MASK32);
        c = (int) s;
        ab += s >>> Integer.SIZE;
    }

    /**
     * Adds the value in-place.
     *
     * @param x Value.
     */
    void add(UInt128 x) {
        // Avoid issues adding to itself
        final int dd = x.d;
        final int cc = x.c;
        final long aabb = x.ab;
        // Sum with carry.
        long s = (dd & MASK32) + (d & MASK32);
        d = (int) s;
        s = (s >>> Integer.SIZE) + (cc & MASK32) + (c & MASK32);
        c = (int) s;
        ab += (s >>> Integer.SIZE) + aabb;
    }

    /**
     * Multiply by the unsigned value.
     * Any overflow bits are lost.
     *
     * @param x Value.
     * @return the product
     */
    UInt128 unsignedMultiply(int x) {
        final long xx = x & MASK32;
        // Multiply with carry.
        long product = xx * (d & MASK32);
        final int dd = (int) product;
        product = (product >>> Integer.SIZE) + xx * (c & MASK32);
        final int cc = (int) product;
        // Possible overflow here and bits are lost
        final long aabb = (product >>> Integer.SIZE) + xx * ab;
        return new UInt128(aabb, cc, dd);
    }

    /**
     * Subtracts the value.
     * Any overflow bits (negative result) are lost.
     *
     * @param x Value.
     * @return the difference
     */
    UInt128 subtract(UInt128 x) {
        // Difference with carry.
        long diff = (d & MASK32) - (x.d & MASK32);
        final int dd = (int) diff;
        diff = (diff >> Integer.SIZE) + (c & MASK32) - (x.c & MASK32);
        final int cc = (int) diff;
        // Possible overflow here and bits are lost containing info on the
        // magnitude of the true negative value
        final long aabb = (diff >> Integer.SIZE) + ab - x.ab;
        return new UInt128(aabb, cc, dd);
    }

    /**
     * Convert to a BigInteger.
     *
     * @return the value
     */
    BigInteger toBigInteger() {
        // Test if we have more than 63-bits
        if (ab != 0 || c < 0) {
            return new BigInteger(1, ByteBuffer.allocate(Integer.BYTES * 4)
                .putLong(ab)
                .putInt(c)
                .putInt(d).array());
        }
        // Create from a long
        return BigInteger.valueOf(lo64());
    }

    /**
     * Convert to a {@code double}.
     *
     * @return the value
     */
    double toDouble() {
        return IntMath.uin128ToDouble(hi64(), lo64());
    }

    /**
     * Return the lower 64-bits as a {@code long} value.
     *
     * @return bits 64-1
     */
    long lo64() {
        return (d & MASK32) | ((c & MASK32) << Integer.SIZE);
    }

    /**
     * Return the low 32-bits as an {@code int} value.
     *
     * @return bits 32-1
     */
    int lo32() {
        return d;
    }

    /**
     * Return the middle 32-bits as an {@code int} value.
     *
     * @return bits 64-33
     */
    int mid32() {
        return c;
    }

    /**
     * Return the higher 64-bits as a {@code long} value.
     *
     * @return bits 128-65
     */
    long hi64() {
        return ab;
    }
}
