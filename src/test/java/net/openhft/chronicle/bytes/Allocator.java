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
         * Modified {@link Bytes#wrap(ByteBuffer)} that wraps NativeBytesStore by address without providing underlying.
         */
        @Override
        Bytes<?> fixedBytes(int capacity) {
            final ByteBuffer byteBuffer = byteBuffer(capacity);

            BytesStore<?, ByteBuffer> bs = new NativeBytesStore<>(byteBuffer, false, byteBuffer.capacity());
            try {
                try {
                    Bytes<ByteBuffer> bbb = bs.bytesForWrite();
                    bbb.writePosition(byteBuffer.position());
                    bbb.writeLimit(byteBuffer.limit());
                    return bbb;
                } finally {
                    bs.release(ReferenceOwner.INIT);
                }
            } catch (IllegalStateException | BufferOverflowException ise) {
                throw new AssertionError(ise);
            }
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
    },
    HEAP_EMBEDDED {
        @Override
        @NotNull Bytes<?> elasticBytes(int capacity) {
            return fixedBytes(Math.max(capacity, 127));
        }

        @Override
        @NotNull ByteBuffer byteBuffer(int capacity) {
            throw new IllegalArgumentException();
        }

        @Override
        Bytes<?> fixedBytes(int capacity) {
            if (capacity >= 128)
                throw new IllegalArgumentException();
            Padding padding = new Padding();
            return Bytes.forFieldGroup(padding, "p").writeLimit(capacity);
        }
    },
    HEX_DUMP {
        @Override
        @NotNull Bytes<?> elasticBytes(int capacity) {
            return new HexDumpBytes();
        }

        @Override
        @NotNull ByteBuffer byteBuffer(int capacity) {
            throw new IllegalArgumentException();
        }

        @Override
        Bytes<?> fixedBytes(int capacity) {
            return new HexDumpBytes();
        }
    };

    @NotNull
    abstract Bytes<?> elasticBytes(int capacity);

    @NotNull
    abstract ByteBuffer byteBuffer(int capacity);

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
    }
}
