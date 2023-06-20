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
package net.openhft.chronicle.bytes;

/**
 * A collection of predefined {@link StopCharTester} implementations that define common stop character criteria.
 */
public enum StopCharTesters implements StopCharTester {
    /**
     * Stop character tester which considers a comma (',') as a stop character.
     */
    COMMA_STOP {
        @Override
        public boolean isStopChar(int ch) {
            return ch < ' ' || ch == ',';
        }
    },
    /**
     * Stop character tester which considers a closing curly brace ('}') as a stop character.
     */
    CURLY_STOP {
        @Override
        public boolean isStopChar(int ch) {
            return ch < ' ' || ch == '}';
        }
    },
    /**
     * Stop character tester which considers comma and spaces as stop characters.
     */
    COMMA_SPACE_STOP {
        @Override
        public boolean isStopChar(int ch) {
            return ch <= ' ' || ch == ',';
        }
    },
    /**
     * Stop character tester which considers control characters (ASCII less than 32) as stop characters.
     */
    CONTROL_STOP {
        @Override
        public boolean isStopChar(int ch) {
            return ch < ' ';
        }
    },
    /**
     * Stop character tester which considers spaces and null character as stop characters.
     */
    SPACE_STOP {
        @Override
        public boolean isStopChar(int ch) {
            return Character.isWhitespace(ch) || ch == 0;
        }
    },
    /**
     * Stop character tester which considers quotes ('"') and null character as stop characters.
     */
    QUOTES {
        @Override
        public boolean isStopChar(int ch) {
            return ch == '"' || ch <= 0;
        }
    },
    /**
     * Stop character tester which considers single quotes ('\'') and null character as stop characters.
     */
    SINGLE_QUOTES {
        @Override
        public boolean isStopChar(int ch) {
            return ch == '\'' || ch <= 0;
        }
    },
    /**
     * Stop character tester which considers equals sign ('=') and null character as stop characters.
     */
    EQUALS {
        @Override
        public boolean isStopChar(int ch) {
            return ch == '=' || ch <= 0;
        }
    },
    /**
     * Stop character tester which considers any non-numeric character as a stop character.
     */
    NUMBER_END {
        @Override
        public boolean isStopChar(int ch) {
            switch (ch) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '+':
                case '-':
                case '.':
                case 'E':
                case 'e':
                    return false;
                default:
                    return true;
            }
        }
    },
    /**
     * Stop character tester which considers any non-alphabetic and non-digit character as a stop character.
     */
    NON_ALPHA_DIGIT {
        @Override
        public boolean isStopChar(int ch) {
            return ch < '0' || !(Character.isAlphabetic(ch) || Character.isDigit(ch));
        }
    },
    /**
     * Stop character tester which considers null or less as stop characters.
     */
    NON_NUL {
        @Override
        public boolean isStopChar(int ch) {
            return ch <= 0;
        }
    },
    /**
     * Stop character tester which considers all negative characters as stop characters.
     */
    ALL {
        @Override
        public boolean isStopChar(int ch) {
            return ch < 0;
        }
    }
}
