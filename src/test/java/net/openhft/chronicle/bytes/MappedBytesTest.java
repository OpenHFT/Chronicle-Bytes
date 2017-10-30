package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MappedBytesTest {

    @Test
    public void shouldNotBeReadOnly() throws Exception {
        assertFalse(MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 1024).
                isBackingFileReadOnly());
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
    }
}