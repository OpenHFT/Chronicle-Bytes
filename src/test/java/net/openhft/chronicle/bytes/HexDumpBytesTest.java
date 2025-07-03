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

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class HexDumpBytesTest extends BytesTestCommon {

    @Test
    public void offsetFormat() {
        doTest(new HexDumpBytes());
    }

    private static void doTest(HexDumpBytes bytes) {
        bytes.numberWrap(8)
        .offsetFormat((o, b) -> b.appendBase16(o, 4));
        bytes.writeHexDumpDescription("hi").write(new byte[18]);
        bytes.adjustHexDumpIndentation(1);
        bytes.writeHexDumpDescription("nest").write(new byte[18]);
        assertEquals("" +
                "0000 00 00 00 00 00 00 00 00 # hi\n" +
                "0008 00 00 00 00 00 00 00 00\n" +
                "0010 00 00\n" +
                "0012    00 00 00 00 00 00 00 00 # nest\n" +
                "001a    00 00 00 00 00 00 00 00\n" +
                "0022    00 00\n", bytes.toHexString());
        bytes.releaseLast();
    }

    @Test
    public void memoryMapped() throws FileNotFoundException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        try (MappedBytes mappedBytes = MappedBytes.mappedBytes("test.dat", 64 * 1024)) {
            doTest(new HexDumpBytes(mappedBytes));
        }
    }
}
