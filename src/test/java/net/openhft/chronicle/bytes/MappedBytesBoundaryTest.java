/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assume.assumeFalse;

public class MappedBytesBoundaryTest extends BytesTestCommon {

    @Test
    public void writeAcrossChunkBoundary() throws FileNotFoundException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final int chunk = 4096;
        final byte[] prefix = new byte[chunk - 4];
        final byte[] tail = "HELLO".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] expected = new byte[prefix.length + tail.length];
        System.arraycopy(prefix, 0, expected, 0, prefix.length);
        System.arraycopy(tail, 0, expected, prefix.length, tail.length);

        File file = new File(OS.getTarget(), "mapped-boundary-" + System.nanoTime() + ".dat");
        try (MappedBytes mb = MappedBytes.mappedBytes(file, chunk)) {
            // position at end of first chunk minus 4
            mb.writePosition(prefix.length);
            mb.write(tail);

            // read back from start
            mb.readPosition(0);
            byte[] actual = new byte[expected.length];
            mb.read(actual);
            assertArrayEquals(expected, actual);
        }
    }
}

