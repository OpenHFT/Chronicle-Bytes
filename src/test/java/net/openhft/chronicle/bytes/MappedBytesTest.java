package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import static org.junit.Assert.*;

public class MappedBytesTest {

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