package net.openhft.chronicle.bytes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.After;
import org.junit.Test;

import net.openhft.chronicle.core.OS;

public class MappedBytesTest {

    @Test
    public void shouldNotBeReadOnly() throws Exception {
        File tempFile = File.createTempFile("mapped", "bytes");
        tempFile.deleteOnExit();
        MappedBytes bytes = MappedBytes.mappedBytes(tempFile, 64 << 10);
        assertFalse(bytes.isBackingFileReadOnly());
        bytes.writeUtf8(null); // used to blow up.
        assertNull(bytes.readUtf8());
        bytes.release();
    }

    @Test
    public void shouldBeReadOnly() throws Exception {
        final File tempFile = File.createTempFile("mapped", "bytes");
        tempFile.deleteOnExit();
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            raf.setLength(4096);
            assertTrue(tempFile.setWritable(false));
            try (MappedBytes mappedBytes = MappedBytes.readOnly(tempFile)) {
                assertTrue(mappedBytes.isBackingFileReadOnly());
            }
        }

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

    @Test
    public void acquireByteStoreBeforeReadNumberValues() throws Exception {
        acquireByteStoreBeforeRead(Long.BYTES, (b, t) -> b.writeLong(t.longValue()), (b) -> b.readLong(), (value) -> Long.valueOf(value));
        acquireByteStoreBeforeRead(Integer.BYTES, (b, t) -> b.writeInt(t.intValue()), (b) -> b.readInt(),
                (value) -> Integer.valueOf(value));
        acquireByteStoreBeforeRead(Short.BYTES, (b, t) -> b.writeShort(t.shortValue()), (b) -> b.readShort(),
                (value) -> Short.valueOf(value.shortValue()));
        acquireByteStoreBeforeRead(Byte.BYTES, (b, t) -> b.writeByte(t.byteValue()), (b) -> b.readByte(),
                (value) -> Byte.valueOf(value.byteValue()));
        acquireByteStoreBeforeRead(Double.BYTES, (b, t) -> b.writeDouble(t.doubleValue()), (b) -> b.readDouble(),
                (value) -> Double.valueOf(value));
        acquireByteStoreBeforeRead(Float.BYTES, (b, t) -> b.writeFloat(t.floatValue()), (b) -> b.readFloat(),
                (value) -> Float.valueOf(value));
    }

    private <T extends Number> void acquireByteStoreBeforeRead(int sizeInBytes, BiConsumer<MappedBytes, T> writer,
            Function<MappedBytes, T> reader, Function<Integer, T> converter) throws Exception {
        int chunkSize = sizeInBytes * 8;
        MappedBytes bytes = null;
        try {
            File tmpFile = File.createTempFile("mapped", "bytes");
            tmpFile.deleteOnExit();
            bytes = MappedBytes.mappedBytes(tmpFile, chunkSize);
            for (int i = 0; i < ((chunkSize * 2) / sizeInBytes); i++) {
                writer.accept(bytes, converter.apply(i));
            }
            bytes.release();
            bytes = MappedBytes.mappedBytes(tmpFile, chunkSize);
            for (int i = 0; i < ((chunkSize * 2) / sizeInBytes); i++) {
                assertEquals(converter.apply(i), reader.apply(bytes));
            }
            bytes.release();

        } finally {
            if ((bytes != null) && !bytes.isClosed()) {
                bytes.release();
            }
        }
    }

    @After
    public void clearInterrupt() {
        Thread.interrupted();
    }
}