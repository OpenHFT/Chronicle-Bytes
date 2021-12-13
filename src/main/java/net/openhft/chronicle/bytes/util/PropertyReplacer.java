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
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

public enum PropertyReplacer {
    ; // none

    /**
     * Returns a modified version of the provided {@code expression} whereby System Properties are replaced with their
     * corresponding values.
     *
     * @param expression to modify
     * @return replaced String
     * @throws IllegalArgumentException if the provided expression is malformed or if a property is missing.
     */
    public static String replaceTokensWithProperties(@NotNull final String expression) {
        return replaceTokensWithProperties(expression, System.getProperties());
    }

    /**
     * Returns a modified version of the provided {@code expression} whereby the provided {@code properties} are replaced with their
     * corresponding values.
     *
     * @param expression to modify
     * @param properties to use as source
     * @return replaced String
     * @throws IllegalArgumentException if the provided expression is malformed or if a property is missing.
     */
    public static String replaceTokensWithProperties(@NotNull final String expression,
                                                     @NotNull final Properties properties) {

        final List<Group> groups = groups(expression);
        if (groups.isEmpty())
            // Nothing to replace...
            return expression;

        final StringBuilder result = new StringBuilder(expression.length());
        int i = 0;
        for (Group group : groups) {
            result.append(expression, i, group.begin());
            final String property = group.tag();

            //look up property and replace
            final String p = properties.getProperty(property);
            if (p == null) {
                throw new IllegalArgumentException(String.format("Property is missing: " +
                        "[property=%s, expression=%s, properties=%s]", property, expression, properties));
            }
            result.append(p);
            i = group.end() + 1;
        }

        // Append the remaining text
        result.append(expression.substring(i));
        return result.toString();
    }

    static List<Group> groups(String expression) {
        final List<Group> groups = new ArrayList<>();
        if (expression.length() < 3) {
            return groups;
        }
        boolean parsing = false;
        int begin = 0;
        int beginKey = Integer.MAX_VALUE;
        int endKey = 0;
        ScanState state = ScanState.AWAITING_DOLLAR;
        for (int i = 0; i < expression.length(); i++) {
            final char c = expression.charAt(i);
            if (state.consumeSpaces() && Character.isWhitespace(c)) {
                state = state.next(c);
                if (state.consumeSpaces())
                    continue;
            }
            state = state.next(c);
            switch (state) {
                case AWAITING_OPEN_BRACKET: {
                    parsing = true;
                    begin = i;
                    beginKey = Integer.MAX_VALUE;
                    endKey = 0;
                    break;
                }
                case CONSUMING_TOKEN: {
                    beginKey = Math.min(beginKey, i);
                    endKey = Math.max(endKey, i);
                    break;
                }
                case CLOSED: {
                    groups.add(new Group(begin, i, beginKey, endKey, expression.substring(beginKey, endKey + 1)));
                    parsing = false;

                    if (c == '$') {
                        // The closing character might indeed be a new group '$' so we need to consume it again
                        state = state.next(c);
                        i--;
                    }
                    break;
                }
                default:
                    // Do nothing
            }
        }
        if (parsing && state == ScanState.CONSUMING_SPACES_AFTER)
            // This means we are still in a group and that the last character in the expression was a terminating '}'
            groups.add(new Group(begin, expression.length() - 1, beginKey, endKey, expression.substring(beginKey, endKey + 1)));

        return groups;
    }

    // The states in the scanning state machine
    private enum ScanState {
        AWAITING_DOLLAR {
            @Override
            public ScanState next(char c) {
                return c != '$'
                        ? this
                        : AWAITING_OPEN_BRACKET;
            }
        },
        AWAITING_OPEN_BRACKET {
            @Override
            public ScanState next(char c) {
                return c != '{'
                        ? AWAITING_DOLLAR // Back again... it was not "${"
                        : CONSUMING_SPACES_BEFORE;
            }
        },
        CONSUMING_SPACES_BEFORE(true) {
            @Override
            public ScanState next(char c) {
                return Character.isWhitespace(c)
                        ? this
                        : CONSUMING_TOKEN;
            }
        },
        CONSUMING_TOKEN {
            @Override
            public ScanState next(char c) {
                if (Character.isWhitespace(c))
                    return CONSUMING_SPACES_AFTER;
                if (c == '}')
                    return CLOSED;
                return this;
            }
        },
        CONSUMING_SPACES_AFTER(true) {
            @Override
            public ScanState next(char c) {
                return Character.isWhitespace(c)
                        ? this
                        : CLOSED;
            }
        },
        CLOSED {
            @Override
            public ScanState next(char c) {
                return AWAITING_DOLLAR;
            }
        };

        private final boolean consumeSpaces;

        ScanState() {
            this(false);
        }

        ScanState(boolean consumeSpaces) {
            this.consumeSpaces = consumeSpaces;
        }

        public abstract ScanState next(char c);

        public boolean consumeSpaces() {
            return consumeSpaces;
        }
    }

    // use IOTools.readFile(Class, String) as this is needed for Java 11.
    @Deprecated(/* to be removed in x.23 */)
    @NotNull
    public static String fileAsString(String fileName)
            throws IOException {
        try {
            Class<PropertyReplacer> propertyReplacerClass = PropertyReplacer.class;
            InputStream is = propertyReplacerClass.getResourceAsStream(fileName);
            if (is != null)
                return convertStreamToString(is);
        } catch (Exception ignored) {
            // Ignore
        }
        return BytesUtil.readFile(fileName).toString();
    }

    @NotNull
    private static String convertStreamToString(@NotNull final java.io.InputStream is) {
        try (Scanner s = new Scanner(is).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    static final class Group {

        private final int begin;
        private final int end;
        private final int keyBegin;
        private final int keyEnd;
        private final String tag;

        public Group(@NonNegative final int begin,
                     @NonNegative final int end,
                     @NonNegative final int keyBegin,
                     @NonNegative final int keyEnd,
                     @NotNull final String tag) {

            this.begin = requireNonNegative(begin);
            this.end = requireNonNegative(end);
            this.keyBegin = requireNonNegative(keyBegin);
            this.keyEnd = requireNonNegative(keyEnd);
            this.tag = requireNonNull(tag);
        }

        int begin() {
            return begin;
        }

        int end() {
            return end;
        }

        int keyBegin() {
            return keyBegin;
        }

        int keyEnd() {
            return keyEnd;
        }

        String tag() {
            return tag;
        }

        @Override
        public String toString() {
            return "{[" + begin + ", " + end + "], [" + keyBegin + ", " + keyEnd + "], " + tag + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Group group = (Group) o;

            if (begin != group.begin) return false;
            if (end != group.end) return false;
            if (keyBegin != group.keyBegin) return false;
            if (keyEnd != group.keyEnd) return false;
            return tag.equals(group.tag);
        }

        @Override
        public int hashCode() {
            int result = begin;
            result = 31 * result + end;
            result = 31 * result + keyBegin;
            result = 31 * result + keyEnd;
            result = 31 * result + tag.hashCode();
            return result;
        }
    }

}
