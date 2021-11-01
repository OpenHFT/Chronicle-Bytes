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

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * BytesStore to wrap memory mapped data.
 * <p>
 * <b>WARNING: Handle with care as this assumes the caller has correct bounds checking</b>
 */
public class MappedBytesStore extends NativeBytesStore<Void> {
    public static final @NotNull MappedBytesStoreFactory MAPPED_BYTES_STORE_FACTORY = MappedBytesStore::new;
    private final MappedFile mappedFile;
    private final long start;
    private final long safeLimit;
    protected final Runnable writeCheck;

    protected MappedBytesStore(ReferenceOwner owner, MappedFile mappedFile, long start, long address, long capacity, long safeCapacity)
            throws IllegalStateException {
        super(address, start + capacity, new OS.Unmapper(address, capacity), false);
        this.mappedFile = mappedFile;
        this.start = start;
        this.safeLimit = start + safeCapacity;
        this.writeCheck = mappedFile.readOnly()
                ? MappedBytesStore::throwReadOnly
                : MappedBytesStore::readWriteOk;

        reserveTransfer(INIT, owner);
    }

    static void throwReadOnly() {
        throw new IllegalStateException("Read Only");
    }

    static void readWriteOk() {
        // nothing to do
    }

    /**
     * Fetch the capacity of the underlying file
     * This can differ from the exposed capacity() of this bytes store (which has been page aligned)
     *
     * @return - capacity of the underlying file
     */
    public long underlyingCapacity() {
        return mappedFile.capacity();
    }

    @Override
    public @NotNull Bytes<Void> bytesForRead()
            throws IllegalStateException {
        try {
            return new NativeBytes<Void>(this)
                    .readLimit(writeLimit())
                    .readPosition(start());
        } catch (BufferUnderflowException | IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    @Override
    public VanillaBytes<Void> bytesForWrite()
            throws IllegalStateException {
        writeCheck.run();
        try {
            return new NativeBytes<>(this);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean inside(long offset) {
        return start <= offset && offset < safeLimit;
    }

    @Override
    public boolean inside(long offset, long buffer) {
        // this is correct that it uses the maximumLimit, yes it is different than the method above.
        return start <= offset && offset + buffer <= limit;
    }

    @Override
    public long safeLimit() {
        return safeLimit;
    }

    @Override
    public byte readByte(long offset) {
        return memory.readByte(address - start + offset);
    }

    @NotNull
    @Override
    public MappedBytesStore writeOrderedInt(long offset, int i)
            throws IllegalStateException {
        writeCheck.run();

        memory.writeOrderedInt(address - start + offset, i);
        return this;
    }

    @Override
    public long translate(long offset) {
        assert offset >= start;
        assert offset < limit;

        return offset - start;
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long readPosition() {
        return start();
    }

    /**
     * Calls lock on the underlying file channel
     */
    public FileLock lock(long position, long size, boolean shared) throws IOException {

        return mappedFile.lock(position, size, shared);
    }

    /**
     * Calls tryLock on the underlying file channel
     */
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return mappedFile.tryLock(position, size, shared);
    }

    @NotNull
    @Override
    public MappedBytesStore zeroOut(long start, long end) {
        writeCheck.run();
        super.zeroOut(start, end);
        return this;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value)
            throws IllegalStateException {
        writeCheck.run();
        return super.compareAndSwapInt(offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value)
            throws IllegalStateException {
        writeCheck.run();
        return super.compareAndSwapLong(offset, expected, value);
    }

    @NotNull
    @Override
    public MappedBytesStore writeByte(long offset, byte i8)
            throws IllegalStateException {
        writeCheck.run();
        super.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeShort(long offset, short i16)
            throws IllegalStateException {
        writeCheck.run();
        super.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeInt(long offset, int i32)
            throws IllegalStateException {
        writeCheck.run();
        super.writeInt(offset, i32);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeLong(long offset, long i64)
            throws IllegalStateException {
        writeCheck.run();
        super.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeOrderedLong(long offset, long i)
            throws IllegalStateException {
        writeCheck.run();
        super.writeOrderedLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeFloat(long offset, float f)
            throws IllegalStateException {
        writeCheck.run();
        super.writeFloat(offset, f);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeDouble(long offset, double d)
            throws IllegalStateException {
        writeCheck.run();
        super.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileByte(long offset, byte i8)
            throws IllegalStateException {
        writeCheck.run();
        super.writeVolatileByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileShort(long offset, short i16)
            throws IllegalStateException {
        writeCheck.run();
        super.writeVolatileShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileInt(long offset, int i32)
            throws IllegalStateException {
        writeCheck.run();
        super.writeVolatileInt(offset, i32);
        return this;
    }


    @NotNull
    @Override
    public MappedBytesStore writeVolatileLong(long offset, long i64)
            throws IllegalStateException {
        writeCheck.run();
        super.writeVolatileLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore write(long offsetInRDO, @NotNull byte[] bytes, int offset, int length)
            throws IllegalStateException {
        requireNonNull(bytes);
        writeCheck.run();
        super.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @Override
    public void write(long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length)
            throws IllegalStateException {
        requireNonNull(bytes);
        writeCheck.run();
        super.write(offsetInRDO, bytes, offset, length);
    }

    @NotNull
    @Override
    public MappedBytesStore write(long writeOffset, @NotNull RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        requireNonNull(bytes);
        writeCheck.run();
        super.write(writeOffset, bytes, readOffset, length);
        return this;
    }

    @Override
    public void write0(long offsetInRDO, @NotNull RandomDataInput bytes, long offset, long length)
            throws IllegalStateException {
        requireNonNull(bytes);
        writeCheck.run();
        super.write0(offsetInRDO, bytes, offset, length);
    }

    @Override
    public void nativeWrite(long address, long position, long size)
            throws IllegalStateException {
        writeCheck.run();
        super.nativeWrite(address, position, size);
    }

    @Override
    public long appendUtf8(long pos, char[] chars, int offset, int length)
            throws IllegalStateException {
        writeCheck.run();
        return super.appendUtf8(pos, chars, offset, length);
    }
}
