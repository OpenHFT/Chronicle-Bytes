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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BytesInternalIOTest extends BytesTestCommon {

    @Test
    public void copyInputStreamLarge() throws Exception {
        byte[] data = new byte[2000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i & 0x7F);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        Bytes<?> out = Bytes.allocateElasticOnHeap(128);
        try {
            BytesInternal.copy(bis, out);
            assertEquals(data.length, out.length());
        } finally {
            out.releaseLast();
        }
    }

    @Test
    public void copyRandomDataToOutputStreamAndToByteArrayWithOffset() throws Exception {
        Bytes<?> src = Bytes.allocateElasticOnHeap(64);
        try {
            src.append("0123456789");
            src.readPosition(2);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BytesInternal.copy(src, bos);
            assertArrayEquals("23456789".getBytes(), bos.toByteArray());

            byte[] arr = BytesInternal.toByteArray(src);
            assertArrayEquals("23456789".getBytes(), arr);
        } finally {
            src.releaseLast();
        }
    }
}

