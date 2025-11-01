/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

import static net.openhft.chronicle.core.Jvm.uncheckedCast;

/**
 * {@link VanillaBytes} backed by an on heap {@code byte[]}.
 */
public class OnHeapBytes extends VanillaBytes<byte[]> {
    public static final int MAX_CAPACITY = Bytes.MAX_HEAP_CAPACITY;
    private final boolean elastic;
    private final long capacity;

    /**
     * Constructs an instance backed by {@code bytesStore}.
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
        BytesStore<?, byte[]> store = uncheckedCast(BytesStore.wrap(new byte[size]));
        store.reserveTransfer(INIT, this);

        BytesStore<?, byte[]> tempStore = this.bytesStore;
        this.bytesStore.copyTo(store);
        this.bytesStore(store);
        tempStore.release(this);
    }
}
