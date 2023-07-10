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

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.values.BooleanValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.HexDumpBytes.MASK;

/**
 * BinaryBooleanReference is a concrete implementation of the BooleanValue interface.
 * It provides methods to read and write boolean values from and to a BytesStore in binary format.
 */
public class BinaryBooleanReference extends AbstractReference implements BooleanValue {

    private static final byte FALSE = (byte) 0xB0;
    private static final byte TRUE = (byte) 0xB1;

    /**
     * Defines how to write to the bytes store.
     * @param bytes  The byte store where the data should be written
     * @param offset The position in the byte store where the write should start
     * @param length The number of bytes that should be written
     * @throws IllegalStateException      If an illegal condition has occurred
     * @throws IllegalArgumentException  If an illegal argument was provided
     * @throws BufferOverflowException If the data cannot fit in the target buffer
     */
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

    /**
     * Returns the maximum size of the byte representation of a boolean value.
     * @return The maximum size of a boolean in bytes
     */
    @Override
    public long maxSize() {
        return 1;
    }

    /**
     * Reads a boolean value from the bytes store.
     * @return The read boolean value
     * @throws IllegalStateException      If an illegal condition has occurred
     * @throws BufferUnderflowException If the bytes store contains insufficient data
     */
    @Override
    public boolean getValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        byte b = bytes.readByte(offset);
        if (b == FALSE)
            return false;
        if (b == TRUE)
            return true;

        throw new IllegalStateException("unexpected code=" + b);

    }

    /**
     * Writes a boolean value to the bytes store.
     * @param flag The boolean value to write
     * @throws IllegalStateException If an illegal condition has occurred
     */
    @Override
    public void setValue(final boolean flag)
            throws IllegalStateException {
        throwExceptionIfClosed();

        try {
            bytes.writeByte(offset, flag ? TRUE : FALSE);
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }
}
