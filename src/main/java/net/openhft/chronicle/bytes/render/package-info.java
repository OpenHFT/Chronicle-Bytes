/**
 * Provides classes and interfaces for rendering byte sequences into decimal representation
 * with various levels of precision and formatting. This package is part of the Chronicle Bytes library
 * and offers a selection of Decimaliser implementations that control the conversion of floating-point numbers
 * to their decimal representations.
 *
 * <p>Classes in this package include:
 *
 * <ul>
 *     <li>{@link net.openhft.chronicle.bytes.render.StandardDecimaliser} - An implementation that produces numbers
 *     in standard form, ensuring that large numbers are represented in full, while small values are rounded to 18 decimal places.</li>
 *
 *     <li>{@link net.openhft.chronicle.bytes.render.SimpleDecimaliser} - A light-weight implementation
 *     that employs simple rounding techniques. This implementation is optimized for performance and simplicity,
 *     making it useful in scenarios where performance is a higher priority than precision.</li>
 *
 *     <li>{@link net.openhft.chronicle.bytes.render.GeneralDecimaliser} - A general implementation that initially
 *     attempts conversion using a light-weight strategy and falls back to the BigDecimal-based strategy if necessary.</li>
 *
 *     <li>{@link net.openhft.chronicle.bytes.render.UsesBigDecimal} - A BigDecimal-based implementation that
 *     is used for higher precision conversions. This is useful in scenarios where precision is a higher priority
 *     than performance.</li>
 *
 *     <li>{@link net.openhft.chronicle.bytes.render.MaximumPrecision} - An implementation that maintains
 *     a maximum precision during conversion, ensuring that numbers are converted using only that precision.</li>
 * </ul>
 *
 * <p>These classes are used to convert floating-point numbers into a string representation that is optimized for either
 * performance or precision, depending on the use case requirements.
 */
package net.openhft.chronicle.bytes.render;
