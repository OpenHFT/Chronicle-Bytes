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

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;
/**
 * Enum representing different byte lengths for binary values.
 * It provides methods to get the code associated with each byte length,
 * to initialize a {@link BytesOut} object with the length code, and
 * to write the length of the data into the {@link Bytes} object.
 */
public enum BinaryLengthLength {
    /**
     * Represents an 8-bit length binary value.
     */
    LENGTH_8BIT {
        /**
         * @return The code for the 8-bit length.
         */
        @Override
        public int code() {
            return BinaryWireCode.BYTES_LENGTH8;
        }

        @Override
        public long initialise(@NotNull final BytesOut<?> bytes) {
            bytes.writeUnsignedByte(code());
            long pos = bytes.writePosition();
            bytes.writeByte((byte) 0);
            return pos;
        }

        @Override
        public void writeLength(@NotNull Bytes<?> bytes, @NonNegative long positionReturnedFromInitialise, @NonNegative long end) {
            long length = (end - positionReturnedFromInitialise - 1) & MASK;
            if (length >= 1 << 8)
                throw invalidLength(length);
            bytes.writeByte(positionReturnedFromInitialise, (byte) length);
            UnsafeMemory.MEMORY.storeFence();
        }
    },
    /**
     * Represents a 16-bit length binary value.
     */
    LENGTH_16BIT {
        @Override
        public int code() {
            return BinaryWireCode.BYTES_LENGTH16;
        }

        @Override
        public long initialise(@NotNull final BytesOut<?> bytes) {
            bytes.writeUnsignedByte(code());
            final long pos = bytes.writePosition();
            bytes.writeShort((short) 0);
            return pos;
        }

        @Override
        public void writeLength(@NotNull Bytes<?> bytes, @NonNegative long positionReturnedFromInitialise, @NonNegative long end) {
            final long length = (end - positionReturnedFromInitialise - 2) & MASK;
            if (length >= 1 << 16)
                throw invalidLength(length);
            bytes.writeShort(positionReturnedFromInitialise, (short) length);
            UnsafeMemory.MEMORY.storeFence();
        }
    },
    /**
     * Represents a 32-bit length binary value.
     */
    LENGTH_32BIT {
        @Override
        public int code() {
            return BinaryWireCode.BYTES_LENGTH32;
        }

        @Override
        public long initialise(@NotNull BytesOut<?> bytes) {
            bytes.writeUnsignedByte(code());
            final long pos = bytes.writePosition();
            bytes.writeInt(0);
            return pos;
        }

        @Override
        public void writeLength(@NotNull Bytes<?> bytes, @NonNegative long positionReturnedFromInitialise, @NonNegative long end) {
            final long length = (end - positionReturnedFromInitialise - 4) & MASK;
            if (length >= 1L << 31)
                throw invalidLength(length);
            bytes.writeOrderedInt(positionReturnedFromInitialise, (int) length);
        }
    };

    static final long MASK = 0xFFFFFFFFL;

    IllegalStateException invalidLength(@NonNegative final long length) {
        return new IllegalStateException("length: " + length);
    }

    public abstract int code();

    public abstract long initialise(@NotNull BytesOut<?> bytes);

    public abstract void writeLength(@NotNull Bytes<?> bytes,
                                     @NonNegative long positionReturnedFromInitialise,
                                     @NonNegative long end);
}
