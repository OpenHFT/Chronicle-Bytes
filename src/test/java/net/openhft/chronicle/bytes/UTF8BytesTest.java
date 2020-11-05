package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class UTF8BytesTest {

    private static final String MESSAGE = "awésome-message-1";

    @Test
    public void testUtfEncoding() throws IOException {
        File f = File.createTempFile("testUtfEncoding", "data");
        f.deleteOnExit();
        final MappedBytes bytes = MappedBytes.mappedBytes(f, 256);
        int len = (int) AppendableUtil.findUtf8Length(MESSAGE);
        bytes.appendUtf8(MESSAGE);

        StringBuilder sb = new StringBuilder();
        bytes.parseUtf8(sb, true, len);
        assertEquals(MESSAGE, sb.toString());
    }
}
