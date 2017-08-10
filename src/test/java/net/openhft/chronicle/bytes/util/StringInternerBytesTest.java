/*
 * Copyright 2016 higherfrequencytrading.com
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

package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 17/09/15.
 */
public class StringInternerBytesTest {

    @Test
    public void testIntern() {
        @NotNull StringInternerBytes si = new StringInternerBytes(128);
        for (int i = 0; i < 100; i++) {
            Bytes b = Bytes.from("key" + i);
            si.intern(b, (int) b.readRemaining());
        }
        assertEquals(82, si.valueCount());
    }
}