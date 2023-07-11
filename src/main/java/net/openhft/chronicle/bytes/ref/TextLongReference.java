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
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.util.ThrowingLongSupplier;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.BytesUtil.roundUpTo8ByteAlign;

/**
 * Provides a reference to an array of 32-bit integer values in Text wire format.
 *
 * <p>The {@code TextLongReference} class allows for the manipulation of long values stored
 * in a Text wire format. It provides methods to read, write and perform atomic operations
 * on the stored values.</p>
 *
 * <p>Text wire format is a human-readable data serialization format used to represent structured data.</p>
 */
public class TextLongReference extends AbstractReference implements LongReference {

    static final int VALUE = 34;
    private static final byte[] template = "!!atomic {  locked: false, value: 00000000000000000000 }".getBytes(ISO_8859_1);
    private static final int FALSE = BytesUtil.asInt("fals");
    private static final int TRUE = BytesUtil.asInt(" tru");
    private static final long UNINITIALIZED = 0x0L;
    private static final long LONG_TRUE = 1L;
    private static final long LONG_FALSE = 0L;
    private static final int LOCKED = 20;
    private static final int DIGITS = 20;

    /**
     * Writes the provided value into the given Bytes instance in Text wire format.
     *
     * @param bytes the Bytes instance to write to.
     * @param value the value to be written.
     * @throws IllegalStateException    if released
     */
    public static void write(@NotNull Bytes<?> bytes, @NonNegative long value)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        long position = bytes.writePosition();
        bytes.write(template);
        bytes.append(position + VALUE, value, DIGITS);
    }

    /**
     * Executes a given operation with the lock held.
     *
     * @param call the operation to execute with the lock held.
     * @return the result of the operation.
     * @throws IllegalStateException if the operation fails.
     */
    private <T extends Exception> long withLock(@NotNull ThrowingLongSupplier<T> call)
            throws IllegalStateException {
        try {
            long valueOffset = offset + LOCKED;
            int value = bytes.readVolatileInt(valueOffset);
            if (value != FALSE && value != TRUE)
                throw new IllegalStateException("Not a lock value");

            while (true) {
                if (bytes.compareAndSwapInt(valueOffset, FALSE, TRUE)) {
                    long t = call.getAsLong();
                    bytes.writeOrderedInt(valueOffset, FALSE);
                    return t;
                }
            }
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        } catch (Exception throwable) {
            throw new AssertionError(throwable);
        }
    }

    @Override
    public void bytesStore(final @NotNull BytesStore bytes, @NonNegative long offset, @NonNegative long length)
            throws IllegalArgumentException, IllegalStateException, BufferOverflowException {
        if (length != template.length)
            throw new IllegalArgumentException();

        // align for ARM
        long newOffset = roundUpTo8ByteAlign(offset);
        for (long i = offset; i < newOffset; i++) {
            bytes.writeByte(i, (byte) ' ');
        }

        super.bytesStore(bytes, newOffset, length);

        if (bytes.readLong(newOffset) == UNINITIALIZED)
            bytes.write(newOffset, template);
    }

    /**
     * Retrieves the value from the Text wire format.
     *
     * @return the long value.
     * @throws IllegalStateException if the operation fails.
     */
    @Override
    public long getValue()
            throws IllegalStateException {
        return withLock(() -> bytes.parseLong(offset + VALUE));
    }

    /**
     * Sets the value in the Text wire format.
     *
     * @param value the value to be set.
     * @throws IllegalStateException if the operation fails.
     */
    @Override
    public void setValue(long value)
            throws IllegalStateException {
        withLock(() -> {
            bytes.append(offset + VALUE, value, DIGITS);
            return LONG_TRUE;
        });
    }

    @Override
    public long maxSize() {
        return template.length;
    }

    @NotNull
    @Override
    public String toString() {
        try {
            return "value: " + getValue();
        } catch (Exception e) {
            return e.toString();
        }
    }

    @Override
    public long addValue(long delta)
            throws IllegalStateException {
        return withLock(() -> {
            long value = bytes.parseLong(offset + VALUE) + delta;
            bytes.append(offset + VALUE, value, DIGITS);
            return value;
        });
    }

    /**
     * Atomically sets the value to the given updated value if the current value is
     * equal to the expected value.
     *
     * @param expected the expected value.
     * @param value    the new value.
     * @return {@code true} if successful, {@code false} otherwise.
     * @throws IllegalStateException if the operation fails.
     */
    @Override
    public boolean compareAndSwapValue(long expected, long value)
            throws IllegalStateException {
        return withLock(() -> {
            if (bytes.parseLong(offset + VALUE) == expected) {
                bytes.append(offset + VALUE, value, DIGITS);
                return LONG_TRUE;
            }
            return LONG_FALSE;
        }) == LONG_TRUE;
    }
}
