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

import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

public class BinaryTwoLongReference extends BinaryLongReference implements TwoLongReference {
    @Override
    public long maxSize() {
        return 2 * 8L;
    }

    @NotNull
    @Override
    public String toString() {
        try {
            return bytes == null ? "bytes is null" : "value: " + getValue() + ", value2: " + getValue2();
        } catch (Exception e) {
            return e.toString();
        }
    }

    @Override
    public long getValue2()
            throws IllegalStateException {
        try {
            return bytes.readLong(offset + Long.BYTES);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void setValue2(long value)
            throws IllegalStateException {
        try {
            bytes.writeLong(offset + Long.BYTES, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long getVolatileValue2()
            throws IllegalStateException {
        try {
            return bytes.readVolatileLong(offset + Long.BYTES);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }

    }

    @Override
    public void setVolatileValue2(long value)
            throws IllegalStateException {
        try {
            bytes.writeVolatileLong(offset + Long.BYTES, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void setOrderedValue2(long value)
            throws IllegalStateException {
        try {
            bytes.writeOrderedLong(offset + Long.BYTES, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long addValue2(long delta)
            throws IllegalStateException {
        try {
            return bytes.addAndGetLong(offset + Long.BYTES, delta);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long addAtomicValue2(long delta)
            throws IllegalStateException {
        try {
            return addValue2(delta);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    @Override
    public boolean compareAndSwapValue2(long expected, long value)
            throws IllegalStateException {
        try {
            return bytes.compareAndSwapLong(offset + Long.BYTES, expected, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }
}
