package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

public class OnHeapBytes extends VanillaBytes<byte[]> {
    public static final int MAX_CAPACITY = Integer.MAX_VALUE - 7;
    private final boolean elastic;
    private final long capacity;

    public OnHeapBytes(@NotNull BytesStore bytesStore, boolean elastic) throws IllegalStateException {
        super(bytesStore);
        this.elastic = elastic;
        this.capacity = elastic ? MAX_CAPACITY : bytesStore.capacity();

        writePosition(0);
        writeLimit(capacity());
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public boolean isElastic() {
        return elastic;
    }

    @Override
    protected void writeCheckOffset(long offset, long adding)
            throws BufferOverflowException {
        if (offset >= bytesStore.start()) {
            long writeEnd = offset + adding;
            if (writeEnd <= bytesStore.safeLimit()) {
                return; // do nothing.
            }
            checkResize(writeEnd);
        } else {
            throw new BufferOverflowException();
        }
    }

    private void checkResize(long endOfBuffer)
            throws BufferOverflowException {
        if (isElastic())
            resize(endOfBuffer);
        else
            throw new BufferOverflowException();
    }

    // the endOfBuffer is the minimum capacity and one byte more than the last addressable byte.
    private void resize(long endOfBuffer)
            throws BufferOverflowException {
        if (endOfBuffer < 0)
            throw new BufferOverflowException();
        if (endOfBuffer > capacity())
            throw new BufferOverflowException();
        final long realCapacity = realCapacity();
        if (endOfBuffer <= realCapacity) {
//            System.out.println("no resize " + endOfBuffer + " < " + realCapacity);
            return;
        }

        // Grow by 50%
        long size0 = Math.max(endOfBuffer, realCapacity * 3 / 2);
        // Size must not be more than capacity(), it may break some assumptions in BytesStore or elsewhere
        int size = (int) Math.min(size0, capacity());

        //        System.out.println("resize " + endOfBuffer + " to " + size);
        if (endOfBuffer > 1 << 20)
            Jvm.warn().on(getClass(), "Resizing buffer was " + realCapacity / 1024 + " KB, " +
                    "needs " + (endOfBuffer - realCapacity) + " bytes more, " +
                    "new-size " + size / 1024 + " KB");
        HeapBytesStore store;
        try {
            store = HeapBytesStore.wrap(new byte[size]);
            store.reserveTransfer(INIT, this);
        } catch (IllegalArgumentException e) {
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
