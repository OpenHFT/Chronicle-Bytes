/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
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

import java.nio.ByteOrder;

final class BytesAccesses {

    static class Read<R extends RandomDataInput<R>> implements RandomDataInputAccess<R> {
        static final Read INSTANCE = new Read();
    }

    static class Write<R extends RandomDataOutput<R>> implements RandomDataOutputAccess<R> {
        static final Write INSTANCE = new Write();
    }

    static class Full<B extends BytesStore<B, U>, U> implements RandomDataInputAccess<B>,
            RandomDataOutputAccess<B>, Access<B> {
        static final Full INSTANCE = new Full();

        @Override
        public boolean compareAndSwapInt(B handle, long offset, int expected, int value) {
            return handle.compareAndSwapInt(offset, expected, value);
        }

        @Override
        public boolean compareAndSwapLong(B handle, long offset, long expected, long value) {
            return handle.compareAndSwapLong(offset, expected, value);
        }

        @Override
        public ByteOrder byteOrder(B handle) {
            return handle.byteOrder();
        }
    }

    private BytesAccesses() {}
}
