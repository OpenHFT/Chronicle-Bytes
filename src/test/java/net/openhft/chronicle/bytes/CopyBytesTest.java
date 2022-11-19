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

import net.openhft.chronicle.core.io.Closeable;
import org.junit.Test;

import java.io.File;
import java.nio.BufferOverflowException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class CopyBytesTest extends BytesTestCommon {

    static void doTest(Bytes<?> toTest, int from) {
        Bytes<?> toCopy = Bytes.allocateDirect(32);
        Bytes<?> toValidate = Bytes.allocateDirect(32);
        try {
            toCopy.writeLong(0, (long) 'W' << 56L | 100L);
            toCopy.writeLong(8, (long) 'W' << 56L | 200L);

            toTest.writePosition(from);
            toTest.write(toCopy, 0, 2 * 8L);
            toTest.write(toCopy, 0, 8L);

            toTest.readPosition(from);
            toTest.read(toValidate, 3 * 8);

            assertEquals((long) 'W' << 56L | 100L, toValidate.readLong(0));
            assertEquals((long) 'W' << 56L | 200L, toValidate.readLong(8));
            assertEquals((long) 'W' << 56L | 100L, toValidate.readLong(16));

        } finally {
            toTest.releaseLast();
            toCopy.releaseLast();
            toValidate.releaseLast();
            // close if closeable.
            Closeable.closeQuietly(toTest);
        }
    }

    @Test
    public void testCanCopyBytesFromBytes() {
        doTest(Bytes.allocateElasticDirect(), 0);
    }

    @Test
    public void testCanCopyBytesFromMappedBytes1()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.mappedBytes(bytes, 64 << 10, 0), 0);
    }

    @Test
    public void testCanCopyBytesFromMappedBytesSingle1()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.singleMappedBytes(bytes, 64 << 10), 0);
    }

    @Test
    public void testCanCopyBytesFromMappedBytes2()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.mappedBytes(bytes, 64 << 10, 0), (64 << 10) - 8);
    }

    @Test
    public void testCanCopyBytesFromMappedBytesSingle2()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.singleMappedBytes(bytes, 128 << 10), (64 << 10) - 8);
    }

    @Test
    public void testCanCopyBytesFromMappedBytes3()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.mappedBytes(bytes, 16 << 10, 16 << 10), (64 << 10) - 8);
    }

    @Test(expected = BufferOverflowException.class)
    public void testCanCopyBytesFromMappedBytesSingle3()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.singleMappedBytes(bytes, 32 << 10), (64 << 10) - 8);
    }
}
