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

public enum UnderflowMode {
    /**
     * Throw a BufferedUnderflowException if reading beyond the end of the buffer.
     */
    BOUNDED {
        @Override
        boolean isRemainingOk(long remaining, int needs) {
            return remaining >= needs;
        }
    },
    /**
     * return 0, false, empty string or some default if remaining() == 0 otherwise if remaining() is less than required throw a BufferUnderflowException.
     */
    ZERO_EXTEND {
        @Override
        boolean isRemainingOk(long remaining, int needs) {
            return remaining >= needs || remaining <= 0;
        }
    },
    /**
     * any read beyond the limit should be treated as 0.
     */
    PADDED {
        @Override
        boolean isRemainingOk(long remaining, int needs) {
            return true;
        }
    };

    abstract boolean isRemainingOk(long remaining, int needs);
}
