/*
 * Copyright (c) 2016-2022 chronicle.software
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

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.values.BooleanValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Implementation of a reference to a 32-bit in in text wire format.
 */
public class TextBooleanReference extends AbstractReference implements BooleanValue {

    private static final int FALSE = 'f' | ('a' << 8) | ('l' << 16) | ('s' << 24);
    private static final int TRUE = ' ' | ('t' << 8) | ('r' << 16) | ('u' << 24);

    @SuppressWarnings("rawtypes")
    public static void write(final boolean value, final BytesStore bytes, @NonNegative long offset)
            throws IllegalStateException, BufferOverflowException {
        bytes.writeVolatileInt(offset, value ? TRUE : FALSE);
        bytes.writeByte(offset + 4, (byte) 'e');
    }

    @Override
    public long maxSize() {
        return 5;
    }

    @NotNull
    @Override
    public String toString() {
        try {
            return "value: " + getValue();
        } catch (IllegalStateException | BufferUnderflowException e) {
            return e.toString();
        }
    }

    @Override
    public boolean getValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileInt(offset) == TRUE;
    }

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
