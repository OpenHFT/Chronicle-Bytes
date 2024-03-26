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

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.bytes.internal.ReferenceCountedUtil;
import net.openhft.chronicle.bytes.internal.Unmapper;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.annotation.Positive;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.posix.PosixAPI;
import net.openhft.posix.internal.jnr.WinJNRPosixAPI;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;

import static net.openhft.chronicle.assertions.AssertUtil.SKIP_ASSERTIONS;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * A BytesStore implementation that wraps memory-mapped data.
 *
 * <p>
 * This class is intended for working with large files that can be read from and written to as if they were a part of the program's memory.
 * <p>
 * <b>WARNING:</b> This class assumes that the caller will correctly handle bounds checking. Incorrect handling can lead to {@link IndexOutOfBoundsException}.
 * Misuse of this class can cause hard-to-diagnose memory access errors and data corruption.
 */
public class MappedBytesStore extends NativeBytesStore<Void> {
    @Deprecated(/* to be removed in x.26 */)
    public static final @NotNull MappedBytesStoreFactory MAPPED_BYTES_STORE_FACTORY = MappedBytesStore::new;
    protected final Runnable writeCheck;
    private final MappedFile mappedFile;
    private final long start;
    private final long safeLimit;
    private final int pageSize;
    private SyncMode syncMode = MappedFile.DEFAULT_SYNC_MODE;
    private long syncLength = 0;

    /**
     * Creates a new MappedBytesStore with the given parameters.
     *
     * @param owner        The owner of this MappedBytesStore.
     * @param mappedFile   The MappedFile to be wrapped by this BytesStore.
     * @param start        The start position within the MappedFile.
     * @param address      The memory address of the mapped data.
     * @param capacity     The capacity of the mapped data.
     * @param safeCapacity The safe capacity of the mapped data. Accessing data beyond the safe capacity might lead to a crash.
     * @throws ClosedIllegalStateException If the resource has been released or closed.
     */
    @Deprecated(/* to be removed in x.26 */)
    protected MappedBytesStore(ReferenceOwner owner, MappedFile mappedFile, @NonNegative long start, long address, @NonNegative long capacity, @NonNegative long safeCapacity)
            throws ClosedIllegalStateException {
        this(owner, mappedFile, start, address, capacity, safeCapacity, PageUtil.getPageSize(mappedFile.file().getAbsolutePath()));
    }

    /**
     * Creates a new MappedBytesStore with the given parameters.
     *
     * @param owner        The owner of this MappedBytesStore.
     * @param mappedFile   The MappedFile to be wrapped by this BytesStore.
     * @param start        The start position within the MappedFile.
     * @param address      The memory address of the mapped data.
     * @param capacity     The capacity of the mapped data.
     * @param safeCapacity The safe capacity of the mapped data. Accessing data beyond the safe capacity might lead to a crash.
     * @param pageSize     Page size to use to check alignment
     * @throws ClosedIllegalStateException If the resource has been released or closed.
     */
    protected MappedBytesStore(ReferenceOwner owner, MappedFile mappedFile, @NonNegative long start, long address, @NonNegative long capacity, @NonNegative long safeCapacity, @Positive int pageSize)
            throws ClosedIllegalStateException {
        super(address, start + capacity, new Unmapper(address, capacity, pageSize), false);
        this.mappedFile = mappedFile;
        this.start = start;
        this.safeLimit = start + safeCapacity;
        this.writeCheck = mappedFile.readOnly()
                ? MappedBytesStore::throwReadOnly
                : MappedBytesStore::readWriteOk;

        reserveTransfer(INIT, owner);
        this.pageSize = pageSize;
    }

    /**
     * Creates a new MappedBytesStore with the given parameters.
     *
     * @param owner        The owner of this MappedBytesStore.
     * @param mappedFile   The MappedFile to be wrapped by this BytesStore.
     * @param start        The start position within the MappedFile.
     * @param address      The memory address of the mapped data.
     * @param capacity     The capacity of the mapped data.
     * @param safeCapacity The safe capacity of the mapped data. Accessing data beyond the safe capacity might lead to a crash.
     * @return the MappedBytesStore
     * @throws ClosedIllegalStateException If the resource has been released or closed.
     */
    @Deprecated(/* to be removed in x.26 */)
    public static MappedBytesStore create(ReferenceOwner owner, MappedFile mappedFile, @NonNegative long start, long address, @NonNegative long capacity, @NonNegative long safeCapacity)
            throws ClosedIllegalStateException {
        return new MappedBytesStore(owner, mappedFile, start, address, capacity, safeCapacity, PageUtil.getPageSize(mappedFile.file().getAbsolutePath()));
    }

    /**
     * Creates a new MappedBytesStore with the given parameters.
     *
     * @param owner        The owner of this MappedBytesStore.
     * @param mappedFile   The MappedFile to be wrapped by this BytesStore.
     * @param start        The start position within the MappedFile.
     * @param address      The memory address of the mapped data.
     * @param capacity     The capacity of the mapped data.
     * @param safeCapacity The safe capacity of the mapped data. Accessing data beyond the safe capacity might lead to a crash.
     * @param pageSize     Page size to use to check alignment
     * @return the MappedBytesStore
     * @throws ClosedIllegalStateException If the resource has been released or closed.
     */
    public static MappedBytesStore create(ReferenceOwner owner, MappedFile mappedFile, @NonNegative long start, long address, @NonNegative long capacity, @NonNegative long safeCapacity, @Positive int pageSize)
            throws ClosedIllegalStateException {
        return new MappedBytesStore(owner, mappedFile, start, address, capacity, safeCapacity, pageSize);
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
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
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
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        return new NativeBytes<>(this);
    }

    @Override
    public boolean inside(@NonNegative long offset) {
        return start <= offset && offset < safeLimit;
    }

    @Override
    public boolean inside(@NonNegative long offset, @NonNegative long bufferSize) {
        // yes this is different to the method above (we take into account the overlap because we know the length)
        return start <= offset && offset + bufferSize <= limit;
    }

    @Override
    public long safeLimit() {
        return safeLimit;
    }

    @Override
    public byte readByte(@NonNegative long offset) {
        return memory.readByte(address - start + offset);
    }

    @NotNull
    @Override
    public MappedBytesStore writeOrderedInt(@NonNegative long offset, int i)
            throws IllegalStateException {
        writeCheck.run();

        memory.writeOrderedInt(address - start + offset, i);
        return this;
    }

    @Override
    public long translate(@NonNegative long offset) {
        assert SKIP_ASSERTIONS || offset >= start;
        assert SKIP_ASSERTIONS || offset < limit;

        return offset - start;
    }

    @Override
    public @NonNegative long start() {
        return start;
    }

    @Override
    public @NonNegative long readPosition() {
        return start();
    }

    /**
     * Acquires a lock on a region of the underlying file.
     * This method blocks until the lock has been acquired.
     *
     * @param position The starting byte position of the region to lock.
     * @param size     The number of bytes to lock, starting from the position.
     * @param shared   If {@code true}, the lock will be shared; otherwise, it will be exclusive.
     * @return A FileLock representing the lock on the specified region.
     * @throws IOException If an I/O error occurs while locking.
     * @see MappedFile#lock(long, long, boolean) for details on how the lock is acquired.
     */
    public FileLock lock(@NonNegative long position, @NonNegative long size, boolean shared) throws IOException {

        return mappedFile.lock(position, size, shared);
    }

    /**
     * Attempts to acquire a lock on a region of the underlying file.
     * This method does not block and returns immediately, either with a lock or with null if the lock could not be acquired.
     *
     * @param position The starting byte position of the region to lock.
     * @param size     The number of bytes to lock, starting from the position.
     * @param shared   If {@code true}, the lock will be shared; otherwise, it will be exclusive.
     * @return A FileLock representing the lock on the specified region or {@code null} if the lock could not be acquired.
     * @throws IOException If an I/O error occurs while trying to lock.
     * @see MappedFile#tryLock(long, long, boolean) for details on how the lock is attempted.
     */
    public FileLock tryLock(@NonNegative long position, @NonNegative long size, boolean shared) throws IOException {
        return mappedFile.tryLock(position, size, shared);
    }

    @NotNull
    @Override
    public MappedBytesStore zeroOut(@NonNegative long start, @NonNegative long end) {
        writeCheck.run();
        super.zeroOut(start, end);
        return this;
    }

    @Override
    public boolean compareAndSwapInt(@NonNegative long offset, int expected, int value)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        return super.compareAndSwapInt(offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(@NonNegative long offset, long expected, long value)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        return super.compareAndSwapLong(offset, expected, value);
    }

    @NotNull
    @Override
    public MappedBytesStore writeByte(@NonNegative long offset, byte i8)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeShort(@NonNegative long offset, short i16)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeInt(@NonNegative long offset, int i32)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeInt(offset, i32);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeLong(@NonNegative long offset, long i64)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeOrderedLong(@NonNegative long offset, long i)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeOrderedLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeFloat(@NonNegative long offset, float f)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeFloat(offset, f);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeDouble(@NonNegative long offset, double d)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileByte(@NonNegative long offset, byte i8)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeVolatileByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileShort(@NonNegative long offset, short i16)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeVolatileShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileInt(@NonNegative long offset, int i32)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeVolatileInt(offset, i32);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileLong(@NonNegative long offset, long i64)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.writeVolatileLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public MappedBytesStore write(@NonNegative final long offsetInRDO,
                                  final byte[] byteArray,
                                  @NonNegative final int offset,
                                  @NonNegative final int length)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        // Parameter invariants are checked in the super method
        writeCheck.run();
        super.write(offsetInRDO, byteArray, offset, length);
        return this;
    }

    @Override
    public void write(@NonNegative long offsetInRDO, @NotNull ByteBuffer bytes, @NonNegative int offset, @NonNegative int length)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNull(bytes);
        writeCheck.run();
        super.write(offsetInRDO, bytes, offset, length);
    }

    @NotNull
    @Override
    public MappedBytesStore write(@NonNegative long writeOffset, @NotNull RandomDataInput bytes, @NonNegative long readOffset, @NonNegative long length)
            throws BufferOverflowException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNegative(writeOffset);
        ReferenceCountedUtil.throwExceptionIfReleased(bytes);
        requireNonNegative(readOffset);
        requireNonNegative(length);
        throwExceptionIfReleased();
        writeCheck.run();
        super.write(writeOffset, bytes, readOffset, length);
        return this;
    }

    @Override
    public void write0(@NonNegative long offsetInRDO, @NotNull RandomDataInput bytes, @NonNegative long offset, @NonNegative long length)
            throws ClosedIllegalStateException {
        requireNonNull(bytes);
        writeCheck.run();
        super.write0(offsetInRDO, bytes, offset, length);
    }

    @Override
    public void nativeWrite(long address, @NonNegative long position, @NonNegative long size)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        super.nativeWrite(address, position, size);
    }

    @Override
    public long appendUtf8(@NonNegative long pos, char[] chars, @NonNegative int offset, @NonNegative int length)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        writeCheck.run();
        return super.appendUtf8(pos, chars, offset, length);
    }

    /**
     * Sync the ByteStore if required.
     */
    @Override
    protected void performRelease() {
        if (address != 0 && syncMode != SyncMode.NONE) {
            performMsync(0, safeLimit - start, syncMode());
        }
        // must sync before releasing
        super.performRelease();
    }

    /**
     * Sync the ByteStore if required.
     *
     * @param offset   the offset within the ByteStore from the start to sync, offset must be a multiple of 4K
     * @param length   the length to sync, length must be a multiple of 4K
     * @param syncMode the mode to sync
     */
    private void performMsync(@NonNegative long offset, long length, SyncMode syncMode) {
        if (syncMode == SyncMode.NONE)
            return;
        long start0 = System.currentTimeMillis();
        boolean full = offset == 0;
        int ret = PosixAPI.posix().msync(address + offset, length, syncMode.mSyncFlag());
        if (ret != 0)
            Jvm.error().on(MappedBytesStore.class, "msync failed, " + PosixAPI.posix().lastErrorStr() + ", ret=" + ret + " " + mappedFile.file() + " " + Long.toHexString(offset) + " " + Long.toHexString(length));
        long time0 = System.currentTimeMillis() - start0;
        if (time0 >= 200)
            Jvm.perf().on(getClass(), "Took " + time0 + " ms to " + syncMode + " " + mappedFile.file() + (full ? " (full)" : ""));
    }

    /**
     * @return the sync mode for this ByteStore
     */
    public SyncMode syncMode() {
        return syncMode == null ? SyncMode.NONE : syncMode;
    }

    /**
     * Set the sync mode for this ByteStore
     *
     * @param syncMode to use
     */
    public void syncMode(SyncMode syncMode) {
        this.syncMode = syncMode;
    }

    /**
     * Synchronise from the last complete page up to this position.
     *
     * @param position to sync with the syncMode()
     */
    public void syncUpTo(long position) {
        syncUpTo(position, this.syncMode);
    }

    /**
     * Synchronise from the last complete page up to this position.
     *
     * @param position to sync with the syncMode()
     * @param syncMode to use
     */
    public void syncUpTo(long position, SyncMode syncMode) {
        if (syncMode == SyncMode.NONE || address == 0 || refCount() <= 0)
            return;
        long positionFromStart = Math.min(limit, position) - start;
        if (positionFromStart <= syncLength)
            return;
        int mask = -pageSize;
        long pageEnd = (positionFromStart + pageSize - 1) & mask;
        long syncStart = syncLength & mask;
        final long length2 = pageEnd - syncStart;
        performMsync(syncStart, length2, syncMode);
        syncLength = positionFromStart;
    }
}
