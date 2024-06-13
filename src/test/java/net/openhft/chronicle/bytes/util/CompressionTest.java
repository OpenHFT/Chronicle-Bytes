package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class CompressionTest {

    @Test
    public void testCompressWithUnsupportedAlgorithm() throws IllegalArgumentException {
        Bytes<?> uncompressed = mock(Bytes.class);
        Bytes<?> compressed = mock(Bytes.class);

        Compression.compress("unsupported_algo", uncompressed, compressed);
    }
}
