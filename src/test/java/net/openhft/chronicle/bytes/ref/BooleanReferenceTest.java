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

package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class BooleanReferenceTest {
    @Test
    public void testBinary() {
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(1);
            byte i8 = (byte) 0xB0;
            nbs.writeByte(0, i8);
    
            ref.bytesStore(nbs, 0, 1);
    
            assertEquals(false, ref.getValue());
            ref.setValue(true);
    
            assertEquals(true, ref.getValue());
            assertEquals(1, ref.maxSize());
    
            nbs.release();
        }
    }

    @Test
    public void testText() {
        try (@NotNull TextBooleanReference ref = new TextBooleanReference()) {
            @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(5);
    
            nbs.write(0, "false".getBytes(StandardCharsets.ISO_8859_1));
    
            ref.bytesStore(nbs, 0, 5);
    
            assertEquals(false, ref.getValue());
            ref.setValue(true);
    
            assertEquals(true, ref.getValue());
            assertEquals(5, ref.maxSize());
    
            nbs.release();
        }
    }
}             