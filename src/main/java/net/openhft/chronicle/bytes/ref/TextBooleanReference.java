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
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.values.BooleanValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Implementation of a reference to a boolean value in text wire format.
 * Textual representations of boolean values are written and read from a {@link BytesStore} instance.
 * " true" and "false" are the textual representations used for boolean values.
 */
public class TextBooleanReference extends AbstractReference implements BooleanValue {

    private static final int FALSE = 'f' | ('a' << 8) | ('l' << 16) | ('s' << 24);
    private static final int TRUE = ' ' | ('t' << 8) | ('r' << 16) | ('u' << 24);

    /**
     * Write a boolean value to a {@link BytesStore} at a given offset.
     * The value is written as a string (" true" or "false").
     *
     * @param value the boolean value to write.
     * @param bytes the {@link BytesStore} to write to.
     * @param offset the position at which to start writing in the {@link BytesStore}.
     * @throws IllegalStateException if the underlying bytes store is closed.
     * @throws BufferOverflowException if there isn't enough space in the {@link BytesStore} to write the value.
     */
    @SuppressWarnings("rawtypes")
    public static void write(final boolean value, final BytesStore bytes, @NonNegative long offset)
            throws IllegalStateException, BufferOverflowException {
        bytes.writeVolatileInt(offset, value ? TRUE : FALSE);
        bytes.writeByte(offset + 4, (byte) 'e');
    }

    /**
     * Get the maximum size in bytes that this object can be serialized to. Always returns 5, the length of the strings "true" and "false".
     *
     * @return the maximum size in bytes this object can be serialized to.
     */
    @Override
    public long maxSize() {
        return 5;
    }

    /**
     * Provides a String representation of the current value of this reference.
     *
     * @return a string representation of the current value.
     */
    @NotNull
    @Override
    public String toString() {
        try {
            return "value: " + getValue();
        } catch (IllegalStateException | BufferUnderflowException e) {
            return e.toString();
        }
    }

    /**
     * Get the current value of this reference.
     *
     * @return the current value.
     * @throws IllegalStateException if the underlying bytes store is closed.
     * @throws BufferUnderflowException if the underlying bytes store cannot provide enough data.
     */
    @Override
    public boolean getValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileInt(offset) == TRUE;
    }

    /**
     * Set the value of this reference.
     *
     * @param value the new value.
     * @throws IllegalStateException if the underlying bytes store is closed.
     */
    @Override
    public void setValue(final boolean value)
            throws IllegalStateException {
        throwExceptionIfClosedInSetter();

        try {
            write(value, bytes, offset);
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }
}
