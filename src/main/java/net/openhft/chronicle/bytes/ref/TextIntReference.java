/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntSupplier;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * Implementation of a reference to a 32-bit in in text wire format.
 */
public class TextIntReference implements IntValue, Byteable {
    private static final byte[] template = "!!atomic { locked: false, value: 0000000000 }".getBytes(ISO_8859_1);
    private static final int FALSE = 'f' | ('a' << 8) | ('l' << 16) | ('s' << 24);
    private static final int TRUE = ' ' | ('t' << 8) | ('r' << 16) | ('u' << 24);
    private static final int UNINITIALIZED = 0;
    private static final int INT_TRUE = 1;
    private static final int INT_FALSE = 0;
    private static final int LOCKED = 19;
    private static final int VALUE = 33;
    private static final int DIGITS = 10;
    private BytesStore bytes;
    private long offset;

    public static void write(@NotNull Bytes bytes, int value) {
        long position = bytes.writePosition();
        bytes.write(template);
        bytes.append(position + VALUE, value, DIGITS);
    }

    private int withLock(@NotNull IntSupplier call) {
        long valueOffset = offset + LOCKED;
        int value = bytes.readVolatileInt(valueOffset);
        if (value != FALSE && value != TRUE)
            throw new IllegalStateException();
        while (true) {
            if (bytes.compareAndSwapInt(valueOffset, FALSE, TRUE)) {
                int t = call.getAsInt();
                bytes.writeOrderedInt(valueOffset, FALSE);
                return t;
            }
        }
    }

    @Override
    public int getValue() {
        return withLock(() -> (int) bytes.parseLong(offset + VALUE));
    }

    @Override
    public void setValue(int value) {
        withLock(() -> {
            bytes.append(offset + VALUE, value, DIGITS);
            return INT_TRUE;
        });
    }

    @Override
    public int getVolatileValue() {
        return getValue();
    }

    @Override
    public void setOrderedValue(int value) {
        setValue(value);
    }

    @Override
    public int addValue(int delta) {
        return withLock(() -> {
            long value = bytes.parseLong(offset + VALUE) + delta;
            bytes.append(offset + VALUE, value, DIGITS);
            return (int) value;
        });
    }

    @Override
    public int addAtomicValue(int delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(int expected, int value) {
        return withLock(() -> {
            if (bytes.parseLong(offset + VALUE) == expected) {
                bytes.append(offset + VALUE, value, DIGITS);
                return INT_TRUE;
            }
            return INT_FALSE;
        }) == INT_TRUE;
    }

    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        if (length != template.length)
            throw new IllegalArgumentException(length + " != " + template.length);

        this.bytes = bytes;
        this.offset = offset;

        if (bytes.readInt(offset) == UNINITIALIZED)
            bytes.write(offset, template);
    }

    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long maxSize() {
        return template.length;
    }

    @NotNull
    public String toString() {
        return "value: " + getValue();
    }
}
