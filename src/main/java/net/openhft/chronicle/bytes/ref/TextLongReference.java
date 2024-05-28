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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.util.ThrowingLongSupplier;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.BytesUtil.roundUpTo8ByteAlign;

/**
 * Implementation of a reference to an array of 64-bit long values in Text wire format.
 * The text representation includes an atomic lock flag along with the value.
 * The format is: {@code !!atomic {  locked: false, value: 00000000000000000000 } }.
 */
@SuppressWarnings("rawtypes")
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
     * Writes the initial structure of a 64-bit long value to the specified {@link Bytes} instance
     * in Text wire format, with the given value.
     *
     * @param bytes the Bytes instance to write to.
     * @param value the long value to be written.
     * @throws BufferOverflowException  If there's not enough space in the buffer to write the value.
     * @throws IllegalArgumentException If an illegal argument is provided.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public static void write(@NotNull Bytes<?> bytes, @NonNegative long value)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        long position = bytes.writePosition();
        bytes.write(template);
        bytes.append(position + VALUE, value, DIGITS);
    }

    /**
     * Executes the given {@link ThrowingLongSupplier} within a lock to ensure thread safety.
     *
     * @param call the ThrowingLongSupplier to be executed.
     * @return the result of ThrowingLongSupplier.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    private <T extends Exception> long withLock(@NotNull ThrowingLongSupplier<T> call)
            throws IllegalStateException {
        try {
            long valueOffset = offset + LOCKED;
            int value = bytesStore.readVolatileInt(valueOffset);
            if (value != FALSE && value != TRUE)
                throw new IllegalStateException("Not a lock value");

            while (true) {
                if (bytesStore.compareAndSwapInt(valueOffset, FALSE, TRUE)) {
                    long t = call.getAsLong();
                    bytesStore.writeOrderedInt(valueOffset, FALSE);
                    return t;
                }
            }
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        } catch (Exception e) {
            throw Jvm.rethrow(e);
        }
    }

    /**
     * Configures the byte store for this reference.
     *
     * @param bytes  the BytesStore instance where the reference is to be stored.
     * @param offset the offset in the byte store where the reference is to be positioned.
     * @param length the length of the reference in bytes.
     * @throws IllegalArgumentException If an illegal argument is provided.
     * @throws BufferOverflowException  If there's not enough space in the buffer to write the reference.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @SuppressWarnings("rawtypes")
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
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public long getValue()
            throws IllegalStateException {
        return withLock(() -> bytesStore.parseLong(offset + VALUE));
    }

    /**
     * Sets the value in the Text wire format.
     *
     * @param value the value to be set.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void setValue(long value)
            throws IllegalStateException {
        withLock(() -> {
            bytesStore.append(offset + VALUE, value, DIGITS);
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
            long value = bytesStore.parseLong(offset + VALUE) + delta;
            bytesStore.append(offset + VALUE, value, DIGITS);
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
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public boolean compareAndSwapValue(long expected, long value)
            throws IllegalStateException {
        return withLock(() -> {
            if (bytesStore.parseLong(offset + VALUE) == expected) {
                bytesStore.append(offset + VALUE, value, DIGITS);
                return LONG_TRUE;
            }
            return LONG_FALSE;
        }) == LONG_TRUE;
    }
}
