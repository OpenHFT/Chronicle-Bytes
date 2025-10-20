/*
 * Copyright 2016-2025 chronicle.software
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
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.values.BooleanValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Reference to a fixed-width text boolean.
 * <p>The format is four ASCII bytes containing either {@code " tru"}
 * or {@code "fals"} and includes a simple spin-lock.</p>
 *
 * <p> {@code FALSE} and {@code TRUE} hold the encoded text.
 * <p> These classes target debugging, not production performance.
 */
@SuppressWarnings("rawtypes")
public class TextBooleanReference extends AbstractReference implements BooleanValue {

    private static final int FALSE = 'f' | ('a' << 8) | ('l' << 16) | ('s' << 24);
    private static final int TRUE = ' ' | ('t' << 8) | ('r' << 16) | ('u' << 24);

    /**
     * Writes a boolean value to the specified {@link BytesStore} at the given offset
     * in text wire format.
     *
     * @param value  the boolean value to write.
     * @param bytes  the BytesStore to write to.
     * @param offset the offset at which to write the value.
     * @throws BufferOverflowException If there's not enough space in the buffer to write the value.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @SuppressWarnings("rawtypes")
    public static void write(final boolean value, final BytesStore<?, ?> bytes, @NonNegative long offset)
            throws IllegalStateException, BufferOverflowException {
        bytes.writeVolatileInt(offset, value ? TRUE : FALSE);
        bytes.writeByte(offset + 4, (byte) 'e');
    }

    /**
     * Returns the maximum size of the text representation of the boolean value in bytes.
     *
     * @return the maximum size in bytes.
     */
    @Override
    public long maxSize() {
        return 5;
    }

    /**
     * Returns a string representation of this TextBooleanReference object,
     * displaying the value it represents.
     *
     * @return a string representation of this object.
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
     * @throws BufferUnderflowException If the underlying bytes store cannot provide enough data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public boolean getValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore.readVolatileInt(offset) == TRUE;
    }

    /**
     * Set the value of this reference.
     *
     * @param value the new value.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void setValue(final boolean value)
            throws IllegalStateException {
        throwExceptionIfClosedInSetter();

        write(value, bytesStore, offset);
    }
}
