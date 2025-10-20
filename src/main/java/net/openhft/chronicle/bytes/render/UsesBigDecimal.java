/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.render;

import net.openhft.chronicle.core.Jvm;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Decimaliser based on {@link BigDecimal} for high precision conversions.
 *
 * <p>Using {@code BigDecimal} allocates objects and may be slower than the
 * lightweight strategies. Reflection is used to access the internal
 * {@code intCompact} field when available; this may break on future JVMs or
 * under a security manager. The code falls back to {@link BigDecimal#unscaledValue()} if reflective access fails.
 */
public class UsesBigDecimal implements Decimaliser {

    /**
     * A singleton instance of {@link UsesBigDecimal} for convenient reuse.
     * This instance is thread-safe and can be used across multiple threads without synchronization.
     */
    public static final Decimaliser USES_BIG_DECIMAL = new UsesBigDecimal();

    /**
     * Reference to the private {@code intCompact} field of {@link BigDecimal}.
     * Access may fail on some JVMs, in which case a slower fallback is used.
     */
    private static final java.lang.reflect.Field INT_COMPACT =
            Jvm.getFieldOrNull(BigDecimal.class, "intCompact");

    /**
     * Constant representing the bits of negative zero in floating point representation.
     */
    private static final long NEGATIVE_ZERO_BITS = Long.MIN_VALUE;

    /**
     * Converts a double value to its decimal representation using {@link BigDecimal} and appends it
     * to the provided {@link DecimalAppender}.
     * <p>
     * If the input is not a finite number or is a negative zero, the conversion will not be performed.
     *
     * @param value           The double value to be converted.
     * @param decimalAppender The {@link DecimalAppender} used to store and append the converted decimal value.
     * @return {@code true} if the conversion and appending were successful, {@code false} otherwise.
     */
    public boolean toDecimal(double value, DecimalAppender decimalAppender) {
        // Check for non-finite values or negative zero
        if (!Double.isFinite(value) || Double.doubleToLongBits(value) == NEGATIVE_ZERO_BITS)
            return false;

        // Convert the double to BigDecimal for high precision representation
        BigDecimal bd = BigDecimal.valueOf(value);
        int exp = bd.scale();

        try {
            if (INT_COMPACT == null) {
                // This block is a fallback for JVM implementations where BigDecimal doesn't have an 'intCompact' field.
                BigInteger bi = bd.unscaledValue();
                long l = bi.longValueExact();
                decimalAppender.append(l < 0, Math.abs(l), exp);
                return true;

            } else {
                // Use reflection to access internal long representation of BigDecimal if possible.
                long l = INT_COMPACT.getLong(bd);
                if (l != NEGATIVE_ZERO_BITS) {
                    decimalAppender.append(l < 0, Math.abs(l), exp);
                    return true;
                }
            }
        } catch (ArithmeticException | IllegalAccessException ae) {
            // Fall back in case of exception.
        }

        return false;
    }

    /**
     * Converts a float value to its decimal representation using {@link BigDecimal} and appends it
     * to the provided {@link DecimalAppender}.
     * <p>
     * If the input is not a finite number, the conversion will not be performed.
     *
     * @param value           The float value to be converted.
     * @param decimalAppender The {@link DecimalAppender} used to store and append the converted decimal value.
     * @return {@code true} if the conversion and appending were successful, {@code false} otherwise.
     */
    public boolean toDecimal(float value, DecimalAppender decimalAppender) {
        // Check for non-finite values
        if (!Float.isFinite(value))
            return false;

        // Convert the float to BigDecimal by first converting it to String to avoid precision issues.
        BigDecimal bd = new BigDecimal(Float.toString(value));
        int exp = bd.scale();

        try {
            if (INT_COMPACT == null) {
                // This block is a fallback for JVM implementations where BigDecimal doesn't have an 'intCompact' field.
                BigInteger bi = bd.unscaledValue();
                long l = bi.longValueExact();
                decimalAppender.append(l < 0, Math.abs(l), exp);
                return true;

            } else {
                // Use reflection to access internal long representation of BigDecimal if possible.
                long l = INT_COMPACT.getLong(bd);
                if (l != NEGATIVE_ZERO_BITS) {
                    decimalAppender.append(l < 0, Math.abs(l), exp);
                    return true;
                }
            }
        } catch (ArithmeticException | IllegalAccessException ae) {
            // Fall back in case of exception.
        }

        return false;
    }
}
