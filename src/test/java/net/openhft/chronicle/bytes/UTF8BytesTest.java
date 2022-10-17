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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class UTF8BytesTest extends BytesTestCommon {

    private static final String MESSAGE = "awésome-message-1";

    @Test
    public void testUtfEncoding()
            throws IOException {
        File f = File.createTempFile("testUtfEncoding", "data");
        f.deleteOnExit();
        final MappedBytes bytes = MappedBytes.mappedBytes(f, 256, 0);
        int len = (int) AppendableUtil.findUtf8Length(MESSAGE);
        bytes.appendUtf8(MESSAGE);

        StringBuilder sb = new StringBuilder();
        bytes.parseUtf8(sb, true, len);
        assertEquals(MESSAGE, sb.toString());
        bytes.releaseLast();
    }
}
