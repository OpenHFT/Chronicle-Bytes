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
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

import static net.openhft.chronicle.bytes.internal.BytesInternal.uncheckedCast;

public class OnHeapBytes extends VanillaBytes<byte[]> {
    public static final int MAX_CAPACITY = Bytes.MAX_HEAP_CAPACITY;
    private final boolean elastic;
    private final long capacity;

    /**
     * Constructs an instance of OnHeapBytes using the provided BytesStore. The elasticity of the created OnHeapBytes instance
     * is specified by the elastic parameter.
     * <p>
     * OnHeapBytes allows for the manipulation of byte sequences in an on-heap manner, meaning it relies on JVM's garbage
     * collector for memory management. It can either have a fixed size (not elastic) or allow for dynamic resizing (elastic).
     * If the OnHeapBytes is set to be elastic, its maximum capacity is specified by {@code MAX_CAPACITY}, else it matches
     * the capacity of the provided BytesStore. In both elastic and non-elastic states, this instance can be read/written using cursors.
     *
     * @param bytesStore the BytesStore instance containing the bytes to be managed.
     * @param elastic    a boolean value specifying whether this instance of OnHeapBytes is elastic.
     *                   If {@code true}, the instance is elastic and its capacity can grow up to {@code MAX_CAPACITY}.
     *                   If {@code false}, the instance has a fixed size that matches the capacity of the provided BytesStore.
     * @throws IllegalArgumentException       If the arguments provided are not valid, for instance if the BytesStore's
     *                                        capacity exceeds the {@code MAX_CAPACITY} when the elastic parameter is false.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @SuppressWarnings("this-escape")
    public OnHeapBytes(@NotNull BytesStore<?, ?> bytesStore, boolean elastic)
            throws ClosedIllegalStateException, IllegalArgumentException, ThreadingIllegalStateException {
        super(bytesStore);
        this.elastic = elastic;
        this.capacity = elastic ? MAX_CAPACITY : bytesStore.capacity();

        writePosition(0);
        writeLimit(capacity());
    }

    @Override
    public @NonNegative long capacity() {
        return capacity;
    }

    @Override
    public void ensureCapacity(@NonNegative long desiredCapacity)
            throws IllegalArgumentException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        if (isElastic())
            resize(endOfBuffer);
        else
            throw new BufferOverflowException();
    }

    // the endOfBuffer is the minimum capacity and one byte more than the last addressable byte.
    private void resize(@NonNegative long endOfBuffer)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
        BytesStore<Bytes<byte[]>, byte[]> store = uncheckedCast(BytesStore.wrap(new byte[size]));
        store.reserveTransfer(INIT, this);

        BytesStore<Bytes<byte[]>, byte[]> tempStore = this.bytesStore;
        this.bytesStore.copyTo(store);
        this.bytesStore(store);
        tempStore.release(this);
    }

}
