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

import org.jetbrains.annotations.NotNull;

/**
 * A BytesStore which can point to arbitrary memory.
 */
public class PointerBytesStore extends NativeBytesStore<Void> {
    private boolean isPresent = false;

    public PointerBytesStore() {
        super(NoBytesStore.NO_PAGE, 0, null, false, false);
    }

    public void set(long address, long capacity) {
        isPresent = true;
        setAddress(address);
        this.limit = maximumLimit = capacity;
    }

    @NotNull
    @Override
    public VanillaBytes<Void> bytesForWrite()
            throws IllegalStateException {
        try {
            return new NativeBytes<>(this, Bytes.MAX_CAPACITY);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long safeLimit() {
        return limit;
    }

    @Override
    public long start() {
        return 0;
    }
}
