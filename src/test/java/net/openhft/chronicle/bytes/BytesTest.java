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

import org.junit.Assert;
import org.junit.Test;

public class BytesTest {


    @Test
    public void testName() throws Exception {
        NativeBytesStore<Void> nativeStore = NativeBytesStore.nativeStoreWithFixedCapacity(30);
        Bytes<Void> bytes = nativeStore.bytes();

        long expected = 12345L;
        int offset = 5;

        bytes.writeLong(offset, expected);
        Assert.assertEquals(expected, bytes.readLong(offset));
    }


}