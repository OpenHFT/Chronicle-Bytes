package net.openhft.chronicle.bytes.render;

/**
 * A light-weight implementation of {@link Decimaliser}, which employs a simple rounding technique for converting floating-point
 * numbers to decimal representation. The range for conversion is between 1e-18 and {@link Long#MAX_VALUE}.
 * <p>
 * This implementation is designed for performance and simplicity, at the cost of potentially reduced accuracy.
 * It is useful in scenarios where performance is a higher priority than precision.
 */
public class SimpleDecimaliser implements Decimaliser {

    /**
     * The largest exponent that can be represented using a long integer. Beyond this value, the Decimaliser gives up.
     */
    public static final int LARGEST_EXPONENT_IN_LONG = 18;

    /**
     * A singleton instance of {@link SimpleDecimaliser} for convenient reuse.
     * This instance is thread-safe and can be used across multiple threads without synchronization.
     */
    public static final Decimaliser SIMPLE = new SimpleDecimaliser();

    /**
     * Converts a double value to its decimal representation using a simple rounding approach, and appends it to the provided
     * {@link DecimalAppender}.
     * <p>
     * This method iteratively scales the input value by powers of 10, and performs rounding to attempt finding a precise
     * representation. If such representation is found, it is appended using the provided {@link DecimalAppender}.
     *
     * @param value           The double value to be converted.
     * @param decimalAppender The {@link DecimalAppender} used to store and append the converted decimal value.
     * @return <code>true</code> if the conversion and appending were successful, <code>false</code> otherwise.
     */
    public boolean toDecimal(double value, DecimalAppender decimalAppender) {
        // Determine if the input value is negative.
        boolean isNegative = Double.doubleToLongBits(value) < 0;
        // Take the absolute value for conversion.
        double absValue = Math.abs(value);

        // Initialize the factor used to scale the input value.
        long factor = 1;
        // Iterate through the exponents to find a precise representation.
        for (int exponent = 0; exponent <= LARGEST_EXPONENT_IN_LONG; exponent++) {
            // Scale and round the value.
            long mantissa = Math.round(absValue * factor);
            // Check if the scaled and rounded value matches the original.
            if ((double) mantissa / factor == absValue) {
                // Append the representation to the decimal appender.
                decimalAppender.append(isNegative, mantissa, exponent);
                return true;
            }
            // Increase the factor for the next iteration.
            factor *= 10;
        }
        return false;
    }

    /**
     * Converts a float value to its decimal representation using a simple rounding approach, and appends it to the provided
     * {@link DecimalAppender}.
     * <p>
     * This method iteratively scales the input value by powers of 10, and performs rounding to attempt finding a precise
     * representation. If such representation is found, it is appended using the provided {@link DecimalAppender}.
     *
     * @param value           The float value to be converted.
     * @param decimalAppender The {@link DecimalAppender} used to store and append the converted decimal value.
     * @return <code>true</code> if the conversion and appending were successful, <code>false</code> otherwise.
     */
    public boolean toDecimal(float value, DecimalAppender decimalAppender) {
        // Determine if the input value is negative.
        boolean sign = Float.floatToRawIntBits(value) < 0;
        // Take the absolute value for conversion.
        float absValue = Math.abs(value);

        // Initialize the factor used to scale the input value.
        long factor = 1;
        // Iterate through the exponents to find a precise representation.
        for (int exponent = 0; exponent <= LARGEST_EXPONENT_IN_LONG; exponent++) {
            // Scale and round the value.
            long mantissa = Math.round(absValue * (double) factor);
            // Check if the scaled and rounded value matches the original.
            if ((float) mantissa / factor == absValue) {
                // Append the representation to the decimal appender.
                decimalAppender.append(sign, mantissa, exponent);
                return true;
            }
            // Increase the factor for the next iteration.
            factor *= 10;
        }
        return false;
    }
}
