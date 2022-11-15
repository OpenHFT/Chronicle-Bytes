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
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.StackTrace;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.BytesStore.nativeStoreWithFixedCapacity;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Elastic memory accessor which can wrap either a ByteBuffer or malloc'ed memory.
 * <p>
 * <p>This class can wrap <i>heap</i> ByteBuffers, called <i>Native</i>Bytes for historical reasons.
 *
 * @param <U> Underlying type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class NativeBytes<U>
        extends VanillaBytes<U> {
    private static final boolean BYTES_GUARDED = Jvm.getBoolean("bytes.guarded");
    private static boolean newGuarded = BYTES_GUARDED;
    private long capacity;

    public NativeBytes(@NotNull final BytesStore store, @NonNegative final long capacity)
            throws IllegalStateException, IllegalArgumentException {
        super(store, 0, capacity);
        this.capacity = capacity;
    }

    public NativeBytes(@NotNull final BytesStore store)
            throws IllegalStateException, IllegalArgumentException {
        this(store, store.capacity());
    }

    /**
     * For testing
     *
     * @return will new NativeBytes be guarded.
     */
    public static boolean areNewGuarded() {
        return newGuarded;
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
        newGuarded = guarded;
        return true;
    }

    /**
     * For testing
     */
    public static void resetNewGuarded() {
        newGuarded = BYTES_GUARDED;
    }

    @NotNull
    public static NativeBytes<Void> nativeBytes() {
        try {
            return NativeBytes.wrapWithNativeBytes(BytesStore.empty(), Bytes.MAX_CAPACITY);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    public static NativeBytes<Void> nativeBytes(@NonNegative final long initialCapacity)
            throws IllegalArgumentException {
        @NotNull final BytesStore<?, Void> store = nativeStoreWithFixedCapacity(initialCapacity);
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

    @Deprecated(/* to be removed in x.26 */)
    public static BytesStore<Bytes<Void>, Void> copyOf(@NotNull final Bytes<?> bytes)
            throws IllegalStateException {
        return BytesUtil.copyOf(bytes);
    }

    private static long alignToPageSize(final long size) {
        final long mask = OS.pageSize() - 1L;
        return (size + mask) & ~mask;
    }

    @NotNull
    public static <T> NativeBytes<T> wrapWithNativeBytes(@NotNull final BytesStore<?, T> bs, @NonNegative long capacity)
            throws IllegalStateException, IllegalArgumentException {
        requireNonNull(bs);
        return newGuarded
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
    public @NonNegative long capacity() {
        return capacity;
    }

    @Override
    protected void writeCheckOffset(final @NonNegative long offset, final @NonNegative long adding)
            throws BufferOverflowException, IllegalStateException {
        if (offset >= bytesStore.start() && offset + adding >= bytesStore.start()) {
            final long writeEnd = offset + adding;
            // Always resize if we are backed by a SingletonEmptyByteStore as this is shared and does not provide all functionality
            if (writeEnd <= bytesStore.safeLimit() && !isImmutableEmptyByteStore()) {
                return; // do nothing.
            }
            if (writeEnd > capacity)
                throw newDBOE(writeEnd);
            checkResize(writeEnd);
        } else {
            throw new BufferOverflowException();
        }
    }

    @NotNull
    private DecoratedBufferOverflowException newDBOE(long writeEnd) {
        return new DecoratedBufferOverflowException("Write cannot grow Bytes to " + writeEnd + ", capacity: " + capacity);
    }

    @Override
    void prewriteCheckOffset(@NonNegative long offset, long subtracting)
            throws BufferOverflowException, IllegalStateException {
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
    public void ensureCapacity(final @NonNegative long desiredCapacity)
            throws IllegalArgumentException, IllegalStateException {

        if (desiredCapacity < 0) throw new IllegalArgumentException();
        assert DISABLE_SINGLE_THREADED_CHECK || threadSafetyCheck(true);
        writeCheckOffset(desiredCapacity, 0);
    }


    private void checkResize(@NonNegative final long endOfBuffer)
            throws BufferOverflowException, IllegalStateException {
        if (isElastic())
            resize(endOfBuffer);
        else
            throw new BufferOverflowException();
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    @Override
    public boolean isEqual(@NonNegative long start, @NonNegative long length, String s) {
        return bytesStore.isEqual(start, length, s);
    }

    // the endOfBuffer is the minimum capacity and one byte more than the last addressable byte.
    private void resize(@NonNegative final long endOfBuffer)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfReleased();
        if (endOfBuffer < 0)
            throw new DecoratedBufferOverflowException(endOfBuffer + "< 0");
        if (endOfBuffer > capacity())
            throw new DecoratedBufferOverflowException(endOfBuffer + ">" + capacity());
        final long realCapacity = realCapacity();
        if (endOfBuffer <= realCapacity && !(isImmutableEmptyByteStore())) {
            //  No resize
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
        // native block of 128 KiB or more have an individual memory mapping so are more expensive.
        if (endOfBuffer >= 128 << 10 && realCapacity > 0)
            Jvm.perf().on(getClass(), "Resizing buffer was " + realCapacity / 1024 + " KB, " +
                    "needs " + (endOfBuffer - realCapacity) + " bytes more, " +
                    "new-size " + size / 1024 + " KB");
        resizeHelper(size, isByteBufferBacked);
    }

    private void resizeHelper(@NonNegative final long size,
                              final boolean isByteBufferBacked) {
        final BytesStore store;
        int position = 0;
        try {
            if (isByteBufferBacked && size <= MAX_HEAP_CAPACITY) {
                position = ((ByteBuffer) bytesStore.underlyingObject()).position();
                store = allocate(size);
            } else {
                store = BytesStore.lazyNativeBytesStoreWithFixedCapacity(size);
                if (referenceCounted.unmonitored())
                    AbstractReferenceCounted.unmonitor(store);
            }
            store.reserveTransfer(INIT, this);
        } catch (IllegalArgumentException e) {
            BufferOverflowException boe = new BufferOverflowException();
            boe.initCause(e);
            throw boe;
        }

        throwExceptionIfReleased();
        @Nullable final BytesStore<Bytes<U>, U> tempStore = this.bytesStore;
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
    private BytesStore allocate(@NonNegative long size) {
        final BytesStore store;
        try {
            store = allocateNewByteBufferBackedStore(Maths.toInt32(size));
        } catch (ArithmeticException e) {
            throw new AssertionError(e);
        }
        return store;
    }

    @Override
    protected void bytesStore(@NotNull BytesStore<Bytes<U>, U> bytesStore) {
        if (capacity < bytesStore.capacity())
            capacity = bytesStore.capacity();
        super.bytesStore(bytesStore);
    }

    @Override
    public void bytesStore(@NotNull BytesStore<Bytes<U>, U> byteStore, @NonNegative long offset, @NonNegative long length) throws IllegalStateException, IllegalArgumentException, BufferUnderflowException {
        requireNonNull(byteStore);
        if (capacity < offset + length)
            capacity = offset + length;
        super.bytesStore(byteStore, offset, length);
    }

    @NotNull
    private BytesStore allocateNewByteBufferBackedStore(@NonNegative final int size) {
        if (isDirectMemory()) {
            return BytesStore.elasticByteBuffer(size, capacity());
        } else {
            return BytesStore.wrap(ByteBuffer.allocate(size));
        }
    }

    @Override
    @NotNull
    public NativeBytes writeSome(@NotNull final Bytes<?> bytes)
            throws IllegalStateException {
        requireNonNull(bytes);
        ReportUnoptimised.reportOnce();
        try {
            long length = Math.min(bytes.readRemaining(), writeRemaining());
            if (length + writePosition() >= 1 << 20)
                length = Math.min(bytes.readRemaining(), realCapacity() - writePosition());
            final long offset = bytes.readPosition();
            ensureCapacity(writePosition() + length);
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
    protected long writeOffsetPositionMoved(final @NonNegative long adding, final @NonNegative long advance)
            throws BufferOverflowException, IllegalStateException {
        final long oldPosition = writePosition();
        if (writePosition() < bytesStore.start())
            throw new BufferOverflowException();
        final long writeEnd = writePosition() + adding;
        if (writeEnd > writeLimit)
            throwBeyondWriteLimit(advance, writeEnd);
        else if (writeEnd > bytesStore.safeLimit())
            checkResize(writeEnd);
        uncheckedWritePosition(writePosition() + advance);
        return oldPosition;
    }

    private void throwBeyondWriteLimit(@NonNegative long advance, @NonNegative long writeEnd)
            throws DecoratedBufferOverflowException {
        throw new DecoratedBufferOverflowException("attempt to write " + advance + " bytes to " + writeEnd + " limit: " + writeLimit);
    }

    @NotNull
    @Override
    public Bytes<U> writeByte(final byte i8)
            throws BufferOverflowException, IllegalStateException {
        final long offset = writeOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeLong(final long i64)
            throws BufferOverflowException, IllegalStateException {
        final long offset = writeOffsetPositionMoved(8L);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @Override
    public long readRemaining() {
        return writePosition() - readPosition;
    }
}