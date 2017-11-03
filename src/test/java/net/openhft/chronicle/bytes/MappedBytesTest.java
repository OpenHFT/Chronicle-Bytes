package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;

import static org.junit.Assert.*;

public class MappedBytesTest {

    @Test
    public void shouldNotBeReadOnly() throws Exception {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 1024);
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
}