/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class CheckOverSizedMessagesTest extends BytesTestCommon {

    private static final byte[] BYTE6K = new byte[6000];

    private static MappedBytes mbNoOverlap() {
        File path = new File(OS.getTarget(), "oversized-" + System.nanoTime());
        path.deleteOnExit();
        try {
            return MappedBytes.mappedBytes(path, 4 << 10, 4 << 10);
        } catch (FileNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @BeforeEach
    public void checkPageSize() {
        assumeTrue(OS.isLinux());
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    public void writeRDI() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            Bytes<?> rdi = Bytes.allocateDirect(BYTE6K);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(rdi);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 6K RandomDataInput to 4K chunk should allocate new BytesStore");
            rdi.releaseLast();
        }
    }

    @Test
    public void writeByteArray2() {
        try (MappedBytes mb = mbNoOverlap()) {
            byte[] arr = new byte[6 << 10];
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(3 << 10, arr);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 6K byte array at offset 3K should allocate new BytesStore");
        }
    }

    @Test
    public void writeByteArray4() {
        try (MappedBytes mb = mbNoOverlap()) {
            byte[] arr = new byte[6 << 10];
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(4000, arr, 128, 5900);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 5900 bytes from array at offset 4000 should allocate new BytesStore");
        }
    }

    @Test
    public void writeBB4() {
        try (MappedBytes mb = mbNoOverlap()) {
            ByteBuffer bb = ByteBuffer.allocate(6000);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(4000, bb, 128, 5800);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 5800 bytes from ByteBuffer at offset 4000 should allocate new BytesStore");
        }
    }

    @Test
    public void writeBB4B() {
        try (MappedBytes mb = mbNoOverlap()) {
            assumeFalse(PageUtil.isHugePage(mb.mappedFile().file().getAbsolutePath()));
            ByteBuffer bb = ByteBuffer.allocate(6000);
            mb.writeLong(4000, -1);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            try {
                mb.write(4000, bb, 128, 5900);
                fail("writeBB4B: fail");
            } catch (IndexOutOfBoundsException expected) {
                // check untouched
                assertEquals(-1, mb.readLong(4000), "Long value at offset 4000 should remain -1 after failed write");
            }
            // didn't actually write
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertSame(bs0, bs2, "failed write should not allocate new BytesStore");
        }
    }

    @Test
    public void writeRDI4() {
        try (MappedBytes mb = mbNoOverlap()) {
            RandomDataInput rdi = Bytes.allocateDirect(BYTE6K);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(4000, rdi, 128, 5900);
            rdi.releaseLast();
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 5900 bytes from RandomDataInput at offset 4000 should allocate new BytesStore");
        }
    }

    @Test
    public void writeByteArray3() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(4000);
            byte[] arr = new byte[6 << 10];
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(arr, 128, 5900);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 5900 bytes from array with position at 4000 should allocate new BytesStore");
        }
    }

    @Test
    public void writeIS() throws IOException {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            InputStream is = new ByteArrayInputStream(BYTE6K);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(is);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 6K from InputStream at position 3K should allocate new BytesStore");
        }
    }

    @Test
    public void writeBytes2() {
        try (MappedBytes mb = mbNoOverlap()) {
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(4000, Bytes.wrapForRead(BYTE6K));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 6K Bytes at offset 4000 should allocate new BytesStore");
        }
    }

    @Test
    public void writeCS() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(new String(BYTE6K, StandardCharsets.US_ASCII));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 6K CharSequence at position 3K should allocate new BytesStore");
        }
    }

    @Test
    public void writeBytes() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(4000);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(Bytes.wrapForRead(BYTE6K));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 6K Bytes with position at 4000 should allocate new BytesStore");
        }
    }

    @Test
    public void writeByteStore3() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<Bytes<byte[]>, byte[]> bytes = Bytes.wrapForRead(BYTE6K);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(bytes, 128L, 5800L);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 5800 bytes from BytesStore at position 3K should allocate new BytesStore");
        }
    }

    @Test
    public void writeByteArray() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.write(BYTE6K);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "writing 6K byte array at position 3K should allocate new BytesStore");
        }
    }

    @Test
    public void appendCS() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.append(new String(BYTE6K, StandardCharsets.US_ASCII));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "appending 6K CharSequence at position 3K should allocate new BytesStore");
        }
    }

    @Test
    public void appendCS3() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.append(new String(BYTE6K, StandardCharsets.US_ASCII), 128, 5800);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "appending 5672 chars from CharSequence at position 3K should allocate new BytesStore");
        }
    }

    @Test
    public void appendBigDecimal() {
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final byte[] bytes = BYTE6K;
            Arrays.fill(bytes, (byte) '1');
            final String s = new String(bytes, StandardCharsets.US_ASCII);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            mb.append(new BigDecimal(s));
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "appending large BigDecimal at position 3K should allocate new BytesStore");
        }
    }

    @Test
    public void read() {
        Bytes<?> in = Bytes.wrapForRead(BYTE6K);
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            in.read(mb);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "reading 6K into MappedBytes at position 3K should allocate new BytesStore");
        }
    }

    @Test
    public void read2() {
        Bytes<?> in = Bytes.wrapForRead(BYTE6K);
        try (MappedBytes mb = mbNoOverlap()) {
            mb.writePosition(3 << 10);
            final BytesStore<?, Void> bs0 = mb.bytesStore();
            in.read(mb, 5900);
            final BytesStore<?, Void> bs2 = mb.bytesStore();
            assertNotSame(bs0, bs2, "reading 5900 bytes into MappedBytes at position 3K should allocate new BytesStore");
        }
    }
}
