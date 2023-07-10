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
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.util.ThrowingIntSupplier;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.BytesUtil.roundUpTo8ByteAlign;

/**
 * Implementation of a reference to a 32-bit integer in text wire format.
 *
 * <p>Allows for the manipulation of 32-bit integer values stored in a Text wire format.
 * It provides methods to read, write, and perform atomic operations on the stored values.</p>
 *
 * <p>Text wire format is a human-readable data serialization format used to represent structured data.</p>
 */
public class TextIntReference extends AbstractReference implements IntValue {
    private static final byte[] template = "!!atomic {  locked: false, value: 0000000000 }".getBytes(ISO_8859_1);
    private static final int FALSE = 'f' | ('a' << 8) | ('l' << 16) | ('s' << 24);
    private static final int TRUE = ' ' | ('t' << 8) | ('r' << 16) | ('u' << 24);
    private static final int UNINITIALIZED = 0;
    private static final int INT_TRUE = 1;
    private static final int INT_FALSE = 0;
    private static final int LOCKED = 20;
    private static final int VALUE = 34;
    private static final int DIGITS = 10;

    /**
     * Writes the provided 32-bit integer value into the given Bytes instance in Text wire format.
     *
     * @param bytes the Bytes instance to write to.
     * @param value the 32-bit integer value to be written.
     * @throws BufferOverflowException if there is insufficient space.
     * @throws IllegalStateException    if an error occurs during writing.
     */
    public static void write(@NotNull Bytes<?> bytes, @NonNegative int value)
            throws BufferOverflowException, IllegalStateException {
        long position = bytes.writePosition();
        bytes.write(template);
        try {
            bytes.append(position + VALUE, value, DIGITS);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Executes the provided operation with the lock held.
     *
     * @param call the operation to execute with the lock held.
     * @return the result of the operation.
     * @throws IllegalStateException if the operation fails.
     */
    private int withLock(@NotNull ThrowingIntSupplier<Exception> call)
            throws IllegalStateException {
        try {
            long alignedOffset = roundUpTo8ByteAlign(offset);
            long lockValueOffset = alignedOffset + LOCKED;
            int lockValue = bytes.readVolatileInt(lockValueOffset);
            if (lockValue != FALSE && lockValue != TRUE)
                throw new IllegalStateException("lockValue: " + lockValue);
            while (true) {
                if (bytes.compareAndSwapInt(lockValueOffset, FALSE, TRUE)) {
                    int t = call.getAsInt();
                    bytes.writeOrderedInt(lockValueOffset, FALSE);
                    return t;
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Retrieves the 32-bit integer value from the Text wire format.
     *
     * @return the 32-bit integer value.
     * @throws IllegalStateException if the operation fails.
     */
    @Override
    public int getValue()
            throws IllegalStateException {
        throwExceptionIfClosed();

        return withLock(() -> (int) bytes.parseLong(offset + VALUE));
    }

    /**
     * Sets the 32-bit integer value in the Text wire format.
     *
     * @param value the 32-bit integer value to be set.
     * @throws IllegalStateException if the operation fails.
     */
    @Override
    public void setValue(int value)
            throws IllegalStateException {
        throwExceptionIfClosedInSetter();

        withLock(() -> {
            bytes.append(offset + VALUE, value, DIGITS);
            return INT_TRUE;
        });
    }

    @Override
    public int getVolatileValue()
            throws IllegalStateException {
        throwExceptionIfClosed();

        return getValue();
    }

    @Override
    public void setOrderedValue(int value)
            throws IllegalStateException {
        throwExceptionIfClosedInSetter();

        setValue(value);
    }

    @Override
    public int addValue(int delta)
            throws IllegalStateException {
        throwExceptionIfClosed();

        return withLock(() -> {
            long value = bytes.parseLong(offset + VALUE) + delta;
            bytes.append(offset + VALUE, value, DIGITS);
            return (int) value;
        });
    }

    @Override
    public int addAtomicValue(int delta)
            throws IllegalStateException {
        throwExceptionIfClosed();

        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(int expected, int value)
            throws IllegalStateException {
        throwExceptionIfClosed();

        return withLock(() -> {
            if (bytes.parseLong(offset + VALUE) == expected) {
                bytes.append(offset + VALUE, value, DIGITS);
                return INT_TRUE;
            }
            return INT_FALSE;
        }) == INT_TRUE;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void bytesStore(final @NotNull BytesStore bytes, @NonNegative long offset, @NonNegative long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        if (length != template.length)
            throw new IllegalArgumentException(length + " != " + template.length);

        // align for ARM
        long newOffset = roundUpTo8ByteAlign(offset);
        for (long i = offset; i < newOffset; i++) {
            bytes.writeByte(i, (byte) ' ');
        }

        super.bytesStore(bytes, newOffset, length);

        try {
            if (bytes.readInt(newOffset) == UNINITIALIZED)
                bytes.write(newOffset, template);
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long maxSize() {
        return template.length;
    }

    /**
     * Returns the string representation of the TextIntReference.
     *
     * @return a string representing the value contained in the TextIntReference.
     */
    @NotNull
    @Override
    public String toString() {
        try {
            return "value: " + getValue();
        } catch (Exception e) {
            return e.toString();
        }
    }
}
