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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * TextIntArrayReference is an implementation of a reference to an array of integers, represented
 * in a text format. It extends AbstractReference and implements ByteableIntArrayValues. This class
 * facilitates the serialization and deserialization of an array of integers in a custom textual
 * representation. The text representation is structured as JSON-like content.
 * <p>
 * The format of the text representation of an integer array is:
 * {@code { capacity: 00000001234, used: 0000000128, values: [ 1234567890, ... ] }}
 * 
 */
@SuppressWarnings("rawtypes")
public class TextIntArrayReference extends AbstractReference implements ByteableIntArrayValues {
    private static final byte[] SECTION1 = "{ locked: false, capacity: ".getBytes(ISO_8859_1);
    private static final byte[] SECTION2 = ", used: ".getBytes(ISO_8859_1);
    private static final byte[] SECTION3 = ", values: [ ".getBytes(ISO_8859_1);
    private static final byte[] SECTION4 = " ] }\n".getBytes(ISO_8859_1);
    private static final byte[] ZERO = "0000000000".getBytes(ISO_8859_1);
    private static final byte[] SEP = ", ".getBytes(ISO_8859_1);

    private static final int DIGITS = ZERO.length;
    private static final int CAPACITY = SECTION1.length;
    private static final int USED = CAPACITY + DIGITS + SECTION2.length;
    private static final int VALUES = USED + DIGITS + SECTION3.length;
    private static final int VALUE_SIZE = DIGITS + SEP.length;
    private static final int LOCK_OFFSET = 10;
    private static final int FALS = 'f' | ('a' << 8) | ('l' << 16) | ('s' << 24);
    private static final int TRU = ' ' | ('t' << 8) | ('r' << 16) | ('u' << 24);

    private long length = VALUES;

    /**
     * Writes an array of integers to the specified {@link Bytes} instance in a text format.
     *
     * @param bytes    the Bytes instance to write to.
     * @param capacity the capacity of the array to be written.
     * @throws IllegalStateException   if an invalid state is encountered.
     * @throws BufferOverflowException if there's not enough space in the buffer to write the array.
     */
    public static void write(@NotNull Bytes<?> bytes, @NonNegative long capacity)
            throws IllegalStateException, BufferOverflowException {
        long start = bytes.writePosition();
        bytes.write(SECTION1);
        bytes.append(capacity);
        while (bytes.writePosition() - start < CAPACITY + DIGITS) {
            bytes.writeUnsignedByte(' ');
        }
        bytes.write(SECTION2);
        bytes.write(ZERO);
        bytes.write(SECTION3);
        for (long i = 0; i < capacity; i++) {
            if (i > 0) {
                bytes.appendUtf8(", ");
            }
            bytes.write(ZERO);
        }
        bytes.write(SECTION4);
    }

    /**
     * Determines the length of the array in the text format represented in the given {@link BytesStore}.
     *
     * @param bytes  the BytesStore containing the text representation.
     * @param offset the offset at which the text representation starts.
     * @return the length of the array in text format.
     * @throws IllegalStateException if an invalid state is encountered.
     */
    public static long peakLength(@NotNull BytesStore bytes, @NonNegative long offset)
            throws IllegalStateException {
        //todo check this, I think there could be a bug here
        return (bytes.parseLong(offset + CAPACITY) * VALUE_SIZE) - SEP.length
                + VALUES + SECTION4.length;
    }

    @Override
    public long getUsed()
            throws IllegalStateException {
        try {
            return bytes.parseLong(USED + offset);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    private void setUsed(long used)
            throws IllegalStateException, IllegalArgumentException {
        try {
            bytes.append(VALUES + offset, used, DIGITS);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    @Override
    public void setMaxUsed(long usedAtLeast)
            throws IllegalStateException {
        try {
            while (true) {
                if (!bytes.compareAndSwapInt(LOCK_OFFSET + offset, FALS, TRU))
                    continue;
                try {
                    if (getUsed() < usedAtLeast) {
                        setUsed(usedAtLeast);
                    }
                    break;
                } finally {
                    bytes.writeInt(LOCK_OFFSET + offset, FALS);
                }
            }
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    @Override
    public long getCapacity() {
        return (length - VALUES) / VALUE_SIZE;
    }

    @Override
    public ByteableIntArrayValues capacity(@NonNegative long arrayLength) {
        BytesStore bytesStore = bytesStore();
        long len = sizeInBytes(arrayLength);
        if (bytesStore == null) {
            this.length = len;
        } else {
            assert this.length == len;
        }
        return this;
    }

    @Override
    public int getValueAt(@NonNegative long index)
            throws IllegalStateException {
        try {
            return (int) bytes.parseLong(VALUES + offset + index * VALUE_SIZE);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }

    }

    @Override
    public void setValueAt(@NonNegative long index, int value)
            throws IllegalStateException {
        try {
            bytes.append(VALUES + offset + index * VALUE_SIZE, value, DIGITS);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    @Override
    public void bindValueAt(@NonNegative long index, IntValue value) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public int getVolatileValueAt(@NonNegative long index)
            throws IllegalStateException {
        OS.memory().loadFence();
        return getValueAt(index);
    }

    @Override
    public void setOrderedValueAt(@NonNegative long index, int value)
            throws IllegalStateException {
        setValueAt(index, value);
        OS.memory().storeFence();
    }

    @Override
    public boolean compareAndSet(@NonNegative long index, int expected, int value)
            throws IllegalStateException {
        try {
            if (!bytes.compareAndSwapInt(LOCK_OFFSET + offset, FALS, TRU))
                return false;
            boolean ret = false;
            try {
                if (getVolatileValueAt(index) == expected) {
                    setOrderedValueAt(index, value);
                    ret = true;
                }
                return ret;
            } finally {
                bytes.writeInt(LOCK_OFFSET + offset, FALS);
            }
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }

    }

    @Override
    public void bytesStore(final @NotNull BytesStore bytes, @NonNegative long offset, @NonNegative long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        if (length != peakLength(bytes, offset))
            throw new IllegalArgumentException(length + " != " + peakLength(bytes, offset));
        super.bytesStore(bytes, offset, length);
        this.length = length;
    }

    @Override
    public boolean isNull() {
        return bytes == null;
    }

    @Override
    public void reset()
            throws IllegalStateException {
        throwExceptionIfClosedInSetter();

        bytes = null;
        offset = 0;
        length = 0;
    }

    @Override
    public long maxSize() {
        return length;
    }

    /**
     * Returns a String representation of this TextIntArrayReference.
     *
     * @return A String representation.
     */
    @NotNull
    @Override
    public String toString() {
        if (bytes == null) {
            return "LongArrayTextReference{" +
                    "bytes=null" +
                    ", offset=" + offset +
                    ", length=" + length +
                    '}';
        }

        try {
            return "value: " + getValueAt(0) + " ...";
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * Calculates the size in bytes needed for storing an array with the specified capacity.
     *
     * @param capacity The capacity of the array.
     * @return The size in bytes needed to store the array.
     */
    @Override
    public long sizeInBytes(@NonNegative long capacity) {
        return (capacity * VALUE_SIZE) + VALUES + SECTION3.length - SEP.length;
    }
}
