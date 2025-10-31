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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StopBitLengthTest extends BytesTestCommon {

    @Test
    public void boundaries() {
        assertEquals(1, BytesUtil.stopBitLength(0));
        assertEquals(1, BytesUtil.stopBitLength(0x7F));
        assertEquals(2, BytesUtil.stopBitLength(0x80));
        assertEquals(2, BytesUtil.stopBitLength(0x3FFF));
        assertTrue(BytesUtil.stopBitLength(0x4000) >= 3);
        assertTrue(BytesUtil.stopBitLength(Integer.MAX_VALUE) >= 3);
        assertTrue(BytesUtil.stopBitLength(Long.MAX_VALUE) >= 9);
    }
}

