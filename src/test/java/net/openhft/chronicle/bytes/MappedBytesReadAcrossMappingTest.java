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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    /** The reader sees everything written so far, from {@code readPosition}. */
    private void syncReader(long readPosition) {
        reader.readLimit(writer.writePosition());
        reader.readPosition(readPosition);
    }
}
