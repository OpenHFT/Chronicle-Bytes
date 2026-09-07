/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * A MappedBytes only exposes its current chunk as its bytesStore(), while its read limit can extend past that
 * mapping. Chronicle Queue's compact read-only table store maps a file as exact, zero-overlap chunks, so a
 * string appended by a writer can start inside the reader's current mapping and end beyond it. The native
 * string-reading fast paths used to read that raw memory beyond the mapping.
 */
public class MappedBytesReadAcrossMappingTest extends BytesTestCommon {

    // one exact, zero-overlap chunk for the reader, as the compact read-only table store uses
    private final long chunk = 16L * OS.pageSize();
    // the string starts shortly before the end of the reader's mapping and ends well past it
    private final long position = chunk - 16;
    private File file;
    private MappedBytes writer;
    private MappedBytes reader;

    @Before
    public void openMappings() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        file = new File(OS.getTarget(), "mapped-read-across-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        // the writer's overlap keeps its own writes contiguous; only the reader's mapping ends at the chunk
        writer = MappedBytes.mappedBytes(file, chunk, chunk);
        reader = MappedBytes.mappedBytes(file, chunk, 0L, true);
    }

    @After
    public void closeMappings() {
        if (reader != null)
            reader.releaseLast();
        if (writer != null)
            writer.releaseLast();
        BackgroundResourceReleaser.releasePendingResources();
        if (file != null)
            deleteIfPossible(file);
    }

    @Test
    public void read8bitAcrossMappingBoundary() {
        final String text = repeat('k', 180) + "399";
        writer.writePosition(position);
        writer.write8bit(text);
        final long end = writer.writePosition();
        final long textStart = end - text.length();

        syncReader(position);
        assertMappingEndsInsideText(textStart, text.length());

        final StringBuilder sb = new StringBuilder();
        assertTrue(reader.read8bit(sb));
        assertEquals(text, sb.toString());
        assertEquals(end, reader.readPosition());
    }

    @Test
    public void read8bitWithinCurrentMapping() {
        final String text = repeat('k', 180) + "399";
        writer.writePosition(position - text.length() - 8);
        writer.write8bit(text);
        final long end = writer.writePosition();

        syncReader(position - text.length() - 8);
        assertTrue(reader.bytesStore().inside(end - text.length(), text.length()));

        final StringBuilder sb = new StringBuilder();
        assertTrue(reader.read8bit(sb));
        assertEquals(text, sb.toString());
        assertEquals(end, reader.readPosition());
    }

    @Test
    public void parseUtf8StopCharAcrossMappingBoundary() {
        final String text = repeat('v', 200);
        writer.writePosition(position);
        writer.appendUtf8(text).appendUtf8(",rest");

        syncReader(position);
        assertMappingEndsInsideText(position, text.length());

        // larger than the distance to the boundary, so the raw scan reaches the mapping end rather than the builder's capacity
        final StringBuilder sb = new StringBuilder(256);
        reader.parseUtf8(sb, StopCharTesters.COMMA_STOP);
        assertEquals(text, sb.toString());
        assertEquals("the stop char is consumed", position + text.length() + 1, reader.readPosition());
    }

    @Test
    public void parseUtf8StopCharAcrossMappingBoundaryUpToTheReadLimit() {
        final String text = repeat('v', 200);
        writer.writePosition(position);
        writer.appendUtf8(text);
        final long end = writer.writePosition();

        syncReader(position);
        assertMappingEndsInsideText(position, text.length());

        // larger than the distance to the boundary, so the raw scan reaches the mapping end rather than the builder's capacity
        final StringBuilder sb = new StringBuilder(256);
        reader.parseUtf8(sb, StopCharTesters.COMMA_STOP);
        assertEquals(text, sb.toString());
        assertEquals("without a stop char the whole text is consumed", end, reader.readPosition());
    }

    @Test
    public void readUtf8AtOffsetAcrossMappingBoundary() {
        final String text = repeat('u', 200) + "é";
        writer.writePosition(position);
        writer.writeUtf8(text);
        final long end = writer.writePosition();

        syncReader(position);
        assertMappingEndsInsideText(end - text.length() - 1, text.length());

        final StringBuilder sb = new StringBuilder();
        assertEquals(end, reader.readUtf8(position, sb));
        assertEquals(text, sb.toString());
        assertEquals("a random access read does not move the read position", position, reader.readPosition());
    }

    @Test
    public void primitiveReadStraddlingTheMappingEndIsRejected() {
        // no single mapping covers a long that straddles a zero-overlap chunk end: reject it rather than read raw memory
        final long straddling = chunk - 4;
        writer.writePosition(straddling);
        writer.writeLong(0x0102030405060708L);
        syncReader(position);
        assertFalse(reader.bytesStore().inside(straddling, Long.BYTES));

        assertThrows(DecoratedBufferUnderflowException.class, () -> reader.readLong(straddling));
        assertThrows(DecoratedBufferUnderflowException.class, () -> reader.readVolatileLong(straddling));
        assertThrows(DecoratedBufferUnderflowException.class, () -> reader.addressForRead(straddling, Long.BYTES));
        reader.readPosition(straddling);
        assertThrows(DecoratedBufferUnderflowException.class, reader::readLong);
        assertEquals("a rejected read leaves the read position alone", straddling, reader.readPosition());
        assertEquals("a byte-wise read of the same bytes still works", 0x0102030405060708L, Long.reverseBytes(readLongByteWise(reader, straddling)));
    }

    @Test
    public void volatileShortStraddlingTheMappingEndIsRejected() {
        final long straddling = chunk - 1;
        writer.writePosition(straddling);
        writer.writeShort((short) 0x0102);
        syncReader(position);
        assertFalse(reader.bytesStore().inside(straddling, Short.BYTES));

        assertThrows(DecoratedBufferUnderflowException.class, () -> reader.readVolatileShort(straddling));
        assertEquals("a rejected read leaves the read position alone", position, reader.readPosition());
    }

    @Test
    public void volatileIntStraddlingTheMappingEndIsRejected() {
        final long straddling = chunk - 2;
        writer.writePosition(straddling);
        writer.writeInt(0x01020304);
        syncReader(position);
        assertFalse(reader.bytesStore().inside(straddling, Integer.BYTES));

        assertThrows(DecoratedBufferUnderflowException.class, () -> reader.readVolatileInt(straddling));
        reader.readPosition(straddling);
        assertThrows(DecoratedBufferUnderflowException.class, reader::peekVolatileInt);
        assertEquals("a rejected peek leaves the read position alone", straddling, reader.readPosition());
    }

    @Test
    public void compareAndSwapLongStraddlingTheMappingEndIsRejected() throws IOException {
        final long straddling = chunk - 4;
        final long expected = 0x0102030405060708L;
        writer.writePosition(straddling);
        writer.writeLong(expected);

        try (MappedBytes zeroOverlapWriter = MappedBytes.mappedBytes(file, chunk, 0L)) {
            zeroOverlapWriter.writePosition(straddling);
            assertFalse(zeroOverlapWriter.bytesStore().inside(straddling, Long.BYTES));

            assertThrows(DecoratedBufferUnderflowException.class,
                    () -> zeroOverlapWriter.compareAndSwapLong(straddling, expected, 42L));
            assertEquals("a rejected swap leaves the value alone", expected, writer.readLong(straddling));
        }
    }

    @Test
    public void equalBytesAcrossTheMappingEndCompares() {
        // the review probe that crashed the JVM: equalBytes compares eight bytes at a time through readLong(offset)
        final String text = repeat('x', 200);
        writer.writePosition(position);
        writer.writeUtf8(text);
        final long textStart = writer.writePosition() - text.length();
        syncReader(textStart);
        assertMappingEndsInsideText(textStart, text.length());

        final Bytes<?> same = Bytes.from(text);
        final Bytes<?> differentAfterTheBoundary = Bytes.from(repeat('x', 199) + "y");
        try {
            assertTrue("the word loop hands over to the byte loop at the mapping end", reader.equalBytes(same, text.length()));
            assertFalse(reader.equalBytes(differentAfterTheBoundary, text.length()));
            assertTrue("the mapped operand on the right takes the same byte loop", same.equalBytes(reader, text.length()));
            assertFalse(differentAfterTheBoundary.equalBytes(reader, text.length()));
            assertEquals("equalBytes does not move the read position", textStart, reader.readPosition());
        } finally {
            same.releaseLast();
            differentAfterTheBoundary.releaseLast();
        }
    }

    @Test
    public void parseUtf8StopCharAfterARandomReadRemappedAhead() {
        final String text = repeat('v', 200);
        writer.writePosition(position);
        writer.appendUtf8(text).appendUtf8(",");
        writer.writePosition(2 * chunk);
        writer.writeUnsignedByte(1);
        syncReader(position);
        reader.readUnsignedByte(2 * chunk);            // remaps ahead: the cursor is now before the current store
        assertEquals(2 * chunk, reader.bytesStore().start());

        final StringBuilder sb = new StringBuilder();
        reader.parseUtf8(sb, StopCharTesters.COMMA_STOP);
        assertEquals(text, sb.toString());
    }

    @Test
    public void bulkReadAcrossTheMappingEnd() {
        final byte[] data = pattern(100, 1, 1);
        writer.writePosition(chunk - 50);
        writer.write(data);
        syncReader(chunk - 50);
        assertFalse(reader.bytesStore().inside(chunk - 50, data.length));

        final byte[] out = new byte[data.length];
        assertEquals(data.length, reader.read(out));
        assertArrayEquals("the copy continues in the next mapping", data, out);
        assertEquals(chunk + 50, reader.readPosition());
        assertEquals(chunk, reader.bytesStore().start());
    }

    @Test
    public void copyToAcrossTheMappingEnd() {
        final byte[] data = pattern(100, 1, 1);
        writer.writePosition(chunk - 50);
        writer.write(data);
        writer.writePosition(2 * chunk + 5);
        writer.write(data, 0, 10);
        syncReader(chunk - 50);

        final byte[] out = new byte[data.length];
        assertEquals(data.length, reader.copyTo(out));
        assertArrayEquals(data, out);
        assertEquals("copyTo does not move the read position", chunk - 50, reader.readPosition());
        final byte[] later = new byte[10];
        assertEquals(10, reader.read(2 * chunk + 5, later, 0, 10));
        assertArrayEquals(Arrays.copyOf(data, 10), later);
        assertEquals("the cursor's own chunk is re-acquired on the next read", 1, reader.readUnsignedByte());
    }

    @Test
    public void peekUnsignedByteInALaterMappingMatchesTheRead() {
        final long later = 2 * chunk + 5;
        writer.writePosition(later);
        writer.writeUnsignedByte(0x7B);
        syncReader(0);
        assertEquals(0, reader.bytesStore().start());

        assertEquals(0x7B, reader.peekUnsignedByte(later));
        assertEquals(0x7B, reader.readUnsignedByte(later));
        assertEquals("a peek at the read limit is still -1", -1, reader.peekUnsignedByte(reader.readLimit()));
    }

    @Test
    public void hashAcrossTheMappingEndIsRejected() throws IOException {
        final byte[] data = pattern(64, 7, 3);
        writer.writePosition(chunk - 24);
        writer.write(data);
        final Bytes<byte[]> heap = Bytes.wrapForRead(Arrays.copyOf(data, 40));
        try (MappedBytes overlapping = MappedBytes.mappedBytes(file, chunk, OS.pageSize(), true)) {
            overlapping.readLimit(writer.writePosition());
            overlapping.readPositionRemaining(chunk - 24, 40);
            final long expected = overlapping.hash(40);
            assertEquals("the raw hash within one mapping agrees with the heap hash", heap.hash(40), expected);

            reader.readLimit(writer.writePosition());
            reader.readPositionRemaining(chunk - 24, 40);
            assertFalse(reader.bytesStore().inside(chunk - 24, 40));
            final BytesStore<?, ?> mapping = reader.bytesStore();
            assertThrows(UnsupportedOperationException.class, () -> reader.hash(40));

            // Reject both aligned words and a word that straddles the mapping end.
            reader.readPositionRemaining(chunk - 20, 40);
            assertThrows(UnsupportedOperationException.class, () -> reader.hash(40));
            // Check the small rejection first so a regression cannot attempt a multi-TB allocation in this test.
            final long largeLength = 4L << 40;
            reader.readLimit(reader.readPosition() + largeLength);
            assertThrows(UnsupportedOperationException.class, () -> reader.hash(largeLength));
            assertThrows(UnsupportedOperationException.class, () -> reader.hash(Long.MAX_VALUE - 31));
            assertSame("rejection neither copies nor acquires another chunk", mapping, reader.bytesStore());
            assertEquals(chunk - 20, reader.readPosition());
            assertEquals(chunk - 20 + largeLength, reader.readLimit());
        } finally {
            heap.releaseLast();
        }
    }

    @Test
    public void hashPrefixWithinCurrentMappingMatchesNative() {
        final byte[] data = pattern(128, 37, 11);
        final Bytes<?> direct = Bytes.allocateElasticDirect();
        try {
            direct.write(data);
            // at chunk - 4 an erroneous full-word read of a short prefix would straddle the boundary; at chunk - 24 it fits
            for (long start : new long[]{chunk - 24, chunk - 4}) {
                writer.writePosition(start).write(data);
                reader.readLimit(writer.writePosition());
                final long limit = reader.readLimit();
                for (int length = 0; length <= 96; length++) {
                    reader.readPosition(start);
                    if (length <= chunk - start) {
                        assertEquals("mapped prefix of " + length + " bytes at chunk - " + (chunk - start),
                                direct.hash(length), reader.hash(length));
                    } else {
                        final int rejectedLength = length;
                        assertThrows(UnsupportedOperationException.class, () -> reader.hash(rejectedLength));
                    }
                    assertEquals(start, reader.readPosition());
                    assertEquals(limit, reader.readLimit());
                }
                reader.readUnsignedByte(chunk + 80);
                assertEquals(chunk, reader.bytesStore().start());
                assertThrows(UnsupportedOperationException.class, () -> reader.hash(4));
                assertEquals(start, reader.readPosition());
                assertEquals(limit, reader.readLimit());
                reader.readPosition(reader.readPosition());
                assertEquals("reselecting the cursor's chunk restores the contiguous hash", direct.hash(4), reader.hash(4));
            }
        } finally {
            direct.releaseLast();
        }
    }

    @Test
    public void parseUtf8StopCharEndOfInputIsTheSameOnEveryBackend() throws IOException {
        // a clean end of input between complete characters ends the scan; a malformed byte, a truncated character or an
        // overlong lead byte throws, on every backend
        final byte[] ascii = utf8(repeat('v', 200));
        final byte[] completeCharacter = utf8(repeat('v', 20) + "\u00e9");          // ends in the two bytes C3 A9
        final byte[] malformedByte = withTrailingByte(utf8(repeat('v', 16)), 0xFF);
        final byte[] truncatedCharacter = withTrailingByte(utf8(repeat('v', 16)), 0xC3);
        final byte[] overlongC0 = withTrailingByte(withTrailingByte(utf8(repeat('v', 16)), 0xC0), 0x80);   // overlong U+0000
        final byte[] overlongC1 = withTrailingByte(withTrailingByte(utf8(repeat('v', 16)), 0xC1), 0x80);   // overlong U+0040
        try (MappedBytes overlapping = MappedBytes.mappedBytes(file, chunk, OS.pageSize(), true)) {
            for (int capacity : new int[]{16, 256}) {
                assertStopCharScanOnEveryBackend(overlapping, ascii, capacity, repeat('v', 200), null);
                assertStopCharScanOnEveryBackend(overlapping, completeCharacter, capacity, repeat('v', 20) + "\u00e9", null);
                assertStopCharScanOnEveryBackend(overlapping, malformedByte, capacity, null, UTFDataFormatRuntimeException.class);
                assertStopCharScanOnEveryBackend(overlapping, truncatedCharacter, capacity, null, UTFDataFormatRuntimeException.class);
                assertStopCharScanOnEveryBackend(overlapping, overlongC0, capacity, null, UTFDataFormatRuntimeException.class);
                assertStopCharScanOnEveryBackend(overlapping, overlongC1, capacity, null, UTFDataFormatRuntimeException.class);
            }
        }
    }

    /** heap, direct and an overlapping mapping once; the zero-overlap reader at two distances from its boundary */
    private void assertStopCharScanOnEveryBackend(MappedBytes overlapping, byte[] input, int capacity,
                                                  String expectedText, Class<? extends RuntimeException> expectedFailure) {
        final String label = " with capacity " + capacity + " for " + input.length + " bytes";
        final Bytes<?> heap = Bytes.allocateElasticOnHeap(512).write(input);
        final Bytes<?> direct = Bytes.allocateElasticDirect(512).write(input);
        try {
            assertStopCharScan("heap" + label, heap, 0, capacity, expectedText, expectedFailure);
            assertStopCharScan("direct" + label, direct, 0, capacity, expectedText, expectedFailure);
            writer.writePosition(100).write(input);
            overlapping.readPositionRemaining(100, input.length);
            assertStopCharScan("overlapping mapping" + label, overlapping, 100, capacity, expectedText, expectedFailure);
            for (long start : new long[]{position, chunk - 32}) {
                writer.writePosition(start).write(input);
                // position before limiting: a cross-chunk readPosition after a lowered limit restores the old limit
                reader.readPositionRemaining(start, input.length);
                assertStopCharScan("zero-overlap boundary at chunk - " + (chunk - start) + label,
                        reader, start, capacity, expectedText, expectedFailure);
            }
        } finally {
            heap.releaseLast();
            direct.releaseLast();
        }
    }

    private static void assertStopCharScan(String label, Bytes<?> bytes, long start, int capacity,
                                           String expectedText, Class<? extends RuntimeException> expectedFailure) {
        final StringBuilder sb = new StringBuilder(capacity);
        bytes.readPosition(start);
        if (expectedFailure == null) {
            bytes.parseUtf8(sb, StopCharTesters.COMMA_STOP);
            assertEquals(label, expectedText, sb.toString());
        } else {
            assertThrows(label, expectedFailure, () -> bytes.parseUtf8(sb, StopCharTesters.COMMA_STOP));
        }
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] withTrailingByte(byte[] head, int trailing) {
        final byte[] out = Arrays.copyOf(head, head.length + 1);
        out[head.length] = (byte) trailing;
        return out;
    }

    @Test
    public void hashWithinOverlapMatchesNativeForASignedTailWord() throws IOException {
        // the high int of the tail's first word is Integer.MIN_VALUE, where the heap and native hashes differ
        for (int length : new int[]{9, 12, 16, 40, 48}) {
            final byte[] data = pattern(length, 37, 11);
            final int tail = length <= 16 ? 0 : 32;
            data[tail + 4] = 0;
            data[tail + 5] = 0;
            data[tail + 6] = 0;
            data[tail + 7] = (byte) 0x80;
            final Bytes<?> direct = Bytes.allocateElasticDirect(64).write(data);
            try (MappedBytes overlapping = MappedBytes.mappedBytes(file, chunk, OS.pageSize(), true)) {
                writer.writePosition(chunk - 8).write(data);
                overlapping.readPositionRemaining(chunk - 8, length);
                assertTrue(overlapping.bytesStore().inside(chunk - 8, length));
                assertEquals("length " + length, direct.hash(length), overlapping.hash(length));
            } finally {
                direct.releaseLast();
            }
        }
    }

    //! Review disposition: the next two cases pin paths that already read through checked calls (the streaming
    //! readUtf8(sb) counts through readByte(offset); the two-char stop tester reads byte by byte). No release hunk claims
    //! them; they exist so that a future raw fast path on either method fails here before it reaches Queue.
    @Test
    public void readUtf8AcrossMappingBoundary() {
        final String text = repeat('u', 200) + "\u00e9";
        writer.writePosition(position);
        writer.writeUtf8(text);
        syncReader(position);
        assertMappingEndsInsideText(position + 2, text.length() + 1);

        final StringBuilder sb = new StringBuilder();
        assertTrue(reader.readUtf8(sb));
        assertEquals(text, sb.toString());
        assertEquals(writer.writePosition(), reader.readPosition());
    }

    @Test
    public void parseUtf8TwoCharStopTesterAcrossMappingBoundary() {
        final String text = repeat('v', 200);
        writer.writePosition(position);
        writer.appendUtf8(text).appendUtf8(",rest");
        syncReader(position);

        final StringBuilder sb = new StringBuilder();
        reader.parseUtf8(sb, (ch, peekNextCh) -> ch == ',');
        assertEquals(text, sb.toString());
        assertEquals("the stop char is consumed", position + text.length() + 1, reader.readPosition());
    }

    @Test
    public void copyFromAZeroOverlapSourceAcrossItsMappingEnd() throws IOException {
        final byte[] data = pattern(200, 1, 1);
        writer.writePosition(position).write(data);
        syncReader(position);
        assertFalse(reader.bytesStore().inside(position, data.length));

        final Bytes<?> direct = Bytes.allocateElasticDirect(256);
        try {
            direct.write(0, reader, position, data.length);                         // store copy at an offset
            assertArrayEquals("random-access copy", data, bytesOf(direct, data.length));
            direct.clear();
            ((VanillaBytes<?>) direct).optimisedWrite(reader, position, data.length); // the protected fast path, same package
            assertArrayEquals("optimisedWrite", data, bytesOf(direct, data.length));
            direct.clear();
            direct.write((BytesStore<?, ?>) reader, position, (long) data.length);   // store-to-store copy
            assertArrayEquals("store copy", data, bytesOf(direct, data.length));
            for (long start : new long[]{position, chunk - 13}) {                  // word-aligned and not
                writer.writePosition(start).write(data);
                reader.readLimit(writer.writePosition());
                direct.clear();
                direct.writeSkip(data.length);
                reader.readPosition(start);
                reader.unsafeRead(direct.addressForRead(0), data.length);          // raw copy into native memory
                assertArrayEquals("unsafeRead from " + start, data, bytesOf(direct, data.length));
                assertEquals(start + data.length, reader.readPosition());
                final byte[] object = new byte[data.length];
                reader.readPosition(start);
                reader.unsafeReadObject(object, Jvm.arrayByteBaseOffset(), data.length);
                assertArrayEquals("unsafeReadObject from " + start, data, object);
            }
            // a range inside one chunk, copied while the source's cursor is two chunks ahead; the direct copy acquires
            // that chunk through addressForRead(offset), which moves the source's read position (pre-existing)
            writer.writePosition(100).write(data, 0, 50);
            writer.writePosition(2 * chunk).writeUnsignedByte(1);
            syncReader(2 * chunk);
            direct.clear();
            direct.write((BytesStore<?, ?>) reader, 100L, 50L);
            assertArrayEquals("copy with the cursor elsewhere", Arrays.copyOf(data, 50), bytesOf(direct, 50));
        } finally {
            direct.releaseLast();
        }

        writer.writePosition(position).write(data);                                // restore the fixture at position
        final File other = new File(OS.getTarget(), "mapped-read-across-dest-" + System.nanoTime() + ".dat");
        try (MappedBytes dest = MappedBytes.mappedBytes(other, chunk, chunk)) {
            dest.write(0, reader, position, data.length);                           // chunked destination
            assertArrayEquals("mapped destination", data, bytesOf(dest, data.length));
        } finally {
            BackgroundResourceReleaser.releasePendingResources();
            deleteIfPossible(other);
        }
    }

    @Test
    public void copyPastTheEndOfASourceStoreIsRejected() {
        // a plain Bytes source whose range exceeds its store used to be read past the allocation (pre-existing)
        final Bytes<?> source = Bytes.allocateDirect(64);
        final Bytes<?> dest = Bytes.allocateElasticDirect(128);
        try {
            source.writeSkip(64);
            assertThrows(BufferUnderflowException.class, () -> dest.write((BytesStore<?, ?>) source, 32L, 64L));
            assertThrows(BufferUnderflowException.class, () -> dest.write(0L, source, 32L, 64L));
        } finally {
            source.releaseLast();
            dest.releaseLast();
        }
    }

    private static byte[] bytesOf(Bytes<?> bytes, int length) {
        final byte[] out = new byte[length];
        bytes.readLimit(Math.max(bytes.readLimit(), length));
        bytes.read(0, out, 0, length);
        return out;
    }

    private static long readLongByteWise(Bytes<?> bytes, long offset) {
        long value = 0;
        for (int i = 0; i < Long.BYTES; i++)
            value = (value << 8) | bytes.readUnsignedByte(offset + i);
        return value;
    }

    private void assertMappingEndsInsideText(long textStart, int textLength) {
        final BytesStore<?, ?> mapping = reader.bytesStore();
        assertEquals("the reader maps one exact, zero-overlap chunk", chunk, mapping.realCapacity() - mapping.start());
        assertTrue(mapping.inside(textStart));
        assertFalse("the fixture must end past the reader's current mapping", mapping.inside(textStart, textLength));
    }

    private static String repeat(char c, int n) {
        return new String(new char[n]).replace('\0', c);
    }

    private static byte[] pattern(int length, int step, int offset) {
        final byte[] data = new byte[length];
        for (int i = 0; i < length; i++)
            data[i] = (byte) (i * step + offset);
        return data;
    }

    /** The reader sees everything written so far, from {@code readPosition}. */
    private void syncReader(long readPosition) {
        reader.readLimit(writer.writePosition());
        reader.readPosition(readPosition);
    }
}
