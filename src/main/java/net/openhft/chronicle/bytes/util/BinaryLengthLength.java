/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

/**
 * Defines strategies for encoding and writing the length prefix of binary data
 * segments. Supported prefixes are 8, 16 and 32-bit fields. After writing the
 * payload the caller must invoke {@link #writeLength(Bytes, long, long)} exactly
 * once to store the final length.
 *
 * <p>Maximum storable lengths:</p>
 * <ul>
 *   <li>8-bit: 255 bytes</li>
 *   <li>16-bit: 65 535 bytes</li>
 *   <li>32-bit: 2 147 483 647 bytes</li>
 * </ul>
 *
 * Note: Each {@code writeLength} call performs a store fence as required by
 * JSR-133 to ensure the length is visible to other threads.
 */
public enum BinaryLengthLength {
    /**
     * Represents an 8-bit length prefix capable of encoding data lengths from 0
     * to 255 bytes. The prefix consists of one byte identifying the type and one
     * byte holding the length value.
     */
    LENGTH_8BIT {
        /**
         * Returns the identifying code for an 8-bit length prefix.
         *
         * @return the code used in the binary stream
         */
        @Override
        public int code() {
            return BinaryWireCode.BYTES_LENGTH8;
        }

        /**
         * Writes the identifying code and reserves one byte for the length.
         *
         * @param bytes the output to initialise
         * @return the offset where the length should later be written
         */
        @Override
        public long initialise(@NotNull final BytesOut<?> bytes) {
            bytes.writeUnsignedByte(code());
            long pos = bytes.writePosition();
            bytes.writeByte((byte) 0);
            return pos;
        }

        /**
         * Calculates the data length and writes it at the supplied position.
         * A store fence is performed to ensure visibility between threads.
         *
         * @param bytes                          the bytes to update
         * @param positionReturnedFromInitialise the offset returned by {@link #initialise(BytesOut)}
         * @param end                            the absolute end of the data
         * @throws IllegalStateException if the length exceeds 255
         */
        @Override
        public void writeLength(@NotNull Bytes<?> bytes, @NonNegative long positionReturnedFromInitialise, @NonNegative long end) {
            long length = (end - positionReturnedFromInitialise - 1) & MASK;
            if (length >= 1 << 8)
                throw invalidLength(length);
            bytes.writeByte(positionReturnedFromInitialise, (byte) length);
            UnsafeMemory.MEMORY.storeFence(); // ensures visibility between threads
        }
    },
    /**
     * Represents a 16-bit length prefix. The prefix comprises one code byte and
     * two bytes storing the length, allowing data up to 65535 bytes.
     */
    LENGTH_16BIT {
        /**
         * Returns the identifying code for a 16-bit length prefix.
         *
         * @return the code used in the binary stream
         */
        @Override
        public int code() {
            return BinaryWireCode.BYTES_LENGTH16;
        }

        /**
         * Writes the identifying code and reserves two bytes for the length.
         *
         * @param bytes the output to initialise
         * @return the offset where the length should later be written
         */
        @Override
        public long initialise(@NotNull final BytesOut<?> bytes) {
            bytes.writeUnsignedByte(code());
            final long pos = bytes.writePosition();
            bytes.writeShort((short) 0);
            return pos;
        }

        /**
         * Calculates the data length and writes it at the supplied position.
         *
         * @param bytes                          the bytes to update
         * @param positionReturnedFromInitialise the offset returned by {@link #initialise(BytesOut)}
         * @param end                            the absolute end of the data
         * @throws IllegalStateException if the length exceeds 65535
         */
        @Override
        public void writeLength(@NotNull Bytes<?> bytes, @NonNegative long positionReturnedFromInitialise, @NonNegative long end) {
            final long length = (end - positionReturnedFromInitialise - 2) & MASK;
            if (length >= 1 << 16)
                throw invalidLength(length);
            bytes.writeShort(positionReturnedFromInitialise, (short) length);
            UnsafeMemory.MEMORY.storeFence(); // ensures visibility between threads
        }
    },
    /**
     * Represents a 32-bit length prefix allowing data up to 2,147,483,647 bytes.
     * The prefix comprises one code byte followed by four bytes of length.
     */
    LENGTH_32BIT {
        /**
         * Returns the identifying code for a 32-bit length prefix.
         *
         * @return the code used in the binary stream
         */
        @Override
        public int code() {
            return BinaryWireCode.BYTES_LENGTH32;
        }

        /**
         * Writes the identifying code and reserves four bytes for the length.
         *
         * @param bytes the output to initialise
         * @return the offset where the length should later be written
         */
        @Override
        public long initialise(@NotNull BytesOut<?> bytes) {
            bytes.writeUnsignedByte(code());
            final long pos = bytes.writePosition();
            bytes.writeInt(0);
            return pos;
        }

        /**
         * Calculates the data length and writes it at the supplied position.
         *
         * @param bytes                          the bytes to update
         * @param positionReturnedFromInitialise the offset returned by {@link #initialise(BytesOut)}
         * @param end                            the absolute end of the data
         * @throws IllegalStateException if the length exceeds the 32-bit range
         */
        @Override
        public void writeLength(@NotNull Bytes<?> bytes, @NonNegative long positionReturnedFromInitialise, @NonNegative long end) {
            final long length = (end - positionReturnedFromInitialise - 4) & MASK;
            if (length >= 1L << 31)
                throw invalidLength(length);
            bytes.writeOrderedInt(positionReturnedFromInitialise, (int) length);
            UnsafeMemory.MEMORY.storeFence(); // ensures visibility between threads
        }
    };

    static final long MASK = 0xFFFFFFFFL;

    /**
     * Constructs an IllegalStateException for an invalid length.
     *
     * @param length the invalid length
     * @return the IllegalStateException instance with a message regarding the invalid length
     */
    IllegalStateException invalidLength(@NonNegative final long length) {
        return new IllegalStateException("length: " + length);
    }

    /**
     * Returns the {@link net.openhft.chronicle.bytes.BinaryWireCode} identifying
     * this length field in the binary stream.
     *
     * @return the code used in the wire format
     */
    public abstract int code();

    /**
     * Writes the identifying code and reserves space for the length field.
     *
     * @param bytes the output bytes to write to
     * @return the absolute offset at which the length should be written later
     */
    public abstract long initialise(@NotNull BytesOut<?> bytes);

    /**
     * Writes the actual length at the supplied offset after the data segment has
     * been written.
     *
     * @param bytes                          the bytes to update
     * @param positionReturnedFromInitialise the offset from {@link #initialise(BytesOut)}
     * @param end                            the absolute end of the data
     */
    public abstract void writeLength(@NotNull Bytes<?> bytes,
                                     @NonNegative long positionReturnedFromInitialise,
                                     @NonNegative long end);
}
