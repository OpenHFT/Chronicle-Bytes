/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VanillaBytes covers read and compareTo branch scenarios")
public class VanillaBytesAdditionalTest extends BytesTestCommon {

    @Test
    @DisplayName("read uses native store fast path for direct bytes")
    public void readUsesNativeStoreFastPath() {
        Bytes<?> bytes = Bytes.allocateDirect(16);
        try {
            bytes.write("data".getBytes(ISO_8859_1));
            bytes.readPosition(0);
            byte[] target = new byte[4];
            int len = bytes.read(target);
            assertEquals(4,
                    len,
                    "read should return number of bytes copied from direct store");
            assertEquals(4,
                    bytes.readPosition(),
                    "read should advance the read position");
            assertEquals('d',
                    target[0],
                    "read should copy the first byte into the target array");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("read uses generic path for heap bytes")
    public void readUsesGenericPathForHeapBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(16);
        try {
            bytes.write("heap".getBytes(ISO_8859_1));
            bytes.readPosition(0);
            byte[] target = new byte[4];
            int len = bytes.read(target);
            assertEquals(4,
                    len,
                    "read should return number of bytes copied from heap store");
            assertEquals('h',
                    target[0],
                    "read should copy data into the heap buffer");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("compareTo detects differing content and lengths")
    public void compareToDetectsDifferences() {
        BytesStore<?, ?> store = BytesStore.wrap("abc".getBytes(ISO_8859_1));
        VanillaBytes<?> bytes = VanillaBytes.wrap(store);
        try {
            bytes.readPosition(0);
            bytes.readLimit(store.capacity());
            bytes.writeLimit(store.capacity());
            assertTrue(bytes.compareTo("abd") < 0,
                    "compareTo should report smaller when bytes differ earlier");
            assertTrue(bytes.compareTo("ab") > 0,
                    "compareTo should report larger when bytes are longer");
        } finally {
            bytes.releaseLast();
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("toTemporaryDirectByteBuffer handles clear and non-clear states")
    public void toTemporaryDirectByteBufferHandlesStates() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            ByteBuffer clearBuffer = bytes.toTemporaryDirectByteBuffer();
            assertNotNull(clearBuffer,
                    "Clear bytes should provide a temporary direct buffer");
            bytes.writeLimit(8);
            ByteBuffer limitedBuffer = bytes.toTemporaryDirectByteBuffer();
            assertNotNull(limitedBuffer,
                    "Non-clear bytes should provide a temporary direct buffer");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("isEqual compares strings for direct and heap stores")
    public void isEqualComparesDirectAndHeap() {
        Bytes<?> direct = Bytes.allocateDirect(16);
        Bytes<?> heap = Bytes.allocateElasticOnHeap(16);
        try {
            direct.write("abc".getBytes(ISO_8859_1));
            heap.write("abc".getBytes(ISO_8859_1));
            direct.readPosition(0);
            heap.readPosition(0);
            assertTrue(((VanillaBytes<?>) direct).isEqual("abc"),
                    "Direct bytes should report equality with matching content");
            assertTrue(((VanillaBytes<?>) heap).isEqual("abc"),
                    "Heap bytes should report equality with matching content");
        } finally {
            direct.releaseLast();
            heap.releaseLast();
        }
    }

    @Test
    @DisplayName("copy handles byte buffer and array-backed stores")
    public void copyHandlesByteBufferAndArrayStores() {
        BytesStore<?, ByteBuffer> bufferStore = BytesStore.wrap(ByteBuffer.allocate(8));
        BytesStore<?, byte[]> arrayStore = BytesStore.wrap("xy".getBytes(ISO_8859_1));
        VanillaBytes<?> bufferBytes = VanillaBytes.wrap(bufferStore);
        VanillaBytes<?> arrayBytes = VanillaBytes.wrap(arrayStore);
        try {
            bufferBytes.writeByte(0, (byte) 'b');
            bufferBytes.writeByte(1, (byte) 'c');
            bufferBytes.readLimit(2);
            BytesStore<?, ?> bufferCopy = bufferBytes.copy();
            assertEquals('b',
                    bufferCopy.readByte(0),
                    "ByteBuffer-backed copy should preserve data");

            arrayBytes.readLimit(arrayStore.capacity());
            BytesStore<?, ?> arrayCopy = arrayBytes.copy();
            assertEquals('x',
                    arrayCopy.readByte(0),
                    "Array-backed copy should preserve data");
        } finally {
            bufferBytes.releaseLast();
            arrayBytes.releaseLast();
            bufferStore.releaseLast();
            arrayStore.releaseLast();
        }
    }
}
