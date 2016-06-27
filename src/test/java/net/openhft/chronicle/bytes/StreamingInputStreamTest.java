package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.threads.ThreadDump;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertArrayEquals;

public class StreamingInputStreamTest {
    private ThreadDump threadDump;

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @Test(timeout = 1000)
    public void testReadBlock() throws IOException {

        Bytes b = Bytes.allocateElasticDirect();
        byte[] test = "Hello World, Have a great day!".getBytes(ISO_8859_1);
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
