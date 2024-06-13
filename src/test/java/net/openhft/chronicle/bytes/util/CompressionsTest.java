package net.openhft.chronicle.bytes.util;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CompressionsTest {

    @Test
    void testBinaryCompression() {
        byte[] original = "test data".getBytes();

        byte[] compressed = Compressions.Binary.compress(original);
        byte[] decompressed = Compressions.Binary.uncompress(compressed);
        assertEquals(new String(original), new String(decompressed));

        InputStream decompressingStream = Compressions.Binary.decompressingStream(new ByteArrayInputStream(compressed));
        OutputStream compressingStream = Compressions.Binary.compressingStream(new ByteArrayOutputStream());
        assertNotNull(decompressingStream);
        assertNotNull(compressingStream);
    }

    @Test
    void testLZWCompression() throws Exception {
        byte[] original = "test data".getBytes();
        ByteArrayOutputStream compressedOut = new ByteArrayOutputStream();
        OutputStream compressingStream = Compressions.LZW.compressingStream(compressedOut);
        compressingStream.write(original);
        compressingStream.close();

        byte[] compressed = compressedOut.toByteArray();
        InputStream decompressingStream = Compressions.LZW.decompressingStream(new ByteArrayInputStream(compressed));
        ByteArrayOutputStream decompressedOut = new ByteArrayOutputStream();
        decompressingStream.transferTo(decompressedOut);
        byte[] decompressed = decompressedOut.toByteArray();

        // Assuming LZW compression doesn't modify the input for this short string
        assertEquals(new String(original), new String(decompressed));
    }

    @Test
    void testGZIPCompression() throws Exception {
        byte[] original = "test data".getBytes();
        ByteArrayOutputStream compressedOut = new ByteArrayOutputStream();
        OutputStream compressingStream = Compressions.GZIP.compressingStream(compressedOut);
        compressingStream.write(original);
        compressingStream.close();

        byte[] compressed = compressedOut.toByteArray();
        InputStream decompressingStream = Compressions.GZIP.decompressingStream(new ByteArrayInputStream(compressed));
        ByteArrayOutputStream decompressedOut = new ByteArrayOutputStream();
        decompressingStream.transferTo(decompressedOut);
        byte[] decompressed = decompressedOut.toByteArray();

        assertEquals(new String(original), new String(decompressed));
    }
}
