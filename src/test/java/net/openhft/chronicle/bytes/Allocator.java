/*
 * Copyright 2016-2020 Chronicle Software
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

import java.nio.ByteBuffer;

public enum Allocator {

    NATIVE {
        @NotNull
        @Override
        Bytes<ByteBuffer> elasticBytes(int capacity) {
            return Bytes.elasticByteBuffer(capacity);
        }

        @NotNull
        @Override
        ByteBuffer byteBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity);
        }
    },
    HEAP {
        @NotNull
        @Override
        Bytes<byte[]> elasticBytes(int capacity) {
            return Bytes.allocateElasticOnHeap(capacity);
        }

        @NotNull
        @Override
        ByteBuffer byteBuffer(int capacity) {
            return ByteBuffer.allocate(capacity);
        }
    },
    BYTE_BUFFER {
        @NotNull
        @Override
        Bytes<ByteBuffer> elasticBytes(int capacity) {
            return Bytes.elasticHeapByteBuffer(capacity);
        }

        @NotNull
        @Override
        ByteBuffer byteBuffer(int capacity) {
            return ByteBuffer.allocate(capacity);
        }
    },
    NATIVE_UNCHECKED {
        @NotNull
        @Override
        Bytes<ByteBuffer> elasticBytes(int capacity) {
            return Bytes.elasticByteBuffer(Math.max(128, capacity)).unchecked(true);
        }

        @NotNull
        @Override
        ByteBuffer byteBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity);
        }
    },
    HEAP_UNCHECKED {
        @NotNull
        @Override
        Bytes<ByteBuffer> elasticBytes(int capacity) {
            return Bytes.elasticHeapByteBuffer(Math.max(32, capacity)).unchecked(true);
        }

        @NotNull
        @Override
        ByteBuffer byteBuffer(int capacity) {
            return ByteBuffer.allocate(capacity);
        }
    };

    @NotNull
    abstract Bytes<?> elasticBytes(int capacity);

    @NotNull
    abstract ByteBuffer byteBuffer(int capacity);

    Bytes<ByteBuffer> fixedBytes(int capacity) {
        return Bytes.wrapForWrite(byteBuffer(capacity));
    }
}
