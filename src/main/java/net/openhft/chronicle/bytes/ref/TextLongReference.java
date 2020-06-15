/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.function.LongSupplier;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.BytesUtil.roundUpTo8ByteAlign;

/**
 * reference to an array fo 32-bit in values in Text wire format.
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

    @SuppressWarnings("rawtypes")
    public static void write(@NotNull Bytes bytes, long value) throws BufferOverflowException, IllegalArgumentException {
        long position = bytes.writePosition();
        bytes.write(template);
        bytes.append(position + VALUE, value, DIGITS);
    }

    private long withLock(@NotNull LongSupplier call) throws IllegalStateException, BufferUnderflowException {
        long valueOffset = offset + LOCKED;
        int value = bytes.readVolatileInt(valueOffset);
        if (value != FALSE && value != TRUE)
            throw new IllegalStateException("Not a lock value");
        try {
            while (true) {
                if (bytes.compareAndSwapInt(valueOffset, FALSE, TRUE)) {
                    long t = call.getAsLong();
                    bytes.writeOrderedInt(valueOffset, FALSE);
                    return t;
                }
            }
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void bytesStore(@NotNull final BytesStore bytes, long offset, long length) {
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

    @Override
    public long getValue() {
        return withLock(() -> bytes.parseLong(offset + VALUE));
    }

    @Override
    public void setValue(long value) {
        withLock(() -> {
            bytes.append(offset + VALUE, value, DIGITS);
            return LONG_TRUE;
        });
    }

    @Override
    public long getVolatileValue() {
        return getValue();
    }

    @Override
    public long maxSize() {
        return template.length;
    }

    @Override
    public void setOrderedValue(long value) {
        setValue(value);
    }

    @Override
    public void setVolatileValue(long value) {
        setValue(value);
    }

    @NotNull
    public String toString() {
        return "value: " + getValue();
    }

    @Override
    public long addValue(long delta) {
        return withLock(() -> {
            long value = bytes.parseLong(offset + VALUE) + delta;
            bytes.append(offset + VALUE, value, DIGITS);
            return value;
        });
    }

    @Override
    public long addAtomicValue(long delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        return withLock(() -> {
            if (bytes.parseLong(offset + VALUE) == expected) {
                bytes.append(offset + VALUE, value, DIGITS);
                return LONG_TRUE;
            }
            return LONG_FALSE;
        }) == LONG_TRUE;
    }
}
