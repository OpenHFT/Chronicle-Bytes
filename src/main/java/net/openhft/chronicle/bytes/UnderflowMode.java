/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
