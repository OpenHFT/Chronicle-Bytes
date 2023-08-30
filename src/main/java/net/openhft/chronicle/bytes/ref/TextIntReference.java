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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.util.ThrowingIntSupplier;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.BytesUtil.roundUpTo8ByteAlign;

/**
 * TextIntReference is an implementation of a reference to a 32-bit integer, represented
 * in text wire format. It extends AbstractReference and implements IntValue. The text representation
 * is formatted to resemble a JSON-like content for an atomic 32-bit integer with a lock indicator.
 * <p>
 * The format of the text representation is:
 * {@code !!atomic { locked: false, value: 0000000000 }}
 * 
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
     * @throws BufferOverflowException If there is insufficient space.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public static void write(@NotNull Bytes<?> bytes, @NonNegative int value)
            throws BufferOverflowException, IllegalStateException {
        long position = bytes.writePosition();
        bytes.write(template);
        bytes.append(position + VALUE, value, DIGITS);
    }

    /**
     * Executes a callable function with lock to ensure atomicity and consistency.
     *
     * @param call the callable function to execute with lock.
     * @return the integer value returned by the callable function.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
        } catch (Exception e) {
            throw Jvm.rethrow(e);
        }
    }

    /**
     * Retrieves the 32-bit integer value from the Text wire format.
     *
     * @return the 32-bit integer value.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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

        if (bytes.readInt(newOffset) == UNINITIALIZED)
            bytes.write(newOffset, template);
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
