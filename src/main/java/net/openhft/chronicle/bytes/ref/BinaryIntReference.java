/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.HexDumpBytes.MASK;

/**
 * This class acts as a Binary 32-bit in values. c.f. TextIntReference
 */
public class BinaryIntReference extends AbstractReference implements IntValue {
    public static final int INT_NOT_COMPLETE = Integer.MIN_VALUE;

    @SuppressWarnings("rawtypes")
    @Override
    public void bytesStore(final @NotNull BytesStore bytes, @NonNegative long offset, @NonNegative final long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        if (length != maxSize())
            throw new IllegalArgumentException();
        if (bytes instanceof HexDumpBytes) {
            offset &= MASK;
        }
        super.bytesStore(bytes, offset, length);
    }

    @Override
    public long maxSize() {
        return 4;
    }

    @NotNull
    @Override
    public String toString() {
        if (bytes == null)
            return "bytes is null";
        try {
            return "value: " + getValue();
        } catch (Throwable e) {
            return "value: " + e;
        }
    }

    @Override
    public int getValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes == null ? 0 : bytes.readInt(offset);
    }

    @Override
    public void setValue(int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeInt(offset, value);
    }

    @Override
    public int getVolatileValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileInt(offset);
    }

    @Override
    public void setOrderedValue(int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeOrderedInt(offset, value);
    }

    @Override
    public int addValue(int delta)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.addAndGetInt(offset, delta);
    }

    @Override
    public int addAtomicValue(int delta)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(int expected, int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        return bytes.compareAndSwapInt(offset, expected, value);
    }
}
