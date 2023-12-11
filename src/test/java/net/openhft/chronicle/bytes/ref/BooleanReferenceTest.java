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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class BooleanReferenceTest extends BytesTestCommon {
    @Test
    public void testBinary() {
        @NotNull BytesStore nbs = BytesStore.nativeStoreWithFixedCapacity(2);
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            // First value
            byte val1 = (byte) BinaryWireCode.FALSE;
            nbs.writeByte(0, val1);

            ref.bytesStore(nbs, 0, 1);

            assertFalse(ref.getValue());
            ref.setValue(true);

            // Second value
            byte val2 = (byte) BinaryWireCode.TRUE; // true
            nbs.writeByte(1, val2);

            ref.bytesStore(nbs, 1, 1);
            assertTrue(ref.getValue());
            assertEquals(1, ref.maxSize());

        }
        nbs.releaseLast();
    }

    @Test
    public void testText() {
        @NotNull BytesStore nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        try (@NotNull TextBooleanReference ref = new TextBooleanReference()) {

            // First value
            nbs.write(0, "false".getBytes(StandardCharsets.ISO_8859_1));

            ref.bytesStore(nbs, 0, 5);

            assertFalse(ref.getValue());
            ref.setValue(true);

            // Second value
            nbs.write(5, " true".getBytes(StandardCharsets.ISO_8859_1));

            ref.bytesStore(nbs, 5, 5);
            assertTrue(ref.getValue());
            assertEquals(5, ref.maxSize());

        }
        nbs.releaseLast();
    }
}
