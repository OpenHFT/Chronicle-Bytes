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
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;

import static org.junit.Assert.assertEquals;

public class HugeMappedBytesTest {

    @Ignore
    @Test
    public void testHugePage() throws Exception {
        File file = new File("/mnt/huge/tom/yevgen/testfile");
        file.createNewFile();
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        int hugePageSize = 2 << 20;
        randomAccessFile.setLength(hugePageSize);
        MappedBytes bytes = MappedBytes.mappedBytes(file, OS.pageSize(), OS.pageSize(), hugePageSize, false);
        bytes.writeVolatileInt(0, 1);
        int value = bytes.readVolatileInt(0);
        assertEquals(1, value);
    }
}
