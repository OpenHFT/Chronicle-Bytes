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

package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Bytes to wrap memory mapped data.
 * <p>
 * NOTE These Bytes are single Threaded as are all Bytes.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SingleMappedBytes extends CommonMappedBytes {

    // assume the mapped file is reserved already.
    public SingleMappedBytes(@NotNull final MappedFile mappedFile)
            throws IllegalStateException {
        this(mappedFile, "");
    }

    protected SingleMappedBytes(@NotNull final MappedFile mappedFile, final String name)
            throws IllegalStateException {
        super(mappedFile, name);

        try {
            bytesStore((BytesStore<Bytes<Void>, Void>) (BytesStore<?, Void>) mappedFile.acquireByteStore(this, 0));
            assert this.bytesStore.reservedBy(this);

        } catch (@NotNull IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @Override
    public @NotNull SingleMappedBytes write(@NonNegative final long offsetInRDO,
                                            final byte[] byteArray,
                                            @NonNegative int offset,
                                            @NonNegative final int length) throws IllegalStateException, BufferOverflowException {
        requireNonNegative(offsetInRDO);
        requireNonNull(byteArray);
        requireNonNegative(offset);
        requireNonNegative(length);
        throwExceptionIfClosed();

        long wp = offsetInRDO;
        if ((length + offset) > byteArray.length)
            throw new ArrayIndexOutOfBoundsException("bytes.length=" + byteArray.length + ", " + "length=" + length + ", offset=" + offset);

        if (offsetInRDO + length > writeLimit)
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. offset: %d + length: %d > writeLimit: %d", offsetInRDO, length, writeLimit));

        int remaining = length;

        while (remaining > 0) {

            long safeCopySize = copySize(wp);

            if (safeCopySize + mappedFile.overlapSize() >= remaining) {
                bytesStore.write(wp, byteArray, offset, remaining);
                return this;
            }

            bytesStore.write(wp, byteArray, offset, (int) safeCopySize);

            offset += safeCopySize;
            wp += safeCopySize;
            remaining -= safeCopySize;

        }
        return this;

    }

    @Override
    public @NotNull SingleMappedBytes write(final long writeOffset,
                                            @NotNull final RandomDataInput bytes,
                                            long readOffset,
                                            final long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        requireNonNegative(writeOffset);
        ReferenceCountedUtil.throwExceptionIfReleased(bytes);
        requireNonNegative(readOffset);
        requireNonNegative(length);
        throwExceptionIfReleased();
        long wp = writeOffset;

        if (writeOffset + length > writeLimit)
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. End of write: %d + %d > writeLimit: %d", writeOffset, length, writeLimit));

        long remaining = length;

        while (remaining > 0) {

            long safeCopySize = copySize(wp);

            if (safeCopySize + mappedFile.overlapSize() >= remaining) {
                bytesStore.write(wp, bytes, readOffset, remaining);
                return this;
            }

            bytesStore.write(wp, bytes, readOffset, safeCopySize);

            readOffset += safeCopySize;
            wp += safeCopySize;
            remaining -= safeCopySize;

        }
        return this;
    }

    private long copySize(final long writePosition) {
        long size = mappedFile.chunkSize();
        return size - writePosition % size;
    }

    @NotNull
    @Override
    public Bytes<Void> readPositionRemaining(final long position, final long remaining)
            throws BufferUnderflowException, IllegalStateException {
//        throwExceptionIfClosed();

        final long limit = position + remaining;

        try {
            if (writeLimit < limit)
                writeLimit(limit);

            if (Jvm.isAssertEnabled())
                readLimit(limit);
            else
                uncheckedWritePosition(limit);

            return readPosition(position);
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public @NotNull Bytes<Void> writeSkip(long bytesToSkip)
            throws BufferOverflowException, IllegalStateException {
        // only check up to 128 bytes are real.
        writeCheckOffset(writePosition(), Math.min(128, bytesToSkip));
        // the rest can be lazily allocated.
        uncheckedWritePosition(writePosition() + bytesToSkip);
        return this;
    }

    @NotNull
    private BufferOverflowException newBufferOverflowException(final long offset) {
        BufferOverflowException exception = new BufferOverflowException();
        exception.initCause(new IllegalArgumentException("Offset out of bound " + offset));
        return exception;
    }

    @NotNull
    @Override
    public Bytes<Void> clear()
            throws IllegalStateException {
        long start = 0L;
        readPosition = start;
        uncheckedWritePosition(start);
        writeLimit = mappedFile.capacity();
        return this;
    }

    @SuppressWarnings("restriction")
    @Override
    public int peekVolatileInt()
            throws IllegalStateException {

        @Nullable MappedBytesStore bytesStore = (MappedBytesStore) (BytesStore) this.bytesStore;
        long address = bytesStore.address + bytesStore.translate(readPosition);
        @Nullable Memory memory = bytesStore.memory;

        // are we inside a cache line?
        if ((address & 63) <= 60) {
            ObjectUtils.requireNonNull(memory);
            UnsafeMemory.unsafeLoadFence();
            return UnsafeMemory.unsafeGetInt(address);
        } else {
            return memory.readVolatileInt(address);
        }
    }

    // used by the Pretoucher, don't change this without considering the impact.
    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (offset < 0 || offset > capacity())
            throw newBufferOverflowException(offset);

        return bytesStore.compareAndSwapLong(offset, expected, value);
    }

    @Override
    public Bytes<Void> write(@NotNull BytesStore bytes) throws BufferOverflowException, IllegalStateException {
        assert bytes != this : "you should not write to yourself !";

        long length = bytes.readRemaining();
        bytesStore.write(writePosition, bytes);
        writeSkip(length);
        return this;
    }
}
