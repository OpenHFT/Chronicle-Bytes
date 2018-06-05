package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import static org.junit.Assert.*;

public class MappedBytesTest {
    @Ignore("https://github.com/OpenHFT/Chronicle-Bytes/issues/66")
    @Test
    public void testLargeWrites() throws IOException {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10, 64 << 10);

        byte[] largeBytes = new byte[300 << 10];
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
        bytes.write(64, Bytes.wrapForRead(largeBytes));
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

        bytes2.release();
        bytes.release();

    }

    @Test
    public void shouldNotBeReadOnly() throws Exception {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10);
        assertFalse(bytes.isBackingFileReadOnly());
        bytes.writeUtf8(null); // used to blow up.
        assertNull(bytes.readUtf8());
        bytes.release();
    }

    @Test
    public void shouldBeReadOnly() throws Exception {
        final File tempFile = File.createTempFile("mapped", "bytes");
        final RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        raf.setLength(4096);
        assertTrue(tempFile.setWritable(false));
        final MappedBytes mappedBytes = MappedBytes.readOnly(tempFile);

        assertTrue(mappedBytes.
                isBackingFileReadOnly());
        mappedBytes.release();
    }

    @Test(expected = IllegalStateException.class)
    public void interrupted() throws FileNotFoundException {
        Thread.currentThread().interrupt();
        File file = new File(OS.TARGET + "/interrupted-" + System.nanoTime());
        file.deleteOnExit();
        MappedBytes mb = MappedBytes.mappedBytes(file, 64 << 10);
        try {
            mb.realCapacity();
        } finally {
            mb.release();
        }
    }

    @After
    public void clearInterrupt() {
        Thread.interrupted();
    }
}