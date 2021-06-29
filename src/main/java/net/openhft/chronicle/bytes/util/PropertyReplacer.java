/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PropertyReplacer {
    ; // none

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{\\s*([^}]*?)\\s*\\}");

    public static String replaceTokensWithProperties(String expression) throws IllegalArgumentException {

        StringBuilder result = new StringBuilder(expression.length());
        int i = 0;
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        while (matcher.find()) {
            result.append(expression, i, matcher.start());
            String property = matcher.group(1);

            //look up property and replace
            String p = System.getProperty(property);

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

    public static String replaceTokensWithProperties(String expression, Properties properties) throws IllegalArgumentException {

        StringBuilder result = new StringBuilder(expression.length());
        int i = 0;
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        while (matcher.find()) {
            result.append(expression, i, matcher.start());
            String property = matcher.group(1);

            //look up property and replace
            String p = properties.getProperty(property);

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

    @NotNull
    public static String fileAsString(String fileName)
            throws IOException {
        try {
            Class<PropertyReplacer> propertyReplacerClass = PropertyReplacer.class;
            InputStream is = propertyReplacerClass.getResourceAsStream(fileName);
            if (is != null)
                return convertStreamToString(is);
        } catch (Exception ignored) {
        }
        return BytesUtil.readFile(fileName).toString();
    }

    @NotNull
    private static String convertStreamToString(@NotNull java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
