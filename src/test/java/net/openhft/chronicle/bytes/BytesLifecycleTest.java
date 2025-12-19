/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;

public class BytesLifecycleTest extends BytesTestCommon {

    @Test
    public void slicesRespectReferenceCounts() {
        Bytes<?> parent = Bytes.allocateElasticDirect();
        boolean parentReleased = false;
        try {
            parent.writeUtf8("reference-test");
            BytesStore<?, ?> store = parent.bytesStore();
            long initialRefCount = store.refCount();

            Bytes<?> slice = parent.bytesForRead();
            try {
                assertEquals(initialRefCount + 1,
                        store.refCount(), "bytesForRead should reserve the bytes store");
            } finally {
                slice.releaseLast();
            }
            assertEquals(initialRefCount,
                    store.refCount(), "Releasing the slice should decrement the ref-count");

            Bytes<?> orphan = parent.bytesForRead();
            try {
                parent.releaseLast();
                parentReleased = true;
                orphan.readPosition(0);
                assertEquals("reference-test", orphan.readUtf8(), "Slice should retain access to data after parent is released");
            } finally {
                orphan.releaseLast();
            }
            assertEquals(0,
                    store.refCount(), "Releasing the final slice should drop ref-count to zero");
            assertThrows(NullPointerException.class,
                    () -> parent.peekUnsignedByte(0), "Once both parent and slices are released, access must fail");
        } finally {
            if (!parentReleased) {
                parent.releaseLast();
            }
        }
    }

    @Test
    public void elasticHeapBytesGrowMonotonically() {
        Bytes<ByteBuffer> elastic = Bytes.elasticByteBuffer(8);
        boolean expanded = false;
        try {
            long previousCapacity = elastic.realCapacity();
            for (int i = 1; i <= 5; i++) {
                byte[] block = new byte[i * 32];
                elastic.write(block);
                long currentCapacity = elastic.realCapacity();
                if (currentCapacity > previousCapacity) {
                    expanded = true;
                }
                assertTrue(currentCapacity >= previousCapacity,
                        "Capacity must not shrink during elastic growth");
                previousCapacity = currentCapacity;
                elastic.clear();
            }
        } finally {
            elastic.releaseLast();
        }
        assertTrue(expanded, "Elastic buffer should expand at least once");
    }

    @Test
    public void copyToCopiesReadableBytesOnly() {
        Bytes<?> source = Bytes.allocateElasticOnHeap();
        source.append("header-body");
        source.readPosition(7); // skip "header-"
        BytesStore<?, Void> target = BytesStore.nativeStoreWithFixedCapacity((int) source.readRemaining());
        try {
            long expectedRemaining = source.readRemaining();
            long copied = source.copyTo(target);
            assertEquals(expectedRemaining, copied, "copyTo should return number of bytes copied (equals readRemaining)");
            assertEquals(7, source.readPosition(), "copyTo should not mutate readPosition");
            Bytes<?> view = target.bytesForRead();
            try {
                view.readPositionRemaining(0, copied);
                byte[] bytes = new byte[(int) copied];
                view.read(bytes);
                assertEquals("body", new String(bytes, ISO_8859_1), "copyToCopiesReadableBytesOnly: assertEquals");
            } finally {
                view.releaseLast();
            }
        } finally {
            target.releaseLast();
            source.releaseLast();
        }
    }
}
