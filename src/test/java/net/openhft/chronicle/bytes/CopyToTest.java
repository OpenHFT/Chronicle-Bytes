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

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class CopyToTest {

    @Test
    public void testCopyFromDirectBytesIntoByteBuffer() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytesToTest = Bytes.fromDirect("THIS IS A TEST STRING");
        ByteBuffer copyToDestination = ByteBuffer.allocateDirect(128);
        copyToDestination.limit((int) bytesToTest.readLimit());
        bytesToTest.copyTo(copyToDestination);
        assertEquals("THIS IS A TEST STRING", Bytes.wrapForRead(copyToDestination).toUtf8String());
    }

    @Test
    public void testCopyFromHeapBytesIntoByteBuffer() {
        Bytes<?> bytesToTest = Bytes.from("THIS IS A TEST STRING");
        ByteBuffer copyToDestination = ByteBuffer.allocate(128);
        copyToDestination.limit((int) bytesToTest.readLimit());
        bytesToTest.copyTo(copyToDestination);
        assertEquals("THIS IS A TEST STRING", Bytes.wrapForRead(copyToDestination).toUtf8String());
    }
}
