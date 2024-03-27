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
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.BytesStore.nativeStoreWithFixedCapacity;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * NativeBytes is a subclass of VanillaBytes which can wrap either a ByteBuffer or malloc'ed memory.
 * It provides flexibility in handling byte arrays which can be of arbitrary sizes and can grow dynamically.
 * The class is called NativeBytes because it deals with 'native' ByteBuffers or memory directly allocated
 * from the operating system which is on heap. It is also capable of handling memory allocation, resizing,
 * and checking boundaries to prevent overflows.
 * <p>
 * The class can be parameterized with a type <U> which represents the underlying type that the byte buffers
 * are intended to represent. This provides a way to use NativeBytes for any type that can be represented as bytes.
 *
 * @param <U> This represents the underlying type that the byte buffers are intended to represent.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class NativeBytes<U>
        extends VanillaBytes<U> {
    private static final boolean BYTES_GUARDED = Jvm.getBoolean("bytes.guarded");
    private static boolean newGuarded = BYTES_GUARDED;
    protected long capacity;

    /**
     * Constructs a new instance of NativeBytes with the specified BytesStore and capacity.
     *
     * @param store    the BytesStore to be used for the newly constructed instance
     * @param capacity the capacity to be used for the newly constructed instance
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public NativeBytes(@NotNull final BytesStore store, @NonNegative final long capacity)
            throws IllegalArgumentException, ClosedIllegalStateException {
        super(store, 0, capacity);
        this.capacity = capacity;
    }

    /**
     * Constructs a new instance of NativeBytes with the specified BytesStore and the store's capacity.
     *
     * @param store the BytesStore to be used for the newly constructed instance
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public NativeBytes(@NotNull final BytesStore store)
            throws IllegalArgumentException, ClosedIllegalStateException {
        this(store, store.capacity());
    }

    /**
     * Checks if new NativeBytes instances will be guarded or not.
     *
     * @return true if new NativeBytes instances will be guarded, false otherwise
     */
    public static boolean areNewGuarded() {
        return newGuarded;
    }

    /**
     * Sets the guarded state for new NativeBytes instances.
     *
     * @param guarded true to turn on guarding for new NativeBytes instances, false to turn it off
     * @return true if the operation is successful, false otherwise
     */
    public static boolean setNewGuarded(final boolean guarded) {
        newGuarded = guarded;
        return true;
    }

    /**
     * Resets the guarded state for new NativeBytes instances to its default value.
     */
    public static void resetNewGuarded() {
        newGuarded = BYTES_GUARDED;
    }

    /**
     * Creates a new instance of NativeBytes with an empty BytesStore and maximum capacity.
     *
     * @return A new instance of NativeBytes.
     * @throws AssertionError If there's an error during the wrapping process.
     */
    @NotNull
    public static NativeBytes<Void> nativeBytes() {
        return NativeBytes.wrapWithNativeBytes(BytesStore.empty(), Bytes.MAX_CAPACITY);
    }

    /**
     * Creates a new instance of NativeBytes with a specific initial capacity.
     *
     * @param initialCapacity The initial capacity of the NativeBytes instance.
     * @return A new instance of NativeBytes.
     * @throws IllegalArgumentException If the initial capacity is not valid.
     * @throws AssertionError           If there's an error during the wrapping process.
     */
    @NotNull
    public static NativeBytes<Void> nativeBytes(@NonNegative final long initialCapacity)
            throws IllegalArgumentException {
        @NotNull final BytesStore<?, Void> store = nativeStoreWithFixedCapacity(initialCapacity);
        try {
            return NativeBytes.wrapWithNativeBytes(store, Bytes.MAX_CAPACITY);
        } finally {
            store.release(INIT);
        }
    }

    /**
     * Adjusts the provided size to align with the operating system's page size.
     *
     * @param size The original size.
     * @return The size aligned with the page size.
     */
    private static long alignToPageSize(final long size) {
        final long mask = OS.pageSize() - 1L;
        return (size + mask) & ~mask;
    }

    /**
     * Wraps the provided BytesStore with a new instance of NativeBytes with the specified capacity.
     *
     * @param bs       The BytesStore to wrap.
     * @param capacity The capacity of the new NativeBytes instance.
     * @return A new instance of NativeBytes.
     * @throws IllegalArgumentException If the provided capacity is not valid.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public static <T> NativeBytes<T> wrapWithNativeBytes(@NotNull final BytesStore<?, T> bs, @NonNegative long capacity)
            throws ClosedIllegalStateException, IllegalArgumentException {
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
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
            if (offset < 0)
                throw new IllegalArgumentException();
            throw new BufferOverflowException();
        }
    }

    @NotNull
    private DecoratedBufferOverflowException newDBOE(long writeEnd) {
        return new DecoratedBufferOverflowException("Write cannot grow Bytes to " + writeEnd + ", capacity: " + capacity);
    }

    @Override
    void prewriteCheckOffset(@NonNegative long offset, long subtracting)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
            throws IllegalArgumentException, ClosedIllegalStateException, ThreadingIllegalStateException {

        if (desiredCapacity < 0) throw new IllegalArgumentException();
        assert DISABLE_SINGLE_THREADED_CHECK || threadSafetyCheck(true);
        writeCheckOffset(desiredCapacity, 0);
    }

    private void checkResize(@NonNegative final long endOfBuffer)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
                              final boolean isByteBufferBacked) throws ClosedIllegalStateException, ThreadingIllegalStateException {
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
        store = allocateNewByteBufferBackedStore(Maths.toInt32(size));
        return store;
    }

    @Override
    protected void bytesStore(@NotNull BytesStore<Bytes<U>, U> bytesStore) {
        if (capacity < bytesStore.capacity())
            capacity = bytesStore.capacity();
        super.bytesStore(bytesStore);
    }

    @Override
    public void bytesStore(@NotNull BytesStore<Bytes<U>, U> byteStore, @NonNegative long offset, @NonNegative long length)
            throws IllegalArgumentException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
    protected long writeOffsetPositionMoved(final @NonNegative long adding, final @NonNegative long advance)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        final long offset = writeOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeLong(final long i64)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        final long offset = writeOffsetPositionMoved(8L);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @Override
    public long readRemaining() {
        return writePosition() - readPosition;
    }
}
