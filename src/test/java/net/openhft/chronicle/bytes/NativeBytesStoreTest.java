/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
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

import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Created by peter.lawrey on 27/02/15.
 */
public class NativeBytesStoreTest {
    @Test
    public void testElasticByteBuffer() {
        Bytes<ByteBuffer> bbb = Bytes.elasticByteBuffer();
        assertEquals(1L << 40, bbb.capacity());
        assertEquals(OS.pageSize(), bbb.realCapacity());
        ByteBuffer bb = bbb.underlyingObject();
        assertNotNull(bb);

        for (int i = 0; i < 16; i++) {
            bbb.skip(1000);
            bbb.writeLong(12345);
        }
        assertEquals(OS.pageSize() * 4, bbb.realCapacity());
        ByteBuffer bb2 = bbb.underlyingObject();
        assertNotNull(bb2);
        assertNotSame(bb, bb2);
    }
}
