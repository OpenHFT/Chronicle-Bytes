/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for substituting property placeholders in strings. Placeholders are
 * expected in the form {@code ${propertyName}} where optional ASCII white space
 * inside the braces is ignored.
 * The class is stateless and therefore thread safe.
 */
public enum PropertyReplacer {
    ; // No instances

    // Pattern to find tokens in the format ${ property }
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{\\s*+([^}\\s]*+)\\s*+}");

    /**
     * Replaces tokens of the format {@code ${property}} in the given expression with their corresponding
     * system property values. If a placeholder has no matching property an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param expression the input string containing tokens to be replaced
     * @return a new string with tokens replaced by system property values
     * @throws IllegalArgumentException if a token has no matching system property
     * @throws NullPointerException if {@code expression} is null
     */
    public static String replaceTokensWithProperties(String expression) throws IllegalArgumentException {
        StringBuilder result = new StringBuilder(expression.length());
        int i = 0;
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        while (matcher.find()) {
            result.append(expression, i, matcher.start());
            String property = matcher.group(1);

            // Look up system property and replace
            String p = Jvm.getProperty(property);

            // Throw exception if the property is not set
            if (p == null) {
                throw new IllegalArgumentException(String.format("System property is missing: " +
                        "[property=%s, expression=%s]", property, expression));
            }

            result.append(p);

            i = matcher.end();
        }
        result.append(expression.substring(i));
        return result.toString();
    }

    /**
     * Replaces tokens of the format {@code ${property}} in the given expression with their corresponding
     * values from the provided {@link Properties} object. Unresolved tokens result
     * in an {@link IllegalArgumentException}.
     *
     * @param expression the input string containing tokens to be replaced
     * @param properties the property source used for substitutions
     * @return a new string with tokens replaced by values from {@code properties}
     * @throws IllegalArgumentException if a token has no corresponding property
     * @throws NullPointerException if {@code expression} or {@code properties} is null
     */
    public static String replaceTokensWithProperties(String expression, Properties properties) throws IllegalArgumentException {
        StringBuilder result = new StringBuilder(expression.length());
        int i = 0;
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        while (matcher.find()) {
            result.append(expression, i, matcher.start());
            String property = matcher.group(1);

            // Look up property and replace
            String p = properties.getProperty(property);

            // Throw exception if the property is not set
            if (p == null) {
                throw new IllegalArgumentException(String.format("Property is missing: " +
                        "[property=%s, expression=%s, properties=%s]", property, expression, properties));
            }

            result.append(p);

            i = matcher.end();
        }
        result.append(expression.substring(i));
        return result.toString();
    }

    /**
     * Converts the content of an {@link java.io.InputStream} to a String. This
     * helper is used instead of {@link java.io.InputStream#readAllBytes()} for
     * compatibility with earlier Java versions.
     *
     * @param is the InputStream to be converted.
     * @return the content of the InputStream as a String.
     */
    @NotNull
    private static String convertStreamToString(@NotNull java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
