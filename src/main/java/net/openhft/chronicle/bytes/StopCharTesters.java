/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        public boolean isStopChar(int ch) throws IllegalStateException {
            return ch == '"' || ch <= 0;
        }
    },
    SINGLE_QUOTES {
        @Override
        public boolean isStopChar(int ch) throws IllegalStateException {
            return ch == '\'' || ch <= 0;
        }
    },
    ALL {
        @Override
        public boolean isStopChar(int ch) {
            return ch < 0;
        }
    }
}
