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

import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class BinaryLongReference extends AbstractReference implements LongReference {
    public static final long LONG_NOT_COMPLETE = -1;
    @Nullable
    private static Set<WeakReference<BinaryLongReference>> binaryLongReferences;

    /**
     * only used for testing
     */
    public static void startCollecting() {
        binaryLongReferences = new CopyOnWriteArraySet<>();
    }

    /**
     * only used for testing
     */
    public static void forceAllToNotCompleteState() {
        binaryLongReferences.forEach(x -> {
            @Nullable BinaryLongReference binaryLongReference = x.get();
            if (binaryLongReference != null) {
                binaryLongReference.setValue(LONG_NOT_COMPLETE);
            }
        });
        binaryLongReferences = null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void bytesStore(@NotNull final BytesStore bytes, final long offset, final long length) throws IllegalStateException, IllegalArgumentException, BufferOverflowException, BufferUnderflowException {
        if (length != maxSize())
            throw new IllegalArgumentException();

        super.bytesStore(bytes, offset, length);
    }

    @Override
    public long maxSize() {
        return 8;
    }

    @NotNull
    public String toString() {
        return bytes == null ? "bytes is null" : "value: " + getValue();
    }

    @Override
    public long getValue() {
        return bytes == null ? 0L : bytes.readLong(offset);
    }

    @Override
    public void setValue(long value) {
        bytes.writeLong(offset, value);
    }

    @Override
    public long getVolatileValue() {
        return bytes.readVolatileLong(offset);
    }

    @Override
    public void setOrderedValue(long value) {
        bytes.writeOrderedLong(offset, value);
    }

    @Override
    public long addValue(long delta) {
        return bytes.addAndGetLong(offset, delta);
    }

    @Override
    public long addAtomicValue(long delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        if (value == LONG_NOT_COMPLETE && binaryLongReferences != null)
            binaryLongReferences.add(new WeakReference<>(this));
        return bytes.compareAndSwapLong(offset, expected, value);
    }
}
