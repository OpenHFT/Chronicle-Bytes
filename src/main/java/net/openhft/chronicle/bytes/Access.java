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

import java.nio.ByteBuffer;

public interface Access<T> extends ReadAccess<T>, WriteAccess<T> {

    static <T> Access<T> nativeAccess() {
        return NativeAccess.instance();
    }

    static Access<ByteBuffer> checkedByteBufferAccess() {
        return ByteBufferAccess.INSTANCE;
    }

    static <B extends BytesStore<B, U>, U> Access<B> checkedBytesStoreAccess() {
        return BytesAccesses.Full.INSTANCE;
    }

    /**
     * Default implementation: throws {@code UnsupportedOperationException}.
     */
    default boolean compareAndSwapInt(T handle, long offset, int expected, int value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Default implementation: throws {@code UnsupportedOperationException}.
     */
    default boolean compareAndSwapLong(T handle, long offset, long expected, long value) {
        throw new UnsupportedOperationException();
    }
}