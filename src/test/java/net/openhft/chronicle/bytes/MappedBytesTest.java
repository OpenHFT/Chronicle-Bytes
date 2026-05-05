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
import org.junit.jupiter.api.DisplayName;
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
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import static net.openhft.chronicle.core.Jvm.uncheckedCast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests MappedBytes behaviours across mapping and threading scenarios
 * because correct memory-mapped file handling is essential to avoid
 * data corruption and resource leaks in production systems.
 * In order to prevent resource exhaustion, these tests verify cleanup.
 */
@SuppressWarnings({"rawtypes", "deprecation", "checkstyle:MMLacksPurpose", "checkstyle:MMOverusedWord", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("Mapped bytes behaviours across mapping and threading cases")
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

    private final String text = buildLargeText();

    private static String buildLargeText() {
        StringBuilder largeTextBuilder = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            largeTextBuilder.append(SMALL_TEXT);
        }
        return largeTextBuilder.toString();
    }

    @SuppressWarnings("EmptyMethod")
    @BeforeEach
    @Override
    public void threadDump() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for mapped bytes tests");

        super.threadDump();
    }

    @Test
    @DisplayName("mapped file safe limit rejects too small")
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
            fail("Mapped bytes safe limit write failed " + e.getMessage());
        }
    }

    @Test
    @DisplayName("acquire next byte store shifts backwards")
    public void testAcquireNextByteStoreShiftingBackwards() throws IOException {
        final long chunkSize = OS.mapAlign(40_000);

        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, chunkSize, chunkSize)) {

            for (int i = 0; i < chunkSize / 4; i++)
                bytesW.writeLong(ThreadLocalRandom.current().nextLong());

            assertEquals(chunkSize * 2, bytesW.writePosition(),
                    "Write position reaches end of second chunk");

            bytesW.writeInt(7);
            assertEquals(chunkSize * 2 + 4, bytesW.writePosition(),
                    "Write position advances after int write");

            bytesW.writeInt(chunkSize * 2 - 2, 9);

            bytesW.readPosition(chunkSize * 2 - 2);
            assertEquals(9, bytesW.readInt(),
                    "Read int returns expected value after reposition");
        }
    }

    @Test
    @DisplayName("mapped file safe limit rejects too small again")
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
            fail("Mapped bytes second safe limit write failed " + e.getMessage());
        }
    }

    @Test
    @DisplayName("write bytes advances positions and reads")
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
            assertEquals(text.length(), bytesW.writePosition(),
                    "Write position matches text length after write read test");
            assertEquals(rp, from.readPosition(),
                    "Source read position remains unchanged after write");

            // read
            bytesR.readLimit(wp);

            assertEquals(text, bytesR.toString(),
                    "Read bytes round trip original text for read test");
            from.releaseLast();
        }
    }

    @Test
    @DisplayName("write read bytes round trip content")
    public void testWriteReadBytes()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {

            // write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(from);
            long wp = bytesW.writePosition();
            assertEquals(text.length(), bytesW.writePosition(),
                    "Write position matches text length after write");

            // read
            bytesR.readLimit(wp);

            assertEquals(text, bytesR.toString(),
                    "Read bytes round trip original text");
            from.releaseLast();
        }
    }

    @Test
    @DisplayName("write read write skip bytes positions")
    public void testWriteReadWriteSkipBytes()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {

            String hello = "hello";
            Bytes<?> from = Bytes.from(hello);
            bytesW.write(from);
            bytesW.writeSkip(-hello.length());
            assertEquals(0, bytesW.writePosition(),
                    "Write position returns to zero after skip");
            assertThrows(BufferOverflowException.class,
                    () -> bytesW.writeSkip(-1),
                    "Negative skip should overflow past limit");

            from.releaseLast();
        }
    }

    @Test
    @DisplayName("write bytes with offset preserves positions")
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
            assertEquals(0, bytesW.writePosition(),
                    "Write position remains zero after offset write read test");

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            assertEquals(text, bytesR.toString(),
                    "Offset read returns expected text in read test");
            from.releaseLast();
        }
    }

    @Test
    @DisplayName("write read bytes with offset round trip")
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
            assertEquals(0, bytesW.writePosition(),
                    "Write position remains zero after offset write");

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            assertEquals(text, bytesR.toString(),
                    "Offset read returns expected text");
            from.releaseLast();
        }
    }

    @Test
    @DisplayName("write bytes with offset and text shift")
    public void testWriteBytesWithOffsetAndTextShift()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10)) {
            int offset = 10;
            int shift = 128;

            //write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(offset, from, shift, text.length() - shift);
            assertEquals(0, bytesW.writePosition(),
                    "Write position remains zero after shifted write read test");

            // read
            bytesR.readLimit(offset + (text.length() - shift));
            bytesR.readPosition(offset);
            String actual = bytesR.toString();
            assertEquals(text.substring(shift), actual,
                    "Shifted text read matches expected substring in read test");
            from.releaseLast();
        }
    }

    @Test
    @DisplayName("write read bytes with offset and text shift")
    public void testWriteReadBytesWithOffsetAndTextShift()
            throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {
            int offset = 10;
            int shift = 128;

            //write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(offset, from, shift, text.length() - shift);
            assertEquals(0, bytesW.writePosition(),
                    "Write position remains zero after shifted write");

            // read
            bytesR.readLimit(offset + (text.length() - shift));
            bytesR.readPosition(offset);
            String actual = bytesR.toString();
            assertEquals(text.substring(shift), actual,
                    "Shifted text read matches expected substring");
            from.releaseLast();
        }
    }

    @Test
    @DisplayName("write large 8bit text triggers underflow")
    public void testWriteLarge8Bit() throws IOException {
        File tempFile1 = Files.createTempFile("mapped", "bytes").toFile();
        try (MappedBytes bytes = MappedBytes.mappedBytes(tempFile1, 64 << 10)) {
            testWrite8Bit(bytes);
        }
    }

    private void testWrite8Bit(final MappedBytes bytes) {
        final Bytes<?> bytes2 = Bytes.from(text + text);
        try {
            bytes.write8bit(bytes2);
            fail("Writing 8bit beyond mapping should throw underflow");
        } catch (DecoratedBufferUnderflowException ex) {
            assertTrue(ex.getMessage().startsWith("Acquired the next BytesStore"),
                    "Underflow message starts with next bytes store");
        }
    }

    @Test
    @DisplayName("large writes preserve data across mapping")
    public void testLargeWrites() throws IOException {
        testLargeWrites(128 << 10, 64 << 10, 500 << 10);
    }

    @Test
    @DisplayName("large writes three chunks preserve data")
    public void testLargeWrites3() throws IOException {
        testLargeWrites(47 << 10, 21 << 10, 513 << 10);
    }

    @Test
    @DisplayName("large writes two chunks preserve data")
    public void testLargeWrites2() throws IOException {
        testLargeWrites(128 << 10, 128 << 10, 128 << 10);
    }

    private void testLargeWrites(final long chunkSize,
                                 final long overlapSize,
                                 final int arraySize)
            throws IOException {
        final MappedBytes bytes = MappedBytes
                .mappedBytes(File.createTempFile("mapped", "bytes"), chunkSize, overlapSize);

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

        Bytes<?> bytes2 = Bytes.allocateDirect(largeBytes);
        bytes.writePosition(0);
        bytes.write(bytes2);
        bytes.writePosition(0);
        bytes.write(64, bytes2);
        bytes.writePosition(0);
        bytes.write(bytes2, 64L, largeBytes.length - 64L);
        bytes.writePosition(0);
        bytes.write(64, bytes2, 64L, largeBytes.length - 64L);

        bytes2.releaseLast();
        bytes.releaseLast();

        assertTrue(bytes.isClosed(),
                "Mapped bytes are closed after large writes");
    }

    @Test
    @DisplayName("mapped bytes default mapping remains writable")
    public void shouldNotBeReadOnly()
            throws Exception {
        checkShouldNotBeReadOnly(MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10));
    }

    @Test
    @DisplayName("single mapped bytes default mapping remains writable")
    public void shouldNotBeReadOnlySingle()
            throws Exception {
        checkShouldNotBeReadOnly(MappedBytes.singleMappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10));
    }

    private void checkShouldNotBeReadOnly(MappedBytes mappedBytes) {
        try {
            assertFalse(mappedBytes.isBackingFileReadOnly(),
                    "Mapped bytes should not be read only");
            mappedBytes.writeUtf8(null); // used to blow up.
            assertNull(mappedBytes.readUtf8(),
                    "Null UTF8 read returns null string value");
        } finally {
            mappedBytes.close();
        }
    }

    @Test
    @DisplayName("read write file reports read only mapping")
    public void shouldBeReadOnlyFileReadWrite()
            throws Exception {
        checkShouldBeReadOnly(MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10, 32 << 10, true));
    }

    @Test
    @DisplayName("single read write file reports read only mapping")
    public void shouldNotBeReadOnlySingleFileReadWrite()
            throws Exception {
        assumeFalse(OS.isWindows(),
                "Windows does not support single read write mapping");
        checkShouldBeReadOnly(MappedBytes.singleMappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10, true));
    }

    @Test
    @DisplayName("read only mapping reports backing file read only")
    public void shouldBeReadOnly()
            throws Exception {
        final File tempFile = Files.createTempFile("mapped", "bytes").toFile();
        try (final RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            raf.setLength(PageUtil.getPageSize(tempFile.getAbsolutePath()));
            assertTrue(tempFile.setWritable(false),
                    "Temporary file is marked read only");
            checkShouldBeReadOnly(MappedBytes.readOnly(tempFile));
        }
    }

    private void checkShouldBeReadOnly(MappedBytes mappedBytes) {
        try {
            assertTrue(mappedBytes.isBackingFileReadOnly(),
                    "Mapped bytes should report read only backing");
            mappedBytes.releaseLast();
            assertEquals(0, mappedBytes.refCount(),
                    "Mapped bytes ref count drops after release");
        } finally {
            mappedBytes.close();
        }
    }

    @Test
    @DisplayName("read only file cannot open read write")
    public void cantOpenReadOnlyFileReadWrite()
            throws Exception {
        final File tempFile = Files.createTempFile("mapped", "bytes").toFile();
        try (final RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            raf.setLength(4096);
            assertTrue(tempFile.setWritable(false),
                    "Temporary file is set read only for open test");
            assertThrows(FileNotFoundException.class,
                    () -> MappedBytes.singleMappedBytes(tempFile, 64 << 10),
                    "Read only file should not open for read write");
        }
    }

    @Test
    @DisplayName("mapped bytes preserve interrupt status flag")
    public void interrupted() throws Exception {
        Thread.currentThread().interrupt();
        File file = IOTools.createTempFile("interrupted");
        file.deleteOnExit();
        try (MappedBytes mb = MappedBytes.mappedBytes(file, 64 << 10)) {
            mb.realCapacity();
            assertTrue(Thread.currentThread().isInterrupted(),
                    "Interrupt flag remains set after mapping");
        }
    }

    @Test
    @DisplayName("single mapped bytes preserve interrupt status")
    public void interruptedSingle() throws Exception {
        Thread.currentThread().interrupt();
        File file = IOTools.createTempFile("interrupted");
        try (MappedBytes mb = MappedBytes.singleMappedBytes(file, 64 << 10)) {
            mb.realCapacity();
            assertTrue(Thread.currentThread().isInterrupted(),
                    "Interrupt flag remains set after single mapping");
        }
    }

    @AfterEach
    public void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    @DisplayName("pointer bytes store shares mapped bytes data")
    public void multiBytes() throws Exception {
        File tmpfile = IOTools.createTempFile("data.dat");
        try (MappedFile mappedFile = MappedFile.mappedFile(tmpfile, 64 << 10);
             MappedBytes original = MappedBytes.mappedBytes(mappedFile)) {
            original.zeroOut(0, 1000);

            original.writeInt(0, 1234);

            PointerBytesStore pbs = new PointerBytesStore();
            pbs.set(original.addressForRead(50), 100);

            pbs.writeInt(0, 4321);
            original.writeInt(54, 12345678);

            int pbsInt = pbs.readInt(4);
            int originalInt = original.readInt(50);

            assertEquals(12345678, pbsInt,
                    "Pointer bytes read returns expected shared value");
            assertEquals(4321, originalInt,
                    "Mapped bytes read returns expected shared value");
        }
    }

    @Test
    @DisplayName("pointer bytes store shares single mapped data")
    public void multiBytesSingle() throws Exception {
        File tmpfile = IOTools.createTempFile("data.dat");
        try (MappedFile mappedFile = MappedFile.ofSingle(tmpfile, 64 << 10, false);
             MappedBytes original = MappedBytes.mappedBytes(mappedFile)) {
            original.zeroOut(0, 1000);

            original.writeInt(0, 1234);

            PointerBytesStore pbs = new PointerBytesStore();
            pbs.set(original.addressForRead(50), 100);

            pbs.writeInt(0, 4321);
            original.writeInt(54, 12345678);

            assertEquals(12345678, original.readInt(54),
                    "Single mapped bytes read returns expected value");
            assertEquals(4321, original.readInt(50),
                    "Pointer bytes value persists in mapped store");

        }
    }

    @Test
    @DisplayName("mapped bytes handle overlap regions correctly")
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
                assertEquals(offset < chunkSize ? 0 : chunkSize, mb.bytesStore().start(),
                        "Bytes store start matches expected for offset " + offset);

                mb.equalBytes(csb, csb.length());
                assertEquals(chunkSize, mb.bytesStore().start(),
                        "Bytes store start resets after first equalBytes at offset " + offset);

                mb.equalBytes(csb, csb.length());
                assertEquals(chunkSize, mb.bytesStore().start(),
                        "Bytes store start resets after second equalBytes at offset " + offset);

                mb.parseUtf8(sb, csb.length());
                assertEquals(chunkSize, mb.bytesStore().start(),
                        "Bytes store start resets after parseUtf8 at offset " + offset);
            }
        } finally {
            csb.releaseLast();
        }
        IOTools.deleteDirWithFiles(tmpfile, 2);
    }

    @Test
    @DisplayName("thread safe mapped bytes accumulate counts")
    public void threadSafeMappedBytes()
            throws FileNotFoundException {
        String tmpfile = IOTools.createTempFile("threadSafeMappedBytes").getAbsolutePath();
        int count = 4000;
        // Limit parallelism on WSL; query processor count for native concurrency testing
        int parallelism = isWsl()
                // WSL: limit parallelism to reduce resource contention
                ? Math.min(2, Runtime.getRuntime().availableProcessors())
                // Native: query processor count for maximum concurrency
                : Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        try {
            pool.submit(() -> IntStream.range(0, count)
                    .parallel()
                    .forEach(i -> {
                        try (MappedBytes mb = MappedBytes.mappedBytes(tmpfile, 256 << 10)) {
                            mb.addAndGetLong(0, 1);
                        } catch (FileNotFoundException e) {
                            // Rethrow as unchecked to propagate within parallel stream
                            throw Jvm.rethrow(e);
                        }
                    })).get();
        } catch (Exception e) {
            // Rethrow to propagate parallel execution failures
            throw Jvm.rethrow(e);
        } finally {
            pool.shutdown();
        }
        try (MappedBytes mb = MappedBytes.mappedBytes(tmpfile, 256 << 10)) {
            assertEquals(count, mb.readVolatileLong(0),
                    "Thread safe mapped bytes count matches expected total");
        }
        IOTools.deleteDirWithFiles(tmpfile, 2);
    }

    @Test
    @DisplayName("disabling thread safety allows cross thread writes")
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
                try {
                    bytes.writeLong(1234);
                    // Fail: write should throw before disabling thread safety
                    fail("Write should fail before disabling thread safety");
                } catch (IllegalStateException expected) {
                    assertNotNull(expected,
                            "IllegalStateException thrown for cross-thread write attempt");
                }
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
    @DisplayName("ensureCapacity advances start when write position grows")
    public void testEnsureCapacity() throws Exception {
        File file = IOTools.createTempFile("ensure");
        final int chunkSize = 64 * PageUtil.getPageSize(file.getAbsolutePath());
        try (MappedBytes mb = MappedBytes.mappedBytes(file, chunkSize, chunkSize / 4)) {
            final int chunks3 = chunkSize * 3;
            mb.writePosition(chunks3).writeByte((byte) 0);
            assertEquals(chunks3, mb.bytesStore().start(),
                    "Bytes store start moves to third chunk");
            mb.ensureCapacity(chunks3);
            assertEquals(chunks3, mb.bytesStore().start(),
                    "ensureCapacity preserves start at write position");
        }
    }

    @Test
    @DisplayName("ensureCapacity fails when exceeding maximum capacity")
    public void testIncreaseCapacityOverMax() throws Exception {
        File file = IOTools.createTempFile("ensure2");
        final int chunkSize = 256 << 10;
        try (MappedBytes mb = MappedBytes.mappedBytes(file, chunkSize, chunkSize / 4)) {
            final long capacity = mb.capacity();
            assertThrows(DecoratedBufferOverflowException.class,
                    () -> mb.ensureCapacity(capacity + 1),
                    "ensureCapacity should fail beyond maximum capacity");
        }
    }

    @Test
    @DisplayName("boundary underflow test completes without errors")
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
        assertTrue(true,
                "Boundary underflow test completes without exceptions"); // if we reach here, the test passes
    }
}
