/*
 * Copyright 2016-2020 chronicle.software
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

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.NativeBytesStore.nativeStoreWithFixedCapacity;
import static net.openhft.chronicle.bytes.NoBytesStore.noBytesStore;

/**
 * Elastic memory accessor which can wrap either a ByteBuffer or malloc'ed memory.
 * <p>
 * <p>This class can wrap <i>heap</i> ByteBuffers, called <i>Native</i>Bytes for historical reasons.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class NativeBytes<Underlying>
        extends VanillaBytes<Underlying> {
    private static final boolean BYTES_GUARDED = Jvm.getBoolean("bytes.guarded");
    private static boolean s_newGuarded = BYTES_GUARDED;
    private final long capacity;

    public NativeBytes(@NotNull final BytesStore store, final long capacity) throws IllegalStateException {
        super(store, 0, capacity);
        this.capacity = capacity;
    }

    public NativeBytes(@NotNull final BytesStore store) throws IllegalStateException {
        super(store, 0, store.capacity());
        capacity = store.capacity();
    }

    /**
     * For testing
     *
     * @return will new NativeBytes be guarded.
     */
    public static boolean areNewGuarded() {
        return s_newGuarded;
    }

    /**
     * turn guarding on/off. Can be enabled by assertion with
     * <code>
     * assert NativeBytes.setNewGuarded(true);
     * </code>
     *
     * @param guarded turn on if true
     */
    public static boolean setNewGuarded(final boolean guarded) {
        s_newGuarded = guarded;
        return true;
    }

    /**
     * For testing
     */
    public static void resetNewGuarded() {
        s_newGuarded = BYTES_GUARDED;
    }

    @NotNull
    public static NativeBytes<Void> nativeBytes() {
        try {
            return NativeBytes.wrapWithNativeBytes(noBytesStore(), Bytes.MAX_CAPACITY);
        } catch (IllegalStateException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    public static NativeBytes<Void> nativeBytes(final long initialCapacity) throws IllegalArgumentException {
        @NotNull final NativeBytesStore<Void> store = nativeStoreWithFixedCapacity(initialCapacity);
        try {
            try {
                return NativeBytes.wrapWithNativeBytes(store, Bytes.MAX_CAPACITY);
            } finally {
                store.release(INIT);
            }
        } catch (IllegalStateException e) {
            throw new AssertionError(e);
        }
    }

    public static BytesStore<Bytes<Void>, Void> copyOf(@NotNull final Bytes bytes) {
        final long remaining = bytes.readRemaining();

        try {
            final NativeBytes<Void> bytes2 = Bytes.allocateElasticDirect(remaining);
            bytes2.write(bytes, 0, remaining);
            return bytes2;
        } catch (IllegalArgumentException | BufferOverflowException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    private static long alignToPageSize(final long size) {
        long mask = OS.pageSize() - 1;
        return (size + mask) & ~mask;
    }

    @NotNull
    public static <T> NativeBytes<T> wrapWithNativeBytes(@NotNull final BytesStore<?, T> bs, long capacity) {
        return s_newGuarded
                ? new GuardedNativeBytes(bs, capacity)
                : new NativeBytes<>(bs, capacity);
    }

    protected static <T> long maxCapacityFor(@NotNull BytesStore<?, T> bs) {
        return bs.underlyingObject() instanceof ByteBuffer
                || bs.underlyingObject() instanceof byte[]
                ? MAX_HEAP_CAPACITY
                : Bytes.MAX_CAPACITY;
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    protected void writeCheckOffset(final long offset, final long adding) throws BufferOverflowException {
        if (offset >= bytesStore.start()) {
            final long writeEnd = offset + adding;
            if (writeEnd <= bytesStore.safeLimit()) {
                return; // do nothing.
            }
            if (writeEnd >= capacity)
                throw new BufferOverflowException(/*"Write exceeds capacity"*/);
            checkResize(writeEnd);
        } else {
            throw new BufferOverflowException();
        }
    }

    @Override
    void prewriteCheckOffset(long offset, long subtracting) throws BufferOverflowException {
        if (offset - subtracting >= bytesStore.start()) {
            if (offset <= bytesStore.safeLimit()) {
                return; // do nothing.
            }
            if (offset >= capacity)
                throw new BufferOverflowException(/*"Write exceeds capacity"*/);
            checkResize(offset);
        } else {
            throw new BufferOverflowException();
        }
    }

    @Override
    public void ensureCapacity(final long size) throws IllegalArgumentException {
        try {
            assert size >= 0;
            writeCheckOffset(writePosition(), size);
        } catch (BufferOverflowException e) {
            IllegalArgumentException iae = new IllegalArgumentException("Bytes cannot be resized to " + size + " limit: " + capacity());
            iae.printStackTrace();
            throw iae;
        }
    }

    private void checkResize(final long endOfBuffer) throws BufferOverflowException {
        if (isElastic())
            resize(endOfBuffer);
        else
            throw new BufferOverflowException();
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    // the endOfBuffer is the minimum capacity and one byte more than the last addressable byte.
    private void resize(final long endOfBuffer)
            throws BufferOverflowException {
        throwExceptionIfReleased();
        if (endOfBuffer < 0)
            throw new DecoratedBufferOverflowException(endOfBuffer + "< 0");
        if (endOfBuffer > capacity())
            throw new DecoratedBufferOverflowException(endOfBuffer + ">" + capacity());
        final long realCapacity = realCapacity();
        if (endOfBuffer <= realCapacity) {
//            System.out.println("no resize " + endOfBuffer + " < " + realCapacity);
            return;
        }

        // Grow by 50%
        long size = Math.max(endOfBuffer + 7, realCapacity * 3 / 2 + 32);
        if (isDirectMemory() || size > MAX_HEAP_CAPACITY) {
            // Allocate direct memory of page granularity
            size = alignToPageSize(size);
        } else {
            size &= ~0x7;
        }
        // Cap the size with capacity() again
        size = Math.min(size, capacity());

        final boolean isByteBufferBacked = bytesStore.underlyingObject() instanceof ByteBuffer;
        if (isByteBufferBacked && size > MAX_HEAP_CAPACITY) {

            // Add a stack trace to this relatively unusual event which will
            // enable tracing of potentially derailed code or excessive buffer use.
            final StackTrace stackTrace = new StackTrace();
            final String stack = BytesUtil.asString("Calling stack is", stackTrace);

            Jvm.warn().on(getClass(), "Going to try to replace ByteBuffer-backed BytesStore with " +
                    "raw NativeBytesStore to grow to " + size / 1024 + " KB. If later it is assumed that " +
                    "this bytes' underlyingObject() is ByteBuffer, NullPointerException is likely to be thrown. " +
                    stack);
        }
//        System.out.println("resize " + endOfBuffer + " to " + size);
        if (endOfBuffer > 1 << 20)
            Jvm.warn().on(getClass(), "Resizing buffer was " + realCapacity / 1024 + " KB, " +
                    "needs " + (endOfBuffer - realCapacity) + " bytes more, " +
                    "new-size " + size / 1024 + " KB");
        final BytesStore store;
        int position = 0;
        try {
            if (isByteBufferBacked && size <= MAX_HEAP_CAPACITY) {
                position = ((ByteBuffer) bytesStore.underlyingObject()).position();
                store = allocateNewByteBufferBackedStore(Maths.toInt32(size));
            } else {
                store = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(size);
            }
            store.reserveTransfer(INIT, this);
        } catch (IllegalArgumentException e) {
            BufferOverflowException boe = new BufferOverflowException();
            boe.initCause(e);
            throw boe;
        }

        throwExceptionIfReleased();
        @Nullable final BytesStore<Bytes<Underlying>, Underlying> tempStore = this.bytesStore;
        this.bytesStore.copyTo(store);
        this.bytesStore(store);
        try {
            tempStore.release(this);
        } catch (IllegalStateException e) {
            Jvm.debug().on(getClass(), e);
        }

        if (this.bytesStore.underlyingObject() instanceof ByteBuffer) {
            @Nullable final ByteBuffer byteBuffer = (ByteBuffer) this.bytesStore.underlyingObject();
            byteBuffer.position(0);
            byteBuffer.limit(byteBuffer.capacity());
            byteBuffer.position(position);
        }
    }

    @NotNull
    private BytesStore allocateNewByteBufferBackedStore(final int size) {
        if (isDirectMemory()) {
            return NativeBytesStore.elasticByteBuffer(size, capacity());
        } else {
            return HeapBytesStore.wrap(ByteBuffer.allocate(size));
        }
    }

    @NotNull
    @Override
    public Bytes<Underlying> write(@NotNull final byte[] bytes,
                                   final int offset,
                                   final int length) throws BufferOverflowException, IllegalArgumentException {
        if (length > writeRemaining())
            throw new BufferOverflowException();
        ensureCapacity(length);
        super.write(bytes, offset, length);
        return this;
    }

    @NotNull
    public Bytes<Underlying> write(@NotNull final BytesStore bytes,
                                   final long offset,
                                   final long length) throws BufferOverflowException, IllegalArgumentException, BufferUnderflowException {
        ensureCapacity(length);
        super.write(bytes, offset, length);
        return this;
    }

    @Override
    @NotNull
    public NativeBytes writeSome(@NotNull final Bytes bytes) {
        try {
            long length = Math.min(bytes.readRemaining(), writeRemaining());
            if (length + writePosition() >= 1 << 20)
                length = Math.min(bytes.readRemaining(), realCapacity() - writePosition());
            final long offset = bytes.readPosition();
            ensureCapacity(length);
            optimisedWrite(bytes, offset, length);
            if (length == bytes.readRemaining()) {
                bytes.clear();
            } else {
                bytes.readSkip(length);
                if (bytes.writePosition() > bytes.realCapacity() / 2)
                    bytes.compact();
            }
            return this;
        } catch (IllegalArgumentException | BufferUnderflowException | BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected long writeOffsetPositionMoved(final long adding, final long advance) throws BufferOverflowException {
        final long oldPosition = writePosition;
        if (writePosition < bytesStore.start())
            throw new BufferOverflowException();
        final long writeEnd = writePosition + adding;
        if (writeEnd > writeLimit)
            throwBeyondWriteLimit(advance, writeEnd);
        else if (writeEnd > bytesStore.safeLimit())
            checkResize(writeEnd);
        this.writePosition = writePosition + advance;
        return oldPosition;
    }

    private void throwBeyondWriteLimit(long advance, long writeEnd) {
        throw new DecoratedBufferOverflowException("attempt to write " + advance + " bytes to " + writeEnd + " limit: " + writeLimit);
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeByte(final byte i8) throws BufferOverflowException {
        final long offset = writeOffsetPositionMoved(1, 1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> write8bit(@Nullable final BytesStore bs) throws BufferOverflowException {
        if (bs == null) {
            writeStopBit(-1);
        } else {
            final long offset = bs.readPosition();
            final long readRemaining = Math.min(writeRemaining(), bs.readLimit() - offset);
            writeStopBit(readRemaining);
            write(bs, offset, readRemaining);
        }
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLong(final long i64) throws BufferOverflowException {
        final long offset = writeOffsetPositionMoved(8L, 8L);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @Override
    public long readRemaining() {
        return writePosition - readPosition;
    }

    public final static class NativeSubBytes extends SubBytes {
        private final NativeBytesStore nativeBytesStore;

        public NativeSubBytes(@NotNull final BytesStore bytesStore,
                              final long start,
                              final long capacity) throws IllegalStateException {
            super(bytesStore, start, capacity);
            nativeBytesStore = (NativeBytesStore) this.bytesStore;
        }

        @Override
        public long read(final long offsetInRDI,
                         @NotNull final byte[] bytes,
                         final int offset,
                         final int length) {

            final int len = (int) Math.min(length, readLimit() - offsetInRDI);
            int i;
            final long address = nativeBytesStore.address + nativeBytesStore.translate(offsetInRDI);
            for (i = 0; i < len - 7; i += 8)
                UnsafeMemory.unsafePutLong(bytes, i, nativeBytesStore.memory.readLong(address + i));
            if (i < len - 3) {
                UnsafeMemory.unsafePutInt(bytes, i, nativeBytesStore.memory.readInt(address + i));
                i += 4;
            }
            for (; i < len; i++)
                UnsafeMemory.unsafePutByte(bytes, i, nativeBytesStore.memory.readByte(address + i));
            return len;
        }
    }
}