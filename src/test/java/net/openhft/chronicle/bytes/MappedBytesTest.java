/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static net.openhft.chronicle.core.Jvm.uncheckedCast;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"rawtypes", "deprecation"})
public class MappedBytesTest extends BytesTestCommon {

    private static final String
            SMALL_TEXT = "It's ten years since the iPhone was first unveiled and Apple has marked " +
            "the occas" +
            "ion with a new iPhone that doesn't just jump one generation, it jumps several. " +
            "Apple has leapt straight from iPhone 7 (via the iPhone 8, reviewed here) all the way " +
            "to iPhone 10 (yes, that's how you are supposed to say it).\n" +
            "\n" +
            "Read on to find out how the new flagship iPhone X shapes up. Is it going to revolutionise " +
            "the mobile phone again like the original iPhone did, or is Apple now just playing catch-up " +
            "with the rest of the industry? (For a comparison with one rival device, see iPhone X vs LG G7.)\n";

    private final StringBuilder largeTextBuilder = new StringBuilder();

    private final String text = buildLargeText();

    private String buildLargeText() {
        for (int i = 0; i < 200; i++) {
            largeTextBuilder.append(SMALL_TEXT);
        }
        return largeTextBuilder.toString();
    }

    @SuppressWarnings("EmptyMethod")
    @BeforeEach
    @Override
    public void threadDump() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        super.threadDump();
    }

    @Test
    public void testMappedFileSafeLimitTooSmall()
            throws IOException {

        final int arraySize = 40_000;

        byte[] data = new byte[arraySize];
        Arrays.fill(data, (byte) 'x');

        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 50_000, 40_000);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 50_000, 40_000)) {

            for (int i = 0; i < 5; i++) {
                bytesW.write(data);
            }

            for (int i = 0; i < 5; i++) {
                bytesR.write(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testAcquireNextByteStoreShiftingBackwards() throws IOException {
        final long chunkSize = OS.mapAlign(40_000);

        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, chunkSize, chunkSize)) {

            for (int i = 0; i < chunkSize / 4; i++)
                bytesW.writeLong(ThreadLocalRandom.current().nextLong());

            assertEquals(chunkSize * 2, bytesW.writePosition(), "After writing longs across chunk boundary, expected write position at 2x chunk size");

            bytesW.writeInt(7);
            assertEquals(chunkSize * 2 + 4, bytesW.writePosition(), "After writing additional int, expected write position advanced by 4 bytes");

            bytesW.writeInt(chunkSize * 2 - 2, 9);

            bytesW.readPosition(chunkSize * 2 - 2);
            assertEquals(9, bytesW.readInt(), "Reading int at position written with writeInt(offset, 9) should return value 9");
        }
    }

    @Test
    public void testMappedFileSafeLimitTooSmall2()
            throws IOException {

        final int arraySize = 40_000;

        byte[] data = new byte[arraySize];
        Arrays.fill(data, (byte) 'x');

        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 50_000, 30_000);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 50_000, 30_000)) {

            for (int i = 0; i < 5; i++) {
                bytesW.write(data);
            }

            for (int i = 0; i < 5; i++) {
                bytesR.write(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testWriteBytes()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10)) {

            // write
            Bytes<?> from = Bytes.from(text);
            long rp = from.readPosition();
            bytesW.write(from);
            long wp = bytesW.writePosition();
            assertEquals(text.length(), bytesW.writePosition(), "After writing text content, write position should equal text length");
            assertEquals(rp, from.readPosition(), "Source bytes read position should remain unchanged after write operation");

            // read
            bytesR.readLimit(wp);

            assertEquals(text, bytesR.toString(), "Reading back written content should return original text unchanged");
            from.releaseLast();
        }
    }

    @Test
    public void testWriteReadBytes()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {

            // write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(from);
            long wp = bytesW.writePosition();
            assertEquals(text.length(), bytesW.writePosition(), "After writing large text, write position should match text length");

            // read
            bytesR.readLimit(wp);

            assertEquals(text, bytesR.toString(), "Separate reader should successfully read back complete written text");
            from.releaseLast();
        }
    }

    @Test
    public void testWriteReadWriteSkipBytes()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {

            String hello = "hello";
            Bytes<?> from = Bytes.from(hello);
            bytesW.write(from);
            bytesW.writeSkip(-hello.length());
            assertEquals(0, bytesW.writePosition(), "After skipping backwards by text length, write position should return to zero");
            assertThrows(BufferOverflowException.class, () -> bytesW.writeSkip(-1));

            from.releaseLast();
        }
    }

    @Test
    public void testWriteBytesWithOffset()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10)) {

            int offset = 10;

            // write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(offset, from);
            long wp = text.length() + offset;
            assertEquals(0, bytesW.writePosition(), "Writing with explicit offset should not modify write position");

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            assertEquals(text, bytesR.toString(), "Reading from offset should retrieve complete text written at that position");
            from.releaseLast();
        }
    }

    @Test
    public void testWriteReadBytesWithOffset()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {

            int offset = 10;

            // write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(offset, from);
            long wp = text.length() + offset;
            assertEquals(0, bytesW.writePosition(), "Position-based write with offset should leave write position at zero");

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            assertEquals(text, bytesR.toString(), "Reading from offset with separate reader should return written text");
            from.releaseLast();
        }
    }

    @Test
    public void testWriteBytesWithOffsetAndTextShift()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10)) {
            assertWriteBytesWithOffsetAndTextShift(bytesW, bytesR);
        }
    }

    @Test
    public void testWriteReadBytesWithOffsetAndTextShift()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {
            assertWriteBytesWithOffsetAndTextShift(bytesW, bytesR);
        }
    }

    private void assertWriteBytesWithOffsetAndTextShift(MappedBytes bytesW, MappedBytes bytesR) {
        int offset = 10;
        int shift = 128;

        Bytes<?> from = Bytes.from(text);
        try {
            bytesW.write(offset, from, shift, text.length() - shift);
            assertEquals(0, bytesW.writePosition(), "Writing substring with explicit offset should not affect write position");

            bytesR.readLimit(offset + (text.length() - shift));
            bytesR.readPosition(offset);
            String actual = bytesR.toString();
            assertEquals(text.substring(shift), actual, "Reading written substring should match expected text portion after shift");
        } finally {
            from.releaseLast();
        }
    }

    @Test
    public void testWriteLarge8Bit() throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytes = MappedBytes.mappedBytes(tempFile1, 64 << 10)) {
            String message = write8BitUnderflowMessage(bytes);
            assertTrue(message.startsWith("Acquired the next BytesStore"), "Writing large 8-bit content across chunk boundary should trigger BytesStore acquisition");
        }
    }

    private String write8BitUnderflowMessage(final MappedBytes bytes) {
        final Bytes<?> bytes2 = Bytes.from(text + text);
        try {
            try {
                bytes.write8bit(bytes2);
                throw new AssertionError("Expected DecoratedBufferUnderflowException");
            } catch (DecoratedBufferUnderflowException ex) {
                return ex.getMessage();
            }
        } finally {
            bytes2.releaseLast();
        }
    }

    @Test
    public void testLargeWrites() throws IOException {
        assertTrue(testLargeWrites(128 << 10, 64 << 10, 500 << 10), "MappedBytes with 128k chunks and 64k overlap should close cleanly after writing 500k arrays");
    }

    @Test
    public void testLargeWrites3() throws IOException {
        assertTrue(testLargeWrites(47 << 10, 21 << 10, 513 << 10), "MappedBytes with 47k chunks and 21k overlap should close cleanly after writing 513k arrays");
    }

    @Test
    public void testLargeWrites2() throws IOException {
        assertTrue(testLargeWrites(128 << 10, 128 << 10, 128 << 10), "MappedBytes with equal 128k chunk and overlap sizes should close cleanly after writing 128k arrays");
    }

    private boolean testLargeWrites(final long chunkSize,
                                    final long overlapSize,
                                    final int arraySize)
            throws IOException {
        final MappedBytes bytes = MappedBytes
                .mappedBytes(File.createTempFile("mapped", "bytes"), chunkSize, overlapSize);

        Bytes<?> bytes2 = null;
        try {
            final byte[] largeBytes = new byte[arraySize];
            bytes.writePosition(0);
            bytes.write(largeBytes);
            bytes.writePosition(0);
            bytes.write(64, largeBytes);
            bytes.writePosition(0);
            bytes.write(largeBytes, 64, largeBytes.length - 64);
            bytes.writePosition(0);
            bytes.write(64, largeBytes, 64, largeBytes.length - 64);

            bytes.writePosition(0);
            bytes.write(Bytes.wrapForRead(largeBytes));
            bytes.writePosition(0);
            Bytes<byte[]> bytes1 = Bytes.wrapForRead(largeBytes);
            bytes.write(64, bytes1);
            bytes.writePosition(0);
            bytes.write(Bytes.wrapForRead(largeBytes), 64L, largeBytes.length - 64L);
            bytes.writePosition(0);
            bytes.write(64, Bytes.wrapForRead(largeBytes), 64L, largeBytes.length - 64L);

            bytes2 = Bytes.allocateDirect(largeBytes);
            bytes.writePosition(0);
            bytes.write(bytes2);
            bytes.writePosition(0);
            bytes.write(64, bytes2);
            bytes.writePosition(0);
            bytes.write(bytes2, 64L, largeBytes.length - 64L);
            bytes.writePosition(0);
            bytes.write(64, bytes2, 64L, largeBytes.length - 64L);
        } finally {
            if (bytes2 != null)
                bytes2.releaseLast();
            bytes.releaseLast();
        }
        return bytes.isClosed();
    }

    @Test
    public void shouldNotBeReadOnly()
            throws Exception {
        checkShouldNotBeReadOnly(MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10));
    }

    @Test
    public void shouldNotBeReadOnlySingle()
            throws Exception {
        checkShouldNotBeReadOnly(MappedBytes.singleMappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10));
    }

    private void checkShouldNotBeReadOnly(MappedBytes mappedBytes) {
        try {
            assertFalse(mappedBytes.isBackingFileReadOnly(), "MappedBytes created without read-only flag should allow write operations");
            mappedBytes.writeUtf8(null); // used to blow up.
            assertNull(mappedBytes.readUtf8(), "Reading after writing null UTF-8 should return null value");
        } finally {
            mappedBytes.close();
        }
    }

    @Test
    public void shouldBeReadOnlyFileReadWrite()
            throws Exception {
        checkShouldBeReadOnly(MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10, 32 << 10, true));
    }

    @Test
    public void shouldNotBeReadOnlySingleFileReadWrite()
            throws Exception {
        assumeFalse(OS.isWindows());
        checkShouldBeReadOnly(MappedBytes.singleMappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10, true));
    }

    @Test
    public void shouldBeReadOnly()
            throws Exception {
        final File tempFile = Files.createTempFile("mapped", "bytes").toFile();
        try (final RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            raf.setLength(PageUtil.getPageSize(tempFile.getAbsolutePath()));
            assertTrue(tempFile.setWritable(false), "Setting file to non-writable should succeed for read-only test");
            checkShouldBeReadOnly(MappedBytes.readOnly(tempFile));
        }
    }

    private void checkShouldBeReadOnly(MappedBytes mappedBytes) {
        try {
            assertTrue(mappedBytes.isBackingFileReadOnly(), "MappedBytes opened with read-only flag should report backing file as read-only");
            mappedBytes.releaseLast();
            assertEquals(0, mappedBytes.refCount(), "After releasing last reference, reference count should be zero");
        } finally {
            mappedBytes.close();
        }
    }

    @Test
    public void cantOpenReadOnlyFileReadWrite()
            throws Exception {
        final File tempFile = Files.createTempFile("mapped", "bytes").toFile();
        try (final RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            raf.setLength(4096);
            assertTrue(tempFile.setWritable(false), "Setting file to read-only should succeed before attempting read-write open");
            assertThrows(FileNotFoundException.class, () -> MappedBytes.singleMappedBytes(tempFile, 64 << 10));
        }
    }

    @Test
    public void interrupted() throws Exception {
        Thread.currentThread().interrupt();
        File file = IOTools.createTempFile("interrupted");
        file.deleteOnExit();
        try (MappedBytes mb = MappedBytes.mappedBytes(file, 64 << 10)) {
            mb.realCapacity();
            assertTrue(Thread.currentThread().isInterrupted(), "Interrupted status should be preserved after MappedBytes operations");
        }
    }

    @Test
    public void interruptedSingle() throws Exception {
        Thread.currentThread().interrupt();
        File file = IOTools.createTempFile("interrupted");
        try (MappedBytes mb = MappedBytes.singleMappedBytes(file, 64 << 10)) {
            mb.realCapacity();
            assertTrue(Thread.currentThread().isInterrupted(), "Interrupted status should persist through singleMappedBytes operations");
        }
    }

    @AfterEach
    public void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    public void multiBytes() throws Exception {
        File tmpfile = IOTools.createTempFile("data.dat");
        try (MappedFile mappedFile = MappedFile.mappedFile(tmpfile, 64 << 10);
             MappedBytes original = MappedBytes.mappedBytes(mappedFile)) {
            original.zeroOut(0, 1000);

            original.writeInt(0, 1234);

            PointerBytesStore pbs = new PointerBytesStore();
            pbs.set(original.addressForRead(50), 100);

            // Print out the int in the two BytesStores.
            // This shows that the copy has the same contents of the original.

            // Now modify the copy and print out the new int in the two BytesStores again.
            pbs.writeInt(0, 4321);
            original.writeInt(54, 12345678);

            int pbsInt = pbs.readInt(4);
            int originalInt = original.readInt(50);

            assertEquals(12345678, pbsInt, "PointerBytesStore reading at offset 4 should reflect value written to original at offset 54");
            assertEquals(4321, originalInt, "Original bytes reading at offset 50 should reflect value written to PointerBytesStore at offset 0");
        }
    }

    @Test
    public void multiBytesSingle() throws Exception {
        File tmpfile = IOTools.createTempFile("data.dat");
        try (MappedFile mappedFile = MappedFile.ofSingle(tmpfile, 64 << 10, false);
             MappedBytes original = MappedBytes.mappedBytes(mappedFile)) {
            original.zeroOut(0, 1000);

            original.writeInt(0, 1234);

            PointerBytesStore pbs = new PointerBytesStore();
            pbs.set(original.addressForRead(50), 100);

            // Print out the int in the two BytesStores.
            // This shows that the copy has the same contents of the original.

            // Now modify the copy and print out the new int in the two BytesStores again.
            pbs.writeInt(0, 4321);
            original.writeInt(54, 12345678);

            assertEquals(12345678, original.readInt(54), "Original bytes should read back value 12345678 written at offset 54");
            assertEquals(4321, original.readInt(50), "Original bytes at offset 50 should contain value 4321 written via PointerBytesStore");

        }
    }

    @Test
    public void zeroOutRespectsCustomPageSize() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final File file = newTempBinary("zero-custom-page");
        final int customPageSize = Math.max(OS.pageSize(), 4096) * 2;
        final long chunkSize = customPageSize * 2L;
        final long range = customPageSize + 256L;

        try (MappedBytes bytes = MappedBytes.mappedBytes(file, chunkSize, 0, customPageSize, false)) {
            for (long offset = 0; offset < range; offset++) {
                bytes.writeByte(offset, (byte) 0x5A);
            }

            bytes.zeroOut(0, range);

            for (long offset = 0; offset < range; offset++) {
                assertEquals(0, bytes.readUnsignedByte(offset), "After zeroOut operation, byte at offset " + offset + " should be cleared to zero");
            }
        } finally {
            assertTrue(file.delete(), "Temporary test file " + file + " should be deleted successfully");
        }
    }

    @Test
    public void memoryOverlapRegions() throws Exception {
        String tmpfile = IOTools.createTempFile("memoryOverlapRegions").getAbsolutePath();
        int chunkSize = 256 << 16;
        int overlapSize = 64 << 16;
        String longString = new String(new char[overlapSize * 2]);
        Bytes<?> csb = Bytes.from(longString);
        try (MappedBytes mb = MappedBytes.mappedBytes(new File(tmpfile), chunkSize, overlapSize)) {
            StringBuilder sb = new StringBuilder();
            for (int offset : new int[]{chunkSize - OS.pageSize(), chunkSize + overlapSize - OS.pageSize()}) {
                mb.writePosition(offset);
                mb.appendUtf8(longString);
                mb.readPosition(offset);
                assertEquals(offset < chunkSize ? 0 : chunkSize, mb.bytesStore().start(), "BytesStore start should be 0 for first chunk, chunkSize for second chunk");

                mb.equalBytes(csb, csb.length());
                assertEquals(chunkSize, mb.bytesStore().start(), "After first equalBytes across boundary, BytesStore should advance to start of chunk 2");

                mb.equalBytes(csb, csb.length());
                assertEquals(chunkSize, mb.bytesStore().start(), "After second equalBytes, BytesStore should remain at start of chunk 2");

                mb.parseUtf8(sb, csb.length());
                assertEquals(chunkSize, mb.bytesStore().start(), "After parseUtf8, BytesStore should still be positioned at start of chunk 2");
            }
        } finally {
            csb.releaseLast();
        }
        IOTools.deleteDirWithFiles(tmpfile, 2);
    }

    @Test
    public void threadSafeMappedBytes()
            throws FileNotFoundException {
        String tmpfile = IOTools.createTempFile("threadSafeMappedBytes").getAbsolutePath();
        int count = 4000;
        IntStream.range(0, count)
                .parallel()
                .forEach(i -> {
                    try (MappedBytes mb = MappedBytes.mappedBytes(tmpfile, 256 << 10)) {
                        mb.addAndGetLong(0, 1);
                    } catch (FileNotFoundException e) {
                        throw Jvm.rethrow(e);
                    }
                });
        try (MappedBytes mb = MappedBytes.mappedBytes(tmpfile, 256 << 10)) {
            assertEquals(count, mb.readVolatileLong(0), "After 4000 parallel increments, volatile long at position 0 should equal 4000");
        }
        IOTools.deleteDirWithFiles(tmpfile, 2);
    }

    @Test
    public void disableThreadSafety() throws InterruptedException {
        Thread t = null;
        try {
            BlockingQueue<MappedBytes> tq = new LinkedBlockingQueue<>();
            t = new Thread(() -> {
                try {
                    MappedBytes bytes = MappedBytes.mappedBytes(IOTools.createTempFile("disableThreadSafety"), 64 << 10);
                    bytes.writeLong(128);
                    tq.add(bytes);
                    Jvm.pause(1000);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    // cause the caller to fail.
                    Queue<IOException> q = uncheckedCast(tq);
                    q.add(ioe);
                }
            });
            t.start();
            try (MappedBytes bytes = tq.take()) {
                assertThrows(IllegalStateException.class, () -> bytes.writeLong(1234));
                bytes.singleThreadedCheckDisabled(true);
                bytes.writeLong(-1);
            }
        } finally {
            if (t != null) {
                t.interrupt();
                t.join(Jvm.isDebug() ? 60_000 : 1000);
            }
        }
    }

    @Test
    public void testEnsureCapacity() throws Exception {
        File file = IOTools.createTempFile("ensure");
        final int chunkSize = 64 * PageUtil.getPageSize(file.getAbsolutePath());
        try (MappedBytes mb = MappedBytes.mappedBytes(file, chunkSize, chunkSize / 4)) {
            final int chunks3 = chunkSize * 3;
            mb.writePosition(chunks3).writeByte((byte) 0);
            assertEquals(chunks3, mb.bytesStore().start(), "After writing at 3x chunk size, BytesStore should start at that position");
            mb.ensureCapacity(chunks3);
            assertEquals(chunks3, mb.bytesStore().start(), "ensureCapacity should not change BytesStore start position when capacity already sufficient");
        }
    }

    @Test
    public void testIncreaseCapacityOverMax() throws Exception {
        assertThrows(DecoratedBufferOverflowException.class, () -> {
            File file = IOTools.createTempFile("ensure2");
            final int chunkSize = 256 << 10;
            try (MappedBytes mb = MappedBytes.mappedBytes(file, chunkSize, chunkSize / 4)) {
                final long capacity = mb.capacity();
                mb.ensureCapacity(capacity + 1);
            }
        });
    }

    @Test
    public void testBoundaryUnderflow() throws Exception {
        File file = IOTools.createTempFile("boundary-underflow");

        Bytes slice = null;
        try (MappedBytes mf = MappedBytes.mappedBytes(file, 256L * OS.pageSize(), OS.pageSize())) {
            slice = mf.bytesForWrite();

            mf.writePosition(0);
            mf.readPositionRemaining(0, 0);
            slice.writeLimit(slice.capacity());

            Random rnd = new Random(123456L);
            for (int i = 0; i < 320000; i++) {
                int size = 10 + rnd.nextInt(100);
                byte[] msg = new byte[size];
                rnd.nextBytes(msg);

                long start = mf.readLimit() + Short.BYTES;
                long wLim = mf.writeLimit();

                slice.writeLimit(wLim);
                slice.writePosition(start);
                slice.readPosition(start);
                slice.write(msg);

                short msgSize = (short) slice.readRemaining();
                mf.writeShort(msgSize);
                mf.writeSkip(msgSize);
            }
        } finally {
            if (slice != null) {
                slice.releaseLast();
            }
        }
        assertTrue(true, "Writing 320000 variable-length messages across chunk boundaries should complete without BufferUnderflowException"); // if we reach here, the test passes
    }

    private static File newTempBinary(String prefix) throws IOException {
        File target = new File(OS.getTarget());
        if (!target.exists() && !target.mkdirs() && !target.isDirectory()) {
            throw new IOException("Unable to create target directory " + target);
        }
        File file = File.createTempFile(prefix, ".dat", target);
        file.deleteOnExit();
        return file;
    }
}
