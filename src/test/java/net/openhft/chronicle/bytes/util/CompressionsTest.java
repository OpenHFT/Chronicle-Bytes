/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
}
