/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

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

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

@SuppressWarnings("rawtypes")
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

    private final String text;

    {
        for (int i = 0; i < 200; i++) {
            largeTextBuilder.append(SMALL_TEXT);
        }

        text = largeTextBuilder.toString();
    }


    @Before
    @BeforeEach
    public void threadDump() {
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

            Assert.assertEquals(chunkSize * 2, bytesW.writePosition());

            bytesW.writeInt(7);
            Assert.assertEquals(chunkSize * 2 + 4, bytesW.writePosition());

            bytesW.writeInt(chunkSize * 2 - 2, 9);

            bytesW.readPosition(chunkSize * 2 - 2);
            Assert.assertEquals(9, bytesW.readInt());
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
            Assert.assertEquals(text.length(), bytesW.writePosition());
            Assert.assertEquals(rp, from.readPosition());

            // read
            bytesR.readLimit(wp);

            Assert.assertEquals(text, bytesR.toString());
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
            Assert.assertEquals(text.length(), bytesW.writePosition());

            // read
            bytesR.readLimit(wp);

            Assert.assertEquals(text, bytesR.toString());
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
            Assert.assertEquals(0, bytesW.writePosition());
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
            Assert.assertEquals(0, bytesW.writePosition());

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            Assert.assertEquals(text, bytesR.toString());
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
            Assert.assertEquals(0, bytesW.writePosition());

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            Assert.assertEquals(text, bytesR.toString());
            from.releaseLast();
        }
    }

    @Test
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
            Assert.assertEquals(0, bytesW.writePosition());

            // read
            bytesR.readLimit(offset + (text.length() - shift));
            bytesR.readPosition(offset);
            String actual = bytesR.toString();
            Assert.assertEquals(text.substring(shift), actual);
            from.releaseLast();
        }
    }

    @Test
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
            Assert.assertEquals(0, bytesW.writePosition());

            // read
            bytesR.readLimit(offset + (text.length() - shift));
            bytesR.readPosition(offset);
            String actual = bytesR.toString();
            Assert.assertEquals(text.substring(shift), actual);
            from.releaseLast();
        }
    }

    @Test
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
            fail();
        } catch (DecoratedBufferUnderflowException ex) {
            assertTrue(ex.getMessage().startsWith("Acquired the next BytesStore"));
        }
    }

    @Test
    public void testLargeWrites() throws IOException {
        testLargeWrites(128 << 10, 64 << 10, 500 << 10);
    }

    @Test
    public void testLargeWrites3() throws IOException {
        testLargeWrites(47 << 10, 21 << 10, 513 << 10);
    }

    @Test
    public void testLargeWrites2() throws IOException {
        testLargeWrites(128 << 10, 128 << 10, 128 << 10);
    }

    public void testLargeWrites(final long chunkSize,
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

        assertTrue(bytes.isClosed());
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
            assertFalse(mappedBytes.isBackingFileReadOnly());
            mappedBytes.writeUtf8(null); // used to blow up.
            assertNull(mappedBytes.readUtf8());
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
            raf.setLength(4096);
            assertTrue(tempFile.setWritable(false));
            checkShouldBeReadOnly(MappedBytes.readOnly(tempFile));
        }
    }

    private void checkShouldBeReadOnly(MappedBytes mappedBytes) {
        try {
            assertTrue(mappedBytes.isBackingFileReadOnly());
            mappedBytes.releaseLast();
            assertEquals(0, mappedBytes.refCount());
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
            assertTrue(tempFile.setWritable(false));
            assertThrows(FileNotFoundException.class, () -> MappedBytes.singleMappedBytes(tempFile, 64 << 10));
        }
    }

    @Test
    public void interrupted()
            throws FileNotFoundException {
        Thread.currentThread().interrupt();
        File file = IOTools.createTempFile("interrupted");
        file.deleteOnExit();
        try (MappedBytes mb = MappedBytes.mappedBytes(file, 64 << 10)) {
            mb.realCapacity();
            assertTrue(Thread.currentThread().isInterrupted());
        }
    }

    @Test
    public void interruptedSingle()
            throws FileNotFoundException {
        Thread.currentThread().interrupt();
        File file = IOTools.createTempFile("interrupted");
        try (MappedBytes mb = MappedBytes.singleMappedBytes(file, 64 << 10)) {
            mb.realCapacity();
            assertTrue(Thread.currentThread().isInterrupted());
        }
    }

    @After
    public void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    public void multiBytes()
            throws FileNotFoundException {
        File tmpfile = IOTools.createTempFile("data.dat");
        try (MappedFile mappedFile = MappedFile.mappedFile(tmpfile, 64 << 10);
             MappedBytes original = MappedBytes.mappedBytes(mappedFile)) {
            original.zeroOut(0, 1000);

            original.writeInt(0, 1234);

            PointerBytesStore pbs = new PointerBytesStore();
            pbs.set(original.addressForRead(50), 100);

            // Print out the int in the two BytesStores.
            // This shows that the copy has the same contents of the original.
//            System.out.println("Original(0): " + original.readInt(0));
//            System.out.println("PBS(0): " + pbs.readInt(0));

            // Now modify the copy and print out the new int in the two BytesStores again.
            pbs.writeInt(0, 4321);
//            System.out.println("Original(50): " + original.readInt(50));
//            System.out.println("PBS(0): " + pbs.readInt(0));
            original.writeInt(54, 12345678);
//            System.out.println("Original(54): " + original.readInt(54));
//            System.out.println("PBS(4): " + pbs.readInt(4));

            int pbsInt = pbs.readInt(4);
            int originalInt = original.readInt(50);

            assertEquals(12345678, pbsInt);
            assertEquals(4321, originalInt);
        }
    }

    @Test
    public void multiBytesSingle()
            throws FileNotFoundException {
        File tmpfile = IOTools.createTempFile("data.dat");
        try (MappedFile mappedFile = MappedFile.ofSingle(tmpfile, 64 << 10, false);
             MappedBytes original = MappedBytes.mappedBytes(mappedFile)) {
            original.zeroOut(0, 1000);

            original.writeInt(0, 1234);

            PointerBytesStore pbs = new PointerBytesStore();
            pbs.set(original.addressForRead(50), 100);

            // Print out the int in the two BytesStores.
            // This shows that the copy has the same contents of the original.
//            System.out.println("Original(0): " + original.readInt(0));
//            System.out.println("PBS(0): " + pbs.readInt(0));

            // Now modify the copy and print out the new int in the two BytesStores again.
            pbs.writeInt(0, 4321);
//            System.out.println("Original(50): " + original.readInt(50));
//            System.out.println("PBS(0): " + pbs.readInt(0));
            original.writeInt(54, 12345678);
//            System.out.println("Original(54): " + original.readInt(54));
//            System.out.println("PBS(4): " + pbs.readInt(4));

            assertEquals(12345678, original.readInt(54));
            assertEquals(4321, original.readInt(50));

        }
    }

    @Test
    public void memoryOverlapRegions()
            throws FileNotFoundException {
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
                assertEquals(offset < chunkSize ? 0 : chunkSize, mb.bytesStore().start());

                mb.equalBytes(csb, csb.length());
                assertEquals(chunkSize, mb.bytesStore().start());

                mb.equalBytes(csb, csb.length());
                assertEquals(chunkSize, mb.bytesStore().start());

                mb.parseUtf8(sb, csb.length());
                assertEquals(chunkSize, mb.bytesStore().start());
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
            assertEquals(count, mb.readVolatileLong(0));
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
                    ((Queue) tq).add(ioe);
                }
            });
            t.start();
            try (MappedBytes bytes = tq.take()) {
                try {
                    bytes.writeLong(1234);
                    fail();
                } catch (IllegalStateException expected) {
//                expected.printStackTrace();
                }
                bytes.singleThreadedCheckDisabled(true);
                bytes.writeLong(-1);
            }
        } finally {
            t.interrupt();
            t.join(Jvm.isDebug() ? 60_000 : 1000);
        }
    }

    @Test
    public void testEnsureCapacity() throws FileNotFoundException {
        File file = IOTools.createTempFile("ensure");
        final int chunkSize = 64 * PageUtil.getPageSize(file.getAbsolutePath());
        try (MappedBytes mb = MappedBytes.mappedBytes(file, chunkSize, chunkSize / 4)) {
            final int chunks3 = chunkSize * 3;
            mb.writePosition(chunks3).writeByte((byte) 0);
            assertEquals(chunks3, mb.bytesStore().start());
            mb.ensureCapacity(chunks3);
            assertEquals("ensureCapacity used to add writePosition", chunks3, mb.bytesStore().start());
        }
    }

    @Test(expected = DecoratedBufferOverflowException.class)
    public void testIncreaseCapacityOverMax() throws FileNotFoundException {
        File file = IOTools.createTempFile("ensure2");
        final int chunkSize = 256 << 10;
        try (MappedBytes mb = MappedBytes.mappedBytes(file, chunkSize, chunkSize / 4)) {
            final long capacity = mb.capacity();
            mb.ensureCapacity(capacity + 1);
        }
    }

    @Test
    public void testBoundaryUnderflow() throws FileNotFoundException {
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
            slice.releaseLast();
        }
    }
}
