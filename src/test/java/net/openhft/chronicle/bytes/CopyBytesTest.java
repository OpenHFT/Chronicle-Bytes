/*
 * Copyright 2014-2018 Chronicle Software
 *
 * http://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class CopyBytesTest {

    static void doTest(Bytes toTest, int from) {
        Bytes toCopy = Bytes.allocateDirect(32);
        Bytes toValidate = Bytes.allocateDirect(32);

        toCopy.writeLong(0, (long) 'W' << 56L | 100L);
        toCopy.writeLong(8, (long) 'W' << 56L | 200L);

        toTest.writePosition(from);
        toTest.write(toCopy, 0, 2 * 8L);
        toTest.write(toCopy, 0, 8L);

        toTest.read(toValidate, 3 * 8);

        assertEquals((long) 'W' << 56L | 100L, toValidate.readLong(0));
        assertEquals((long) 'W' << 56L | 200L, toValidate.readLong(8));
        assertEquals((long) 'W' << 56L | 100L, toValidate.readLong(16));

        toTest.release();
        toCopy.release();
        toValidate.release();
    }

    @Test
    public void testCanCopyBytesFromBytes() {
        doTest(Bytes.allocateElasticDirect(), 0);
    }

    @Test
    public void testCanCopyBytesFromMappedBytes() throws Exception {
        File bytes = File.createTempFile("mapped-test", "bytes");
        bytes.deleteOnExit();
        doTest(MappedBytes.mappedBytes(bytes, 64 << 10, 0), 0);
        doTest(MappedBytes.mappedBytes(bytes, 64 << 10, 0), (64 << 10) - 8);
    }
}
