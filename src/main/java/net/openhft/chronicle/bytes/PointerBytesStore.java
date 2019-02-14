/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.core.annotation.NotNull;

/**
 * A BytesStore which can point to arbitrary memory.
 */
public class PointerBytesStore extends NativeBytesStore<Void> {
    private boolean isPresent = false;

    public PointerBytesStore() {
        super(NoBytesStore.NO_PAGE, 0, null, false);
    }

    public void set(long address, long capacity) {
        isPresent = true;
        setAddress(address);
        this.maximumLimit = capacity;
    }

    @NotNull
    @Override
    public VanillaBytes<Void> bytesForWrite() throws IllegalStateException {
        return new VanillaBytes<>(this);
    }

    @Override
    public long safeLimit() {
        return maximumLimit;
    }

    @Override
    public long start() {
        return 0;
    }

    @Override
    public void isPresent(boolean isPresent) {
        this.isPresent = isPresent;
    }

    @Override
    public boolean isPresent() {
        return isPresent;
    }
}
