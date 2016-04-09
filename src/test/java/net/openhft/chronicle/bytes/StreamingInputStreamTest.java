package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertArrayEquals;

public class StreamingInputStreamTest {

    @Test(timeout = 1000)
    public void testReadBlock() throws IOException {

        Bytes b = Bytes.allocateElasticDirect();
        byte[] test = "Hello World, Have a great day!".getBytes();
        b.write(test);

        InputStream is = b.inputStream();
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8];
            for (int len; (len = is.read(buffer)) != -1; )
                os.write(buffer, 0, len);
            os.flush();
            assertArrayEquals(test, os.toByteArray());
        }

    }
}
