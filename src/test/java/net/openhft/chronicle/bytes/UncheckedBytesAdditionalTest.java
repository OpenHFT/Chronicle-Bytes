/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UncheckedBytes behaviour with real backing stores")
public class UncheckedBytesAdditionalTest {

    @Test
    @DisplayName("ensureCapacity grows the underlying bytes store")
    public void ensureCapacityGrowsUnderlyingStore() {
        Bytes<?> underlying = Bytes.allocateElasticOnHeap(8);
        try {
            UncheckedBytes<?> unchecked = new UncheckedBytes<>(underlying);
            unchecked.ensureCapacity(64);
            assertTrue(underlying.realCapacity() >= 64,
                    "Underlying store should grow to satisfy the requested capacity");
            unchecked.releaseLast();
        } finally {
            underlying.releaseLast();
        }
    }

    @Test
    @DisplayName("setBytes updates positions from the underlying bytes")
    public void setBytesUpdatesPositions() {
        Bytes<?> first = Bytes.allocateElasticOnHeap(16);
        Bytes<?> second = Bytes.allocateElasticOnHeap(16);
        try {
            first.writePosition(4);
            second.writePosition(8);
            second.readPosition(2);
            UncheckedBytes<?> unchecked = new UncheckedBytes<>(first);
            unchecked.setBytes(second);
            assertEquals(2,
                    unchecked.readPosition(),
                    "readPosition should follow the newly set bytes");
            assertEquals(8,
                    unchecked.writePosition(),
                    "writePosition should follow the newly set bytes");
            unchecked.releaseLast();
        } finally {
            first.releaseLast();
            second.releaseLast();
        }
    }

    @Test
    @DisplayName("copy is unsupported for unchecked bytes")
    public void copyIsUnsupported() {
        Bytes<?> underlying = Bytes.allocateElasticOnHeap(8);
        try {
            UncheckedBytes<?> unchecked = new UncheckedBytes<>(underlying);
            assertThrows(UnsupportedOperationException.class,
                    unchecked::copy,
                    "Unchecked bytes should not support copy");
            unchecked.releaseLast();
        } finally {
            underlying.releaseLast();
        }
    }

    @Test
    @DisplayName("unchecked returns the same instance for fluent calls")
    public void uncheckedReturnsSameInstance() {
        Bytes<?> underlying = Bytes.allocateElasticOnHeap(8);
        try {
            UncheckedBytes<?> unchecked = new UncheckedBytes<>(underlying);
            assertSame(unchecked,
                    unchecked.unchecked(true),
                    "unchecked should return the same instance");
            unchecked.releaseLast();
        } finally {
            underlying.releaseLast();
        }
    }

    @Test
    @DisplayName("write uses the long fast path for eight bytes")
    public void writeUsesLongFastPath() {
        Bytes<?> underlying = Bytes.allocateElasticOnHeap(16);
        Bytes<?> source = Bytes.allocateElasticOnHeap(16);
        UncheckedBytes<?> unchecked = new UncheckedBytes<>(underlying);
        try {
            long value = 0x0102030405060708L;
            source.writeLong(0, value);
            unchecked.write((BytesStore<?, ?>) source, 0L, 8L);
            assertEquals(value,
                    unchecked.readLong(0),
                    "Unchecked write should copy eight-byte values using the long fast path");
        } finally {
            unchecked.releaseLast();
            source.releaseLast();
            underlying.releaseLast();
        }
    }

    @Test
    @DisplayName("write uses rawCopy for native stores with large lengths")
    public void writeUsesRawCopyForNativeStores() {
        BytesStore<?, Void> sourceStore = BytesStore.nativeStoreWithFixedCapacity(64);
        BytesStore<?, Void> targetStore = BytesStore.nativeStoreWithFixedCapacity(64);
        Bytes<?> source = NativeBytes.wrapWithNativeBytes(sourceStore, sourceStore.capacity());
        Bytes<?> target = NativeBytes.wrapWithNativeBytes(targetStore, targetStore.capacity());
        UncheckedBytes<?> unchecked = new UncheckedBytes<>(target);
        try {
            byte[] payload = new byte[32];
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (i + 1);
            }
            source.write(payload);
            unchecked.write((BytesStore<?, ?>) source, 0L, payload.length);
            byte[] actual = new byte[payload.length];
            for (int i = 0; i < actual.length; i++) {
                actual[i] = targetStore.readByte(i);
            }
            assertArrayEquals(payload,
                    actual,
                    "Unchecked rawCopy should transfer native payload bytes");
        } finally {
            unchecked.releaseLast();
            source.releaseLast();
            target.releaseLast();
            sourceStore.releaseLast();
            targetStore.releaseLast();
        }
    }

    @Test
    @DisplayName("write falls back to the default copy path for small lengths")
    public void writeFallsBackForSmallLengths() {
        Bytes<?> underlying = Bytes.allocateElasticOnHeap(16);
        Bytes<?> source = Bytes.allocateElasticOnHeap(16);
        UncheckedBytes<?> unchecked = new UncheckedBytes<>(underlying);
        try {
            source.write(new byte[] {9, 8, 7, 6});
            unchecked.write((BytesStore<?, ?>) source, 0L, 4L);
            byte[] actual = new byte[] {
                    underlying.readByte(0),
                    underlying.readByte(1),
                    underlying.readByte(2),
                    underlying.readByte(3)
            };
            assertArrayEquals(new byte[] {9, 8, 7, 6},
                    actual,
                    "Unchecked write should copy small payloads using the standard path");
        } finally {
            unchecked.releaseLast();
            source.releaseLast();
            underlying.releaseLast();
        }
    }

    @Test
    @DisplayName("append8bit replaces characters outside Latin-1 range")
    public void append8bitReplacesNonLatin1() {
        Bytes<?> underlying = Bytes.allocateElasticOnHeap(16);
        UncheckedBytes<?> unchecked = new UncheckedBytes<>(underlying);
        try {
            unchecked.append8bit("A\u20ACB");
            byte[] actual = new byte[] {
                    underlying.readByte(0),
                    underlying.readByte(1),
                    underlying.readByte(2)
            };
            assertEquals("A?B",
                    new String(actual, java.nio.charset.StandardCharsets.ISO_8859_1),
                    "append8bit should replace characters outside Latin-1 with '?'");
        } finally {
            unchecked.releaseLast();
            underlying.releaseLast();
        }
    }

    @Test
    @DisplayName("writeUtf8 encodes null as stopbit -1")
    public void writeUtf8EncodesNull() {
        Bytes<?> underlying = Bytes.allocateElasticOnHeap(16);
        UncheckedBytes<?> unchecked = new UncheckedBytes<>(underlying);
        try {
            unchecked.writeUtf8(null);
            unchecked.readPosition(0);
            assertEquals(-1L,
                    unchecked.readStopBit(),
                    "writeUtf8 should encode null as stopbit -1");
        } finally {
            unchecked.releaseLast();
            underlying.releaseLast();
        }
    }

    @Test
    @DisplayName("appendUtf8 writes ASCII and multi-byte characters")
    public void appendUtf8WritesAsciiAndMultibyte() {
        Bytes<?> underlying = Bytes.allocateElasticOnHeap(16);
        UncheckedBytes<?> unchecked = new UncheckedBytes<>(underlying);
        try {
            char[] ascii = "abc".toCharArray();
            char[] multi = new char[] {'\u00A3'};
            unchecked.appendUtf8(ascii, 0, ascii.length);
            unchecked.appendUtf8(multi, 0, 1);
            assertEquals('a',
                    underlying.readByte(0),
                    "ASCII characters should be written directly");
            assertEquals((byte) 0xC2,
                    underlying.readByte(3),
                    "Multi-byte UTF-8 lead byte should be present");
            assertEquals((byte) 0xA3,
                    underlying.readByte(4),
                    "Multi-byte UTF-8 trailing byte should be present");
        } finally {
            unchecked.releaseLast();
            underlying.releaseLast();
        }
    }

    @Test
    @DisplayName("rawCopy returns zero when there is nothing to copy")
    public void rawCopyReturnsZeroWhenNoData() {
        Bytes<?> underlying = Bytes.allocateElasticOnHeap(8);
        Bytes<?> source = Bytes.allocateElasticOnHeap(8);
        UncheckedBytes<?> unchecked = new UncheckedBytes<>(underlying);
        try {
            long copied = unchecked.rawCopy((BytesStore<?, ?>) source, 0L, 0L);
            assertEquals(0L,
                    copied,
                    "rawCopy should return zero when no bytes are requested");
        } finally {
            unchecked.releaseLast();
            source.releaseLast();
            underlying.releaseLast();
        }
    }
}
