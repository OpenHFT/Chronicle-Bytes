/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

public class PointerBytesStore extends NativeBytesStore {
    protected PointerBytesStore() {
        super(NoBytesStore.NO_PAGE, 0, null, false);
    }

    public void wrap(long address, long capacity) {
        this.address = address;
        this.maximumLimit = capacity;
    }

    @Override
    public Bytes<Void> bytes() {
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
}
