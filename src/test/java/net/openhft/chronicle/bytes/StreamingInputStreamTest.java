/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.threads.ThreadDump;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
public class StreamingInputStreamTest extends BytesTestCommon {
    private ThreadDump threadDump;

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    // https://github.com/OpenHFT/Chronicle-Bytes/issues/48
    @Test
    public void readOfZeroShouldReturnZero() throws IOException {
        @NotNull Bytes b = Bytes.allocateElasticDirect();
        prepareBytes(b);

        @NotNull InputStream is = b.inputStream();
        assertEquals(0, is.read(new byte[5], 0, 0));
        b.releaseLast();
    }

    @Test(timeout = 1000)
    public void testReadBlock() throws IOException {

        @NotNull Bytes b = Bytes.allocateElasticDirect();
        @NotNull byte[] test = prepareBytes(b);

        @NotNull InputStream is = b.inputStream();
        try (@NotNull ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            @NotNull byte[] buffer = new byte[8];
            for (int len; (len = is.read(buffer)) != -1; )
                os.write(buffer, 0, len);
            os.flush();
            assertArrayEquals(test, os.toByteArray());
        }

        b.releaseLast();
    }

    private byte[] prepareBytes(final Bytes b) {
        @NotNull byte[] test = "Hello World, Have a great day!".getBytes(ISO_8859_1);
        b.write(test);
        return test;
    }
}
