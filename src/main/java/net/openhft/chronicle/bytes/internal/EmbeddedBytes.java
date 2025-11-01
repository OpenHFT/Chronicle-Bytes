/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.core.Jvm.uncheckedCast;

/**
 * Non elastic {@link VanillaBytes} view over a field group inside an object.
 * The length is stored in a byte immediately before the group.
 */
public class EmbeddedBytes<U> extends VanillaBytes<U> {
    /**
     * Creates a view over {@code bytesStore} with the given bounds. The byte
     * at {@code start()-1} holds the length.
     */
    private EmbeddedBytes(@NotNull BytesStore<?, ?> bytesStore, long writePosition, long writeLimit) throws IllegalStateException, IllegalArgumentException {
        super(bytesStore, writePosition, writeLimit);
    }

    /**
     * Wraps {@code bytesStore}, which must be a HeapBytesStore for a field
     * group, returning an EmbeddedBytes view.
     */
    public static <U> EmbeddedBytes<U> wrap(BytesStore<?, U> bytesStore) {
        return wrap(uncheckedCast(bytesStore));
    }

    /**
     * Creates an EmbeddedBytes over the data in {@code bytesStore}. The length
     * is read from the byte before {@code bytesStore.start()}.
     */
    public static <U> EmbeddedBytes<U> wrap(HeapBytesStore<U> bytesStore) {
        long wp = bytesStore.start();
        int length = bytesStore.readUnsignedByte(wp - 1);
        return new EmbeddedBytes<>(bytesStore, wp, wp + length);
    }

    @Override
    /**
     * Sets the write position and updates the length byte stored before this
     * field group.
     */
    protected void uncheckedWritePosition(@NonNegative long writePosition) {
        super.uncheckedWritePosition(writePosition);
        bytesStore.writeUnsignedByte(lengthOffset(), (int) writePosition);
    }

    @Override
    /**
     * Reads the current length prefix and returns it as the write position.
     */
    public @NonNegative long writePosition() {
        try {
            return bytesStore.readUnsignedByte(lengthOffset());
        } catch (ClosedIllegalStateException e) {
            Jvm.debug().on(EmbeddedBytes.class, "bytesStore is closed", e);
            return 0;
        }
    }

    /**
     * @return the absolute offset of the length byte
     */
    private long lengthOffset() {
        return bytesStore.start() - 1;
    }
}
