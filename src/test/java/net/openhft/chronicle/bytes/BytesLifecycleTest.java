/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

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
                assertEquals("bytesForRead should reserve the bytes store",
                        initialRefCount + 1, store.refCount());
            } finally {
                slice.releaseLast();
            }
            assertEquals("Releasing the slice should decrement the ref-count",
                    initialRefCount, store.refCount());

            Bytes<?> orphan = parent.bytesForRead();
            try {
                parent.releaseLast();
                parentReleased = true;
                orphan.readPosition(0);
                assertEquals("reference-test", orphan.readUtf8());
            } finally {
                orphan.releaseLast();
            }
            assertEquals("Releasing the final slice should drop ref-count to zero",
                    0, store.refCount());
            assertThrows("Once both parent and slices are released, access must fail",
                    NullPointerException.class, () -> parent.peekUnsignedByte(0));
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
                assertTrue("Capacity must not shrink during elastic growth",
                        currentCapacity >= previousCapacity);
                previousCapacity = currentCapacity;
                elastic.clear();
            }
        } finally {
            elastic.releaseLast();
        }
        assertTrue("Elastic buffer should expand at least once", expanded);
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
            assertEquals(expectedRemaining, copied);
            assertEquals("copyTo should not mutate readPosition", 7, source.readPosition());
            Bytes<?> view = target.bytesForRead();
            try {
                view.readPositionRemaining(0, copied);
                byte[] bytes = new byte[(int) copied];
                view.read(bytes);
                assertEquals("body", new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1));
            } finally {
                view.releaseLast();
            }
        } finally {
            target.releaseLast();
            source.releaseLast();
        }
    }
}
