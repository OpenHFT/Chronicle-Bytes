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

import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

interface ByteBufferAccessor<T> extends Accessor.Full<ByteBuffer, T> {

    static ByteBufferAccessor<?> unchecked(ByteBuffer buffer) {
        return buffer.isDirect() ? Direct.INSTANCE : Heap.INSTANCE;
    }

    static ByteBufferAccessor<ByteBuffer> checked() {
        return Generic.INSTANCE;
    }

    enum Direct implements ByteBufferAccessor<Void> {
        INSTANCE;

        @Override
        public Access<Void> access(ByteBuffer buffer) {
            return NativeAccess.instance();
        }

        @Override
        public Void handle(ByteBuffer buffer) {
            return null;
        }

        @Override
        public long offset(ByteBuffer buffer, long bufferIndex) {
            return ((DirectBuffer) buffer).address() + bufferIndex;
        }
    }

    enum Heap implements ByteBufferAccessor<byte[]> {
        INSTANCE;

        @Override
        public Access<byte[]> access(ByteBuffer buffer) {
            return NativeAccess.instance();
        }

        @Override
        public byte[] handle(ByteBuffer buffer) {
            return buffer.array();
        }

        @Override
        public long offset(ByteBuffer buffer, long bufferIndex) {
            return ArrayAccessors.BYTE_BASE + buffer.arrayOffset() + bufferIndex;
        }
    }

    enum Generic implements ByteBufferAccessor<ByteBuffer> {
        INSTANCE;

        @Override
        public Access<ByteBuffer> access(ByteBuffer buffer) {
            return ByteBufferAccess.INSTANCE;
        }

        @Override
        public ByteBuffer handle(ByteBuffer buffer) {
            return buffer;
        }

        @Override
        public long offset(ByteBuffer buffer, long bufferIndex) {
            return bufferIndex;
        }
    }
}
