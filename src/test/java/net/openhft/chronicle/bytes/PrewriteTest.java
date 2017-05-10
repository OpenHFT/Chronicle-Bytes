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

package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 20/12/16.
 */
public class PrewriteTest {
    @Test
    public void test() {
        Bytes bytes = Bytes.allocateDirect(64);
        bytes.clearAndPad(64);
        bytes.prepend(1234);
        bytes.prewrite(",hi,".getBytes());
        bytes.prewrite(Bytes.from("words"));
        bytes.prewriteByte((byte) ',');
        bytes.prewriteInt(0x34333231);
        bytes.prewriteLong(0x3837363534333231L);
        bytes.prewriteShort((short) 0x3130);
        assertEquals("01123456781234,words,hi,1234", bytes.toString());

        bytes.release();
    }
}
