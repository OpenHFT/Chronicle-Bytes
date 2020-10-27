package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@SuppressWarnings("rawtypes")
public class MappedBytesTest extends BytesTestCommon {

    final private String
            smallText = "It's ten years since the iPhone was first unveiled and Apple has marked " +
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
            largeTextBuilder.append(smallText);
        }

        text = largeTextBuilder.toString();
    }

    @Test
    public void testMappedFileSafeLimitTooSmall() throws IOException {

        final int arraySize = 40_000;

        byte[] data = new byte[arraySize];
        Arrays.fill(data, (byte) 'x');

        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 50_000, 40_000);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 50_000, 40_000)) {

            for (int i = 0; i < 5; i++) {
                bytesW.write(data);
            }

            for (int i = 0; i < 5; i++) {
                bytesR.write(data);
            }
        }
    }

    @Test
    public void testMappedFileSafeLimitTooSmall2() throws IOException {

        final int arraySize = 40_000;

        byte[] data = new byte[arraySize];
        Arrays.fill(data, (byte) 'x');

        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 50_000, 30_000);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 50_000, 30_000)) {

            for (int i = 0; i < 5; i++) {
                bytesW.write(data);
            }

            for (int i = 0; i < 5; i++) {
                bytesR.write(data);
            }
        }
    }

    @Test
    public void testWriteBytes() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10)) {

            // write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(from);
            long wp = bytesW.writePosition;
            Assert.assertEquals(text.length(), bytesW.writePosition);

            // read
            bytesR.readLimit(wp);

            Assert.assertEquals(text, bytesR.toString());
            from.releaseLast();
        }
    }

    @Test
    public void testWriteReadBytes() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {

            // write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(from);
            long wp = bytesW.writePosition;
            Assert.assertEquals(text.length(), bytesW.writePosition);

            // read
            bytesR.readLimit(wp);

            Assert.assertEquals(text, bytesR.toString());
            from.releaseLast();
        }
    }

    @Test
    public void testWriteBytesWithOffset() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10)) {

            int offset = 10;

            // write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(offset, from);
            long wp = text.length() + offset;
            Assert.assertEquals(0, bytesW.writePosition);

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            Assert.assertEquals(text, bytesR.toString());
            from.releaseLast();
        }
    }

    @Test
    public void testWriteReadBytesWithOffset() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {

            int offset = 10;

            // write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(offset, from);
            long wp = text.length() + offset;
            Assert.assertEquals(0, bytesW.writePosition);

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            Assert.assertEquals(text, bytesR.toString());
            from.releaseLast();
        }
    }

    @Test
    public void testWriteBytesWithOffsetAndTextShift() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10)) {
            int offset = 10;
            int shift = 128;

            //write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(offset, from, shift, text.length() - shift);
            Assert.assertEquals(0, bytesW.writePosition);

            // read
            bytesR.readLimit(offset + (text.length() - shift));
            bytesR.readPosition(offset);
            String actual = bytesR.toString();
            Assert.assertEquals(text.substring(shift), actual);
            from.releaseLast();
        }
    }

    @Test
    public void testWriteReadBytesWithOffsetAndTextShift() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10)) {
            int offset = 10;
            int shift = 128;

            //write
            Bytes<?> from = Bytes.from(text);
            bytesW.write(offset, from, shift, text.length() - shift);
            Assert.assertEquals(0, bytesW.writePosition);

            // read
            bytesR.readLimit(offset + (text.length() - shift));
            bytesR.readPosition(offset);
            String actual = bytesR.toString();
            Assert.assertEquals(text.substring(shift), actual);
            from.releaseLast();
        }
    }

    @Test
    public void testLargeWrites() throws IOException {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 128 <<
                10, 64 << 10);

        byte[] largeBytes = new byte[500 << 10];
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

        Bytes bytes2 = Bytes.allocateDirect(largeBytes);
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

    }

    @Test
    public void testLargeWrites3() throws IOException {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 47 <<
                10, 21 << 10);

        byte[] largeBytes = new byte[513 << 10];
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

        Bytes bytes2 = Bytes.allocateDirect(largeBytes);
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

    }

    @Test
    public void testLargeWrites2() throws IOException {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 128 <<
                10, 128 << 10);

        byte[] largeBytes = new byte[500 << 10];
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

        Bytes bytes2 = Bytes.allocateDirect(largeBytes);
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

    }

    @Test
    public void shouldNotBeReadOnly() throws Exception {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10);
        assertFalse(bytes.isBackingFileReadOnly());
        bytes.writeUtf8(null); // used to blow up.
        assertNull(bytes.readUtf8());
        bytes.releaseLast();
    }

    @Test
    public void shouldBeReadOnly() throws Exception {
        final File tempFile = File.createTempFile("mapped", "bytes");
        try (final RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            raf.setLength(4096);
            assertTrue(tempFile.setWritable(false));
            final MappedBytes mappedBytes = MappedBytes.readOnly(tempFile);

            assertTrue(mappedBytes.
                    isBackingFileReadOnly());
            mappedBytes.releaseLast();
            assertEquals(0, mappedBytes.refCount());
        }
    }

    @Test
    public void interrupted() throws FileNotFoundException {
        Thread.currentThread().interrupt();
        File file = IOTools.createTempFile("interrupted");
        file.deleteOnExit();
        MappedBytes mb = MappedBytes.mappedBytes(file, 64 << 10);
        try {
            mb.realCapacity();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            mb.releaseLast();
        }
    }

    @After
    public void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    public void multiBytes() throws FileNotFoundException {
        File tmpfile = IOTools.createTempFile("data.dat");
        try (MappedFile mappedFile = MappedFile.mappedFile(tmpfile, 64 << 10);
             MappedBytes original = MappedBytes.mappedBytes(mappedFile)) {
            original.zeroOut(0, 1000);

            original.writeInt(0, 1234);

            PointerBytesStore pbs = new PointerBytesStore();
            pbs.set(original.addressForRead(50), 100);

            // Print out the int in the two BytesStores.
            // This shows that the copy has the same contents of the original.
            System.out.println("Original(0): " + original.readInt(0));
            System.out.println("PBS(0): " + pbs.readInt(0));

            // Now modify the copy and print out the new int in the two BytesStores again.
            pbs.writeInt(0, 4321);
            System.out.println("Original(50): " + original.readInt(50));
            System.out.println("PBS(0): " + pbs.readInt(0));
            original.writeInt(54, 12345678);
            System.out.println("Original(54): " + original.readInt(54));
            System.out.println("PBS(4): " + pbs.readInt(4));
        }
    }

    @Test
    public void memoryOverlapRegions() throws FileNotFoundException {
        String tmpfile = IOTools.createTempFile("memoryOverlapRegions").getAbsolutePath();
        int chunkSize = 256 << 16;
        int overlapSize = 64 << 16;
        String longString = new String(new char[overlapSize * 2]);
        Bytes csb = Bytes.from(longString);
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
    public void threadSafeMappedBytes() throws FileNotFoundException {
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
}