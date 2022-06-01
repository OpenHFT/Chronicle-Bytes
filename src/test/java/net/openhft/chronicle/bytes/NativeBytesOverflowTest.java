/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.BytesStore.wrap;
import static org.junit.Assert.assertTrue;

public class NativeBytesOverflowTest extends BytesTestCommon {

    @Test(expected = BufferOverflowException.class)
    public void testExceedWriteLimitNativeWriteBytes() {
        BytesStore<?, ByteBuffer> store = wrap(ByteBuffer.allocate(128));
        Bytes<?> nb = new NativeBytes(store);
        try {
            nb.writeLimit(2).writePosition(0);
            nb.writeLong(10L);
        } finally {
            nb.releaseLast();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testExceedWriteLimitGuardedBytes() {
        Bytes<?> guardedNativeBytes = new GuardedNativeBytes(wrap(ByteBuffer.allocate(128)), 128);
        try {
            guardedNativeBytes.writeLimit(2).writePosition(0);
            guardedNativeBytes.writeLong(10L);
        } finally {
            guardedNativeBytes.releaseLast();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testElastic() {
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            bytes.writeLimit(2).writePosition(0);
            bytes.writeLong(10L);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testNativeWriteBytes2() {
        Bytes<?> nb = new NativeBytes(wrap(ByteBuffer.allocate(128))).unchecked(true);

        nb.writeLimit(2).writePosition(0);
        nb.writeLong(10L);

        // this is OK as we are unchecked !
        assertTrue(nb.writePosition() > nb.writeLimit());
    }
}
