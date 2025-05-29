/*
 * Copyright 2016-2025 chronicle.software
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
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.annotation.Positive;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.posix.PosixAPI;
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
 * {@link NativeBytesStore} backed by a region of a memory-mapped file.
 * <p>
 * Direct access methods assume the caller performs bounds checking; misuse can
 * corrupt memory.
 */
public class MappedBytesStore extends NativeBytesStore<Void> {
    /** run before each write, throws if read only */
    protected final Runnable writeCheck;
    /** owning mapped file */
    private final MappedFile mappedFile;
    /** logical start offset within the file */
    private final long start;
    /** region end up to which accesses are always safe */
    private final long safeLimit;
    /** filesystem page size */
    private final int pageSize;
    /** mode used when syncing to disk */
    private SyncMode syncMode = MappedFile.DEFAULT_SYNC_MODE;
    /** length already synced */
    private long syncLength = 0;

    /**
     * Constructs a {@code MappedBytesStore} for a mapped region.
     *
     * @param owner        reference owner
     * @param mappedFile   parent mapped file
     * @param start        logical start offset
     * @param address      native address of the mapping
     * @param capacity     mapped capacity
     * @param safeCapacity portion guaranteed not to cross a chunk
     * @param pageSize     page size for alignment
     */
    @SuppressWarnings("this-escape")
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
     * Factory method mirroring the protected constructor.
     */
    public static MappedBytesStore create(ReferenceOwner owner, MappedFile mappedFile, @NonNegative long start, long address, @NonNegative long capacity, @NonNegative long safeCapacity, @Positive int pageSize)
            throws ClosedIllegalStateException {
        return new MappedBytesStore(owner, mappedFile, start, address, capacity, safeCapacity, pageSize);
    }

    static void throwReadOnly() {
        throw new IllegalStateException("Read Only");
    }

    @SuppressWarnings("EmptyMethod")
    static void readWriteOk() {
        // nothing to do
    }

    /**
     * @return capacity of the underlying file, which may differ from
     * {@link #capacity()} if alignment is applied
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
    /**
     * Returns {@code true} if the given offset lies within this mapping's safe range.
     */
    public boolean inside(@NonNegative long offset) {
        return start <= offset && offset < safeLimit;
    }

    @Override
    /**
     * Same as {@link #inside(long)} but also checks the end of the supplied range.
     */
    public boolean inside(@NonNegative long offset, @NonNegative long bufferSize) {
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
    /**
     * Converts a logical offset in the file into an offset relative to this mapped region.
     */
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
     * Syncs the mapped region to disk prior to release if required.
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
    /**
     * Helper performing the actual msync call.
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
