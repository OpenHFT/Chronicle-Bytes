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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

public class OnHeapBytes extends VanillaBytes<byte[]> {
    public static final int MAX_CAPACITY = Bytes.MAX_HEAP_CAPACITY;
    private final boolean elastic;
    private final long capacity;

    /**
     * Creates an OnHeapBytes using the bytes in a BytesStore which can be elastic or not as specified.
     * <p>
     * If the OnHeapBytes is specified elastic, its capacity is set to {@code MAX_CAPACITY}, otherwise its capacity will be
     * set to the BytesStore capacity. Whether the OnHeapBytes is elastic or not it can be read/written using cursors.
     *
     * @param bytesStore the specified BytesStore to wrap
     * @param elastic    if <code>true</code> this OnHeapBytes is elastic
     * @throws IllegalStateException    if bytesStore has been released
     * @throws IllegalArgumentException
     */
    public OnHeapBytes(@NotNull BytesStore<?, ?> bytesStore, boolean elastic)
            throws IllegalStateException, IllegalArgumentException {
        super(bytesStore);
        this.elastic = elastic;
        this.capacity = elastic ? MAX_CAPACITY : bytesStore.capacity();

        try {
            writePosition(0);
            writeLimit(capacity());
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public @NonNegative long capacity() {
        return capacity;
    }

    @Override
    public void ensureCapacity(@NonNegative long desiredCapacity) throws IllegalArgumentException, IllegalStateException {
        if (isElastic() && bytesStore.capacity() < desiredCapacity)
            resize(desiredCapacity);
        else
            super.ensureCapacity(desiredCapacity);
    }

    @Override
    public boolean isElastic() {
        return elastic;
    }

    @Override
    protected void writeCheckOffset(@NonNegative long offset, @NonNegative long adding)
            throws BufferOverflowException, IllegalStateException {
        if (offset >= bytesStore.start() && offset + adding >= bytesStore.start()) {
            long writeEnd = offset + adding;
            if (writeEnd > writeLimit)
                throwBeyondWriteLimit(adding, writeEnd);
            if (writeEnd <= bytesStore.safeLimit()) {
                return; // do nothing.
            }
            checkResize(writeEnd);
        } else {
            if (offset < 0) throw new IllegalArgumentException();
            throw new BufferOverflowException();
        }
    }

    private void throwBeyondWriteLimit(@NonNegative long advance, @NonNegative long writeEnd)
            throws DecoratedBufferOverflowException {
        throw new DecoratedBufferOverflowException("attempt to write " + advance + " bytes to " + writeEnd + " limit: " + writeLimit);
    }

    private void checkResize(@NonNegative long endOfBuffer)
            throws BufferOverflowException, IllegalStateException {
        if (isElastic())
            resize(endOfBuffer);
        else
            throw new BufferOverflowException();
    }

    // the endOfBuffer is the minimum capacity and one byte more than the last addressable byte.
    private void resize(@NonNegative long endOfBuffer)
            throws BufferOverflowException, IllegalStateException {
        if (endOfBuffer < 0)
            throw new BufferOverflowException();
        if (endOfBuffer > capacity())
            throw new BufferOverflowException();
        final long realCapacity = realCapacity();
        if (endOfBuffer <= realCapacity) {
            // No resize
            return;
        }

        // Grow by 50%
        long size0 = Math.max(endOfBuffer, realCapacity * 3 / 2);
        // Size must not be more than capacity(), it may break some assumptions in BytesStore or elsewhere
        int size = (int) Math.min(size0, capacity());

        // native block of 128 KiB or more have an individual memory mapping so are more expensive.
        if (endOfBuffer >= 128 << 10)
            Jvm.perf().on(getClass(), "Resizing buffer was " + realCapacity / 1024 + " KB, " +
                    "needs " + (endOfBuffer - realCapacity) + " bytes more, " +
                    "new-size " + size / 1024 + " KB");
        BytesStore<Bytes<byte[]>, byte[]> store;
        try {
            store = (BytesStore<Bytes<byte[]>, byte[]>) BytesStore.wrap(new byte[size]);
            store.reserveTransfer(INIT, this);
        } catch (IllegalStateException e) {
            BufferOverflowException boe = new BufferOverflowException();
            boe.initCause(e);
            throw boe;
        }

        BytesStore<Bytes<byte[]>, byte[]> tempStore = this.bytesStore;
        this.bytesStore.copyTo(store);
        this.bytesStore(store);
        try {
            tempStore.release(this);
        } catch (IllegalStateException e) {
            Jvm.debug().on(getClass(), e);
        }
    }
}
