/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests Bytes lifecycle scenarios for reference counting and elastic growth because
 * correct resource management is required to avoid memory leaks and dangling references.
 */
@DisplayName("Bytes lifecycle behaviour scenarios for reference counting and growth")
public class BytesLifecycleTest extends BytesTestCommon {

    @Test
    @DisplayName("Slices update reference counts on reserve and release")
    public void slicesRespectReferenceCounts() {
        Bytes<?> parent = Bytes.allocateElasticDirect();
        boolean parentReleased = false;
        try {
            parent.writeUtf8("reference-test");
            BytesStore<?, ?> store = parent.bytesStore();
            long initialRefCount = store.refCount();

            Bytes<?> slice = parent.bytesForRead();
            try {
                assertEquals(initialRefCount + 1, store.refCount(),
                        "bytesForRead should reserve the bytes store");
            } finally {
                slice.releaseLast();
            }
            assertEquals(initialRefCount, store.refCount(),
                    "Releasing the slice should decrement the ref-count");

            Bytes<?> orphan = parent.bytesForRead();
            try {
                parent.releaseLast();
                parentReleased = true;
                orphan.readPosition(0);
                assertEquals("reference-test", orphan.readUtf8(),
                        "orphan slice should retain content after parent release");
            } finally {
                orphan.releaseLast();
            }
            assertEquals(0, store.refCount(),
                    "Releasing the final slice should drop ref-count to zero");
            assertThrows(NullPointerException.class, () -> parent.peekUnsignedByte(0),
                    "Once both parent and slices are released, access must fail");
        } finally {
            if (!parentReleased) {
                parent.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("Elastic heap bytes grow monotonically under repeated writes")
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
                        "capacity must not shrink during elastic growth at iteration " + i);
                previousCapacity = currentCapacity;
                elastic.clear();
            }
        } finally {
            elastic.releaseLast();
        }
        assertTrue(expanded, "Elastic buffer should expand at least once");
    }

    @Test
    @DisplayName("copyTo only copies readable bytes from the current view")
    public void copyToCopiesReadableBytesOnly() {
        Bytes<?> source = Bytes.allocateElasticOnHeap();
        source.append("header-body");
        source.readPosition(7); // skip "header-"
        BytesStore<?, Void> target = BytesStore.nativeStoreWithFixedCapacity((int) source.readRemaining());
        try {
            long expectedRemaining = source.readRemaining();
            long copied = source.copyTo(target);
            assertEquals(expectedRemaining, copied,
                    "copyTo should return the number of readable bytes copied");
            assertEquals(7, source.readPosition(),
                    "copyTo should not mutate readPosition");
            Bytes<?> view = target.bytesForRead();
            try {
                view.readPositionRemaining(0, copied);
                byte[] bytes = new byte[(int) copied];
                view.read(bytes);
                assertEquals("body", new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1),
                        "copyTo should copy only the readable suffix");
            } finally {
                view.releaseLast();
            }
        } finally {
            target.releaseLast();
            source.releaseLast();
        }
    }
}
