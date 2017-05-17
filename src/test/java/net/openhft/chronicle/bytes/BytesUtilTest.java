package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by peter on 17/05/2017.
 */
public class BytesUtilTest {
    @Test
    public void fromFileInJar() throws IOException {
        Bytes bytes = BytesUtil.readFile("/net/openhft/chronicle/core/onoes/Google.properties");
        long n = bytes.indexOf(Bytes.from("Apache License"));
        assertTrue(n > 0);
    }
}
