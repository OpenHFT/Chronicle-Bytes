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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

public class EmbeddedBytes<U> extends VanillaBytes<U> {
    private EmbeddedBytes(@NotNull BytesStore<?, ?> bytesStore, long writePosition, long writeLimit) throws IllegalStateException, IllegalArgumentException {
        super(bytesStore, writePosition, writeLimit);
    }

    public static <U> EmbeddedBytes<U> wrap(BytesStore<?, U> bytesStore) {
        return wrap((HeapBytesStore<U>) bytesStore);
    }

    public static <U> EmbeddedBytes<U> wrap(HeapBytesStore<U> bytesStore) {
        long wp = bytesStore.start();
        int length = bytesStore.readUnsignedByte(wp - 1);
        return new EmbeddedBytes<>(bytesStore, wp, wp + length);
    }

    @Override
    protected void uncheckedWritePosition(@NonNegative long writePosition) {
        super.uncheckedWritePosition(writePosition);
        bytesStore.writeUnsignedByte(bytesStore.start() - 1, (int) writePosition);
    }

    @Override
    public @NonNegative long writePosition() {
        return bytesStore.readUnsignedByte(bytesStore.start() - 1);
    }
}
