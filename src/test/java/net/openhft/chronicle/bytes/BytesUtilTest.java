package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/*
 * Created by Peter Lawrey on 17/05/2017.
 */
@SuppressWarnings("rawtypes")
public class BytesUtilTest {
    @Test
    public void fromFileInJar() throws IOException {
        Bytes bytes = BytesUtil.readFile("/net/openhft/chronicle/core/onoes/Google.properties");
        Bytes<?> apache_license = Bytes.from("Apache License");
        long n = bytes.indexOf(apache_license);
        assertTrue(n > 0);
        apache_license.release();
    }

    @Test
    public void findFile() throws FileNotFoundException {
        String file = BytesUtil.findFile("file-to-find.txt");
        assertTrue(new File(file).exists());
        assertTrue(new File(file).canWrite());
    }
}
