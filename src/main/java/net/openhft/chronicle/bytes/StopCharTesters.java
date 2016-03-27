/*
 * Copyright 2016 higherfrequencytrading.com
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
 * @author peter.lawrey
 */
public enum StopCharTesters implements StopCharTester {
    COMMA_STOP {
        @Override
        public boolean isStopChar(int ch) {
            return ch < ' ' || ch == ',';
        }
    },
    CONTROL_STOP {
        @Override
        public boolean isStopChar(int ch) {
            return ch < ' ';
        }
    },
    SPACE_STOP {
        @Override
        public boolean isStopChar(int ch) {
            return Character.isWhitespace(ch) || ch == 0;
        }
    },
    QUOTES {
        @Override
        public boolean isStopChar(int ch) {
            return ch == '"' || ch <= 0;
        }
    },
    SINGLE_QUOTES {
        @Override
        public boolean isStopChar(int ch) {
            return ch == '\'' || ch <= 0;
        }
    },
    EQUALS {
        @Override
        public boolean isStopChar(int ch) {
            return ch == '=' || ch <= 0;
        }
    },
    ALL {
        @Override
        public boolean isStopChar(int ch) {
            return ch < 0;
        }
    }
}
