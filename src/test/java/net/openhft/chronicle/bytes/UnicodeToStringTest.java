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

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class UnicodeToStringTest {

    @Test
    public void testUtfStringInAndOut() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.elasticByteBuffer();
        bytes.appendUtf8("óaóó");
        assertEquals("óaóó", bytes.toUtf8String());
    }

    @Test
    public void testUtfStringInAndOutOnHeap() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        bytes.appendUtf8("óaóó");
        assertEquals("óaóó", bytes.toUtf8String());
    }
}
