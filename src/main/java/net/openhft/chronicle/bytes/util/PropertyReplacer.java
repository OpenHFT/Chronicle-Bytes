/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for replacing tokens in strings with their corresponding property values.
 * <p>
 * The class provides methods to replace tokens in the format {@code ${property}} within strings,
 * with the corresponding property values from system properties or a given {@link Properties} object.
 */
public enum PropertyReplacer {
    ; // No instances

    // Pattern to find tokens in the format ${property}
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{\\s*+([^}\\s]*+)\\s*+}");

    /**
     * Replaces tokens of the format {@code ${property}} in the given expression with their corresponding
     * system property values.
     *
     * @param expression the input string containing tokens to be replaced.
     * @return a new string with tokens replaced by their corresponding system property values.
     * @throws IllegalArgumentException if a token does not have a corresponding system property.
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
     * values from the provided {@link Properties} object.
     *
     * @param expression the input string containing tokens to be replaced.
     * @param properties the Properties object to retrieve property values from.
     * @return a new string with tokens replaced by their corresponding property values from the given Properties object.
     * @throws IllegalArgumentException if a token does not have a corresponding property in the given Properties object.
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
     * Converts the content of an {@link java.io.InputStream} to a String.
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
