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

final class BytesAccessors {

    static <B extends BytesStore<B, U>, U> Accessor.Full<B, ?> unchecked(B bytesStore) {
        if (bytesStore instanceof NativeBytesStore)
            return (Accessor.Full<B, ?>) Native.INSTANCE;
        if (bytesStore instanceof HeapBytesStore)
            return (Accessor.Full<B, ?>) Heap.INSTANCE;
        if (bytesStore instanceof AbstractBytes)
            return (Accessor.Full<B, ?>) StoreBasedBytes.INSTANCE;
        return (Accessor.Full<B, ?>) Generic.INSTANCE;
    }

    static class Native<S extends BytesStore> implements Accessor.Full<S, Void> {
        static final Native INSTANCE = new Native();

        @Override
        public Access<Void> access(S source) {
            return NativeAccess.instance();
        }

        @Override
        public Void handle(S source) {
            return null;
        }

        @Override
        public long offset(S source, long index) {
            return ((NativeBytesStore) source).address() + index;
        }
    }

    static class Heap<S extends BytesStore> implements Accessor.Full<S, Object> {
        static final Heap INSTANCE = new Heap();

        @Override
        public Access<Object> access(S source) {
            return NativeAccess.instance();
        }

        @Override
        public Object handle(S source) {
            return ((HeapBytesStore) source).realUnderlyingObject;
        }

        @Override
        public long offset(S source, long index) {
            return ((HeapBytesStore) source).dataOffset + index;
        }
    }

    static class StoreBasedBytes<S extends BytesStore> implements Accessor.Full<S, Object> {
        static final StoreBasedBytes INSTANCE = new StoreBasedBytes();

        @Override
        public Access<Object> access(S source) {
            BytesStore bytesStore = ((AbstractBytes) source).bytesStore;
            return (Access<Object>) unchecked(bytesStore).access(bytesStore);
        }

        @Override
        public Object handle(S source) {
            BytesStore bytesStore = ((AbstractBytes) source).bytesStore;
            return unchecked(bytesStore).handle(bytesStore);
        }

        @Override
        public long offset(S source, long index) {
            BytesStore bytesStore = ((AbstractBytes) source).bytesStore;
            return unchecked(bytesStore).offset(bytesStore, index);
        }
    }

    static class Generic<S extends BytesStore> implements Accessor.Full<S, S> {
        static final Generic INSTANCE = new Generic();

        @Override
        public Access<S> access(S source) {
            return BytesAccesses.Full.INSTANCE;
        }

        @Override
        public S handle(S source) {
            return source;
        }

        @Override
        public long offset(S source, long index) {
            return index;
        }
    }

    private BytesAccessors() {}
}
