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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class MappedBytesWrite8bitBoundaryTest extends BytesTestCommon {

    @Test
    public void write8bitAcrossChunkBoundary() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        // Use page size as chunk to make boundary deterministic
        final int chunk = OS.pageSize();
        File file = new File(OS.getTarget(), "mapped-write8bit-boundary-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        String msg = repeat('A', 32);
        try (MappedBytes mb = MappedBytes.mappedBytes(file, chunk)) {
            // position at end of first chunk minus a few bytes so the encoded length + data cross
            mb.writePosition(chunk - 2);
            mb.write8bit(msg);
            mb.readPosition(chunk - 2);
            String got = mb.read8bit();
            assertEquals(msg, got);
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private static String repeat(char c, int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) b[i] = (byte) c;
        return new String(b, StandardCharsets.ISO_8859_1);
    }
}

