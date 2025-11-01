/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;

/**
 * An abstract base for {@link net.openhft.chronicle.bytes.BytesStore}
 * implementations. It extends
 * {@link net.openhft.chronicle.core.io.AbstractReferenceCounted} to provide
 * reference counting and supplies default behaviour for many
 * {@code BytesStore} operations such as
 * {@link #hashCode()}, {@link #equals(Object)} and simple cursor methods.
 * Concrete subclasses are expected to implement the raw data access and
 * capacity related methods.
 *
 * @param <B> the BytesStore type
 * @param <U> the underlying backing type
 */

public abstract class AbstractBytesStore<B extends BytesStore<B, U>, U>
        extends AbstractReferenceCounted
        implements BytesStore<B, U> {

    /**
     * Creates an instance that is not monitored via JMX.
     */
    protected AbstractBytesStore() {
    }

    /**
     * @param monitored if true the reference count is tracked for debugging or
     *                  monitoring purposes
     */
    protected AbstractBytesStore(boolean monitored) {
        super(monitored);
    }

    /**
     * Returns {@code -1} if {@code offset} is outside the readable range,
     * otherwise the unsigned byte value at that position.
     */
    @Override
    public int peekUnsignedByte(@NonNegative long offset)
            throws IllegalStateException {
        return offset < start() || readLimit() <= offset ? -1 : readUnsignedByte(offset);
    }

    @Override
    public int hashCode() {
        // default implementation based on the content
        return BytesStoreHash.hash32(this);
    }

    @Override
    public boolean equals(Object obj) {
        // equality is based on the readable content
        return obj instanceof BytesStore && BytesInternal.contentEqual(this, (BytesStore<?, ?>) obj);
    }

    @Override
    public @NonNegative long readPosition() {
        // subclasses may return a different value
        return 0L;
    }

    @Override
    public long readRemaining() {
        return readLimit() - readPosition();
    }

    @Override
    public long writeRemaining() {
        return writeLimit() - writePosition();
    }

    @Override
    public @NonNegative long start() {
        // default start offset
        return 0L;
    }

    /**
     * Indicates whether {@link #performRelease()} can be run on a background
     * thread. The default implementation returns {@code true} for stores backed
     * by direct memory.
     */
    @Override
    protected boolean canReleaseInBackground() {
        // background release is allowed for native memory stores only
        return isDirectMemory();
    }
}
