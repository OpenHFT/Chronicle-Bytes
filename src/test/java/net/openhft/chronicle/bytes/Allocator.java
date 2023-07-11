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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * This enum represents different types of Allocators. Each Allocator provides a way to create elastic bytes and byte buffers.
 */
public enum Allocator {
    /**
     * NATIVE allocator type uses native (off-heap) memory for both ByteBuffer and elastic Bytes.
     */
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

    /**
     * NATIVE_ADDRESS allocator type uses native (off-heap) memory for ByteBuffer, elastic Bytes, and fixed Bytes.
     * The created fixed Bytes wraps the NativeBytesStore by address without providing the underlying ByteBuffer.
     */
    NATIVE_ADDRESS {
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

        /**
         * Creates fixed Bytes using off-heap memory with the specified capacity.
         * Unlike the base method, this wraps NativeBytesStore by address without providing the underlying ByteBuffer.
         *
         * @param capacity the capacity of the Bytes
         * @return the created Bytes
         */
        @Override
        Bytes<?> fixedBytes(int capacity) {
            final ByteBuffer byteBuffer = byteBuffer(capacity);

            BytesStore<?, ByteBuffer> bs = new NativeBytesStore<>(byteBuffer, false, byteBuffer.capacity());
            try {
                Bytes<ByteBuffer> bbb = bs.bytesForWrite();
                bbb.writePosition(byteBuffer.position());
                bbb.writeLimit(byteBuffer.limit());
                return bbb;
            } finally {
                bs.release(ReferenceOwner.INIT);
            }
        }
    },
    /**
     * HEAP allocator type uses on-heap memory for both ByteBuffer and elastic Bytes.
     */
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
    /**
     * BYTE_BUFFER allocator type uses on-heap memory for ByteBuffer and elastic Bytes.
     */
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
    /**
     * NATIVE_UNCHECKED allocator type uses off-heap memory for both ByteBuffer and elastic Bytes, and it disables bound checks.
     */
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
    /**
     * HEAP_UNCHECKED allocator type uses on-heap memory for both ByteBuffer and elastic Bytes, and it disables bound checks.
     */
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
    },
    /**
     * HEAP_EMBEDDED allocator type uses on-heap memory and embeds Bytes into an object, doesn't support ByteBuffer creation.
     */
    HEAP_EMBEDDED {
        @Override
        @NotNull Bytes<?> elasticBytes(int capacity) {
            return fixedBytes(Math.max(capacity, 255));
        }

        @Override
        @NotNull ByteBuffer byteBuffer(int capacity) {
            throw new UnsupportedOperationException();
        }

        @Override
        Bytes<?> fixedBytes(int capacity) {
            if (capacity >= 256)
                throw new IllegalArgumentException();
            Padding padding = new Padding();
            return Bytes.forFieldGroup(padding, "p").writeLimit(capacity);
        }
    },
    /**
     * HEX_DUMP allocator type uses HexDumpBytes for both elastic and fixed Bytes, doesn't support ByteBuffer creation.
     */
    HEX_DUMP {
        @Override
        @NotNull Bytes<?> elasticBytes(int capacity) {
            return new HexDumpBytes();
        }

        @Override
        @NotNull ByteBuffer byteBuffer(int capacity) {
            throw new UnsupportedOperationException();
        }

        @Override
        Bytes<?> fixedBytes(int capacity) {
            return new HexDumpBytes();
        }
    };

    /**
     * Creates elastic Bytes with the specified initial capacity.
     * The resulting Bytes can grow as needed, subject to the available memory.
     *
     * @param capacity the initial capacity of the Bytes
     * @return the created Bytes
     */
    @NotNull
    abstract Bytes<?> elasticBytes(int capacity);

    /**
     * Creates a ByteBuffer with the specified capacity.
     *
     * @param capacity the capacity of the ByteBuffer
     * @return the created ByteBuffer
     */
    @NotNull
    abstract ByteBuffer byteBuffer(int capacity);

    /**
     * Creates fixed Bytes with the specified capacity, backed by a ByteBuffer.
     * The resulting Bytes cannot grow beyond the specified capacity.
     *
     * @param capacity the capacity of the Bytes
     * @return the created Bytes
     */
    Bytes<?> fixedBytes(int capacity) {
        return Bytes.wrapForWrite(byteBuffer(capacity));
    }

    static class Parent {
        int start;
    }

    static class Padding extends Parent {
        @FieldGroup("p")
        // 128 bytes
        transient long p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;
        @FieldGroup("p")
        transient long q0, q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12, q13, q14, q15;
    }
}
