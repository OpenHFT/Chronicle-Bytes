/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests large message writes to mapped bytes because correct expansion
 * is essential for handling messages larger than initial chunk size,
 * so that the underlying store grows dynamically to avoid buffer overflows.
 */
@SuppressWarnings({"checkstyle:MMOverusedWord", "checkstyle:MMLacksPurpose", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("Large message writes trigger mapped bytes store expansion")
class CheckOverSizedMessagesTest extends BytesTestCommon {

    private static final byte[] BYTE6K = new byte[6000];

    private static MappedBytes mbNoOverlap() {
        File path = new File(OS.getTarget(), "oversized-" + System.nanoTime());
        path.deleteOnExit();
        try {
            return MappedBytes.mappedBytes(path, 4 << 10, 4 << 10);
        } catch (FileNotFoundException e) {
            throw new AssertionError("Failed to create mapped bytes file", e);
        }
    }

    @BeforeEach
    public void checkPageSize() {
        assumeTrue(OS.isLinux(),
                "Oversized mapping tests are Linux specific");
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for oversized mappings");
    }

    @Test
    @DisplayName("oversized RandomDataInput write expands mapped bytes store")
    public void writeRDI() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            Bytes<?> rdi = Bytes.allocateDirect(BYTE6K);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(rdi);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized RandomDataInput write replaces bytes store");
            rdi.releaseLast();
        }
    }

    @Test
    @DisplayName("oversized byte array write expands mapped bytes store")
    public void writeByteArray2() {
        try (MappedBytes mb = mbNoOverlap()) {
            byte[] arr = new byte[6 << 10];
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(3 << 10, arr);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized byte[] write at position replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized byte array slice write expands mapped store")
    public void writeByteArray4() {
        try (MappedBytes mb = mbNoOverlap()) {
            byte[] arr = new byte[6 << 10];
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(4000, arr, 128, 5900);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized byte[] slice write replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized ByteBuffer write expands mapped bytes store")
    public void writeBB4() {
        try (MappedBytes mb = mbNoOverlap()) {
            ByteBuffer bb = ByteBuffer.allocate(6000);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(4000, bb, 128, 5800);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized ByteBuffer write replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized ByteBuffer write fails without store change")
    public void writeBB4B() {
        try (MappedBytes mb = mbNoOverlap()) {
            assumeFalse(PageUtil.isHugePage(mb.mappedFile().file().getAbsolutePath()),
                    "Huge page mapping changes oversized write behaviour");
            ByteBuffer bb = ByteBuffer.allocate(6000);
            mb.writeLong(4000, -1);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            assertThrows(IndexOutOfBoundsException.class,
                    () -> mb.write(4000, bb, 128, 5900),
                    "Oversized ByteBuffer write throws bounds exception");
            // check untouched
            assertEquals(-1, mb.readLong(4000),
                    "Marker remains after failed oversized write");
            // didn't actually write
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertSame(bs0, bs2,
                    "Failed oversized write keeps original bytes store");
        }
    }

    @Test
    @DisplayName("oversized RandomDataInput slice write expands mapped store")
    public void writeRDI4() {
        try (MappedBytes mb = mbNoOverlap()) {
            RandomDataInput rdi = Bytes.allocateDirect(BYTE6K);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(4000, rdi, 128, 5900);
            rdi.releaseLast();
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized RandomDataInput slice write replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized byte array append expands mapped bytes store")
    public void writeByteArray3() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(4000);
            byte[] arr = new byte[6 << 10];
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(arr, 128, 5900);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized byte[] append replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized InputStream write expands mapped bytes store")
    public void writeIS() throws IOException {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            InputStream is = new ByteArrayInputStream(BYTE6K);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(is);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized InputStream write replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized Bytes write at offset expands mapped store")
    public void writeBytes2() {
        try (MappedBytes mb = mbNoOverlap()) {
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(4000, Bytes.wrapForRead(BYTE6K));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized Bytes write at offset replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized CharSequence write expands mapped bytes store")
    public void writeCS() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(new String(BYTE6K, StandardCharsets.US_ASCII));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized CharSequence write replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized Bytes append expands mapped bytes store")
    public void writeBytes() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(4000);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(Bytes.wrapForRead(BYTE6K));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized Bytes append replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized BytesStore slice write expands mapped store")
    public void writeByteStore3() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<Bytes<byte[]>, byte[]> bytes = Bytes.wrapForRead(BYTE6K);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(bytes, 128L, 5800L);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized BytesStore slice write replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized byte array write at position expands store")
    public void writeByteArray() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(BYTE6K);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized byte[] write replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized CharSequence append expands mapped bytes store")
    public void appendCS() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.append(new String(BYTE6K, StandardCharsets.US_ASCII));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized CharSequence append replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized CharSequence slice append expands mapped store")
    public void appendCS3() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.append(new String(BYTE6K, StandardCharsets.US_ASCII), 128, 5800);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized CharSequence slice append replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized BigDecimal append expands mapped bytes store")
    public void appendBigDecimal() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final byte[] bytes = BYTE6K;
            Arrays.fill(bytes, (byte) '1');
            final String s = new String(bytes, StandardCharsets.US_ASCII);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.append(new BigDecimal(s));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized BigDecimal append replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized Bytes read expands mapped bytes store")
    public void read() {
        Bytes<?> in = Bytes.wrapForRead(BYTE6K);
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            in.read(mb);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized read into mapped bytes replaces bytes store");
        }
    }

    @Test
    @DisplayName("oversized Bytes read length expands mapped store")
    public void read2() {
        Bytes<?> in = Bytes.wrapForRead(BYTE6K);
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            in.read(mb, 5900);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2,
                    "Oversized read length replaces bytes store");
        }
    }
}
