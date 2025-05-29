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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
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
        } catch (ClosedIllegalStateException ignored) {
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
