/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static org.junit.Assert.*;

@SuppressWarnings({"rawtypes"})
@RunWith(Parameterized.class)
public class Bytes3Test extends BytesTestCommon {

    private final Supplier<Bytes<?>> supplier;
    private final boolean forRead;

    public Bytes3Test(String testName, Supplier<Bytes<?>> supplier) {
        this.supplier = supplier;
        this.forRead = testName.contains("ForRead");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Bytes::elasticHeapByteBuffer", (Supplier<Bytes<?>>) Bytes::elasticHeapByteBuffer},
                {"Bytes.elasticByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260)},
                {"Bytes.elasticByteBuffer(260, 1025)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260, 1025)},
                {"elasticHeapByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticHeapByteBuffer(260)},
                {"Bytes::allocateElasticOnHeap", (Supplier<Bytes<?>>) Bytes::allocateElasticOnHeap},
                {"Bytes.wrapForRead(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(ByteBuffer.allocateDirect(260))},
                {"Bytes.wrapForWrite(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(ByteBuffer.allocateDirect(260))},
                {"Bytes.wrapForRead(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(new byte[1024])},
                {"Bytes.wrapForWrite(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(new byte[1024])}
        });
    }

    @Test
    public void readPositionAt0() {
        final Bytes<?> bytes = supplier.get();
        assertEquals(0L, bytes.readPosition());
    }

    @Test
    public void writePositionAt0() {
        if (forRead) return;
        final Bytes<?> bytes = supplier.get();
        assertEquals(0L, bytes.writePosition());
    }

    @Test
    public void isClear() {
        final Bytes<?> bytes = supplier.get();
        assertTrue(bytes.isClear());
    }

    @Test
    public void byteOrder() {
        final Bytes<?> bytes = supplier.get();
        assertEquals(ByteOrder.nativeOrder(), bytes.byteOrder());
    }

    @Test
    public void writeLimit() {
        final Bytes<?> bytes = supplier.get();
        assertTrue(bytes.writeLimit() >= 260);
    }

    @Test
    public void write() {
        if (forRead) return;
        final Bytes<?> bytes = supplier.get();

        assertEquals(0, bytes.writePosition());
        assertTrue(bytes.isClear());
        bytes.writeInt(42);
        assertEquals(42, bytes.readInt());
        assertFalse(bytes.isClear());
        bytes.clear();
        assertTrue(bytes.isClear());
        bytes.releaseLast();
    }

}