package net.openhft.chronicle.bytes.render;

import net.openhft.chronicle.core.Jvm;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An implementation of {@link Decimaliser} that uses {@link BigDecimal} for higher-precision conversions of floating-point
 * numbers to decimal representation.
 * <p>
 * This implementation is designed for scenarios where precision is of utmost importance, and is suitable for handling
 * very large numbers or numbers with a large number of decimal places.
 * <p>
 * Note: Using {@link BigDecimal} might incur a performance overhead compared to more lightweight approaches.
 */
public class UsesBigDecimal implements Decimaliser {

    /**
     * A singleton instance of {@link UsesBigDecimal} for convenient reuse.
     * This instance is thread-safe and can be used across multiple threads without synchronization.
     */
    public static final Decimaliser USES_BIG_DECIMAL = new UsesBigDecimal();

    /**
     * Field reference to the internal storage of {@link BigDecimal} values. It is used to access
     * implementation details of {@link BigDecimal}, specifically the unscaled value of the number.
     */
    private static final java.lang.reflect.Field INT_COMPACT = Jvm.getFieldOrNull(BigDecimal.class, "intCompact");

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
     * @return <code>true</code> if the conversion and appending were successful, <code>false</code> otherwise.
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
     * @return <code>true</code> if the conversion and appending were successful, <code>false</code> otherwise.
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
