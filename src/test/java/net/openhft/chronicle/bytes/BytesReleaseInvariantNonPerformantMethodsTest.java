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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.EmbeddedBytes;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceCounted;
import net.openhft.chronicle.core.util.Histogram;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.*;
import static net.openhft.chronicle.core.io.ReferenceOwner.INIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests if certain non-performant methods works as expected when called on a released Bytes object
 */
final class BytesReleaseInvariantNonPerformantMethodsTest extends BytesTestCommon {

    private static final String SILLY_NAME = "Tryggve";

    private static Stream<NamedConsumer<Bytes<Object>>> provideNonPerformantOperations() {
        final OutputStream os = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new UnsupportedEncodingException();
            }
        };
        final BytesStore<?, ?> bs = BytesStore.from(SILLY_NAME);
        final Bytes<?> bytes = Bytes.from(SILLY_NAME);
        return Stream.of(
                NamedConsumer.of(BytesStore::to8bitString, "to8bitString()"),
                NamedConsumer.of(RandomDataInput::toByteArray, "toByteArray()"),
                NamedConsumer.of(RandomDataInput::toTemporaryDirectByteBuffer, "toTemporaryDirectByteBuffer()"),
                NamedConsumer.of(StreamingDataInput::readUtf8, "readUtf8()"),
                NamedConsumer.of(b -> b.readUtf8Limited(1, 1), "readUtf8Limited(1, 1)"),
                NamedConsumer.of(b -> b.parseUtf8(l -> true), "parseUtf8(StopCharTester)"),
                NamedConsumer.of(b -> b.parse8bit(l -> true), "parse8bit(StopCharTester)"),
                NamedConsumer.of(b -> b.parse8bit(bytes, l -> true), "parse8bit(bytes, StopCharTester)"),
                NamedConsumer.of(b -> b.parse8bit(new StringBuilder(), (StopCharTester) l -> true), "parse8bit(sb, StopCharTester)"),
                NamedConsumer.of(b -> b.parse8bit(new StringBuilder(), (StopCharsTester) (l, m) -> true), "parse8bit(sb, StopCharsTester)"),
                NamedConsumer.of(b -> b.parse8bit(new StringBuilder(), l -> true), "parse8bit(sb, p)"),
                NamedConsumer.of(ByteStringParser::parseBigDecimal, "parseBigDecimal()"),
                NamedConsumer.of(ByteStringParser::parseBoolean, "parseBoolean()"),
                NamedConsumer.of(b -> b.parseBoolean(l -> true), "parseBoolean(StopCharTester)"),
                // Resource operations
                NamedConsumer.of(ReferenceCounted::releaseLast, "releaseLast()"),
                NamedConsumer.of(b -> b.reserve(INIT), "tryReserve(INIT)"),
                // https://github.com/OpenHFT/Chronicle-Core/issues/270
                // NamedConsumer.of(b -> b.reservedBy(INIT), "reservedBy(INIT)"),
                NamedConsumer.of(b -> b.reserveTransfer(INIT, INIT), "reserveTransfer(INIT, INIT)"),
                // Other
                NamedConsumer.of(Bytes::bytesForRead, "bytesForRead()"),
                NamedConsumer.of(Bytes::bytesForWrite, "bytesForWrite()"),
                NamedConsumer.of(b -> b.unchecked(true), "unchecked(true)"),
                NamedConsumer.of(b -> b.unchecked(false), "unchecked(false)"),
                // Copy operations
                NamedConsumer.of(Bytes::copy, "copy()"),
                NamedConsumer.ofThrowing(b -> b.copyTo(os), "copyTo(OutputStream)"),
                NamedConsumer.of(b -> b.copyTo(bs), "copyTo(ByteStore)"),
                NamedConsumer.of(bs::copyTo, "Bytes.copyTo(b)"),
                NamedConsumer.of(b -> b.copyTo(new byte[10]), "copyTo(byte[])"),
                NamedConsumer.of(b -> b.copyTo(ByteBuffer.allocate(10)), "copyTo(ByteBuffer)"),
                // Object creation operations
                NamedConsumer.of(Bytes::readBigInteger, "readBigDecimal()"),
                NamedConsumer.of(Bytes::readBigDecimal, "readBigInteger()"),
                NamedConsumer.of(b -> b.readHistogram(new Histogram()), "readHistogram()"),
                NamedConsumer.of(ByteStringParser::reader, "reader()"),
                NamedConsumer.of(BytesIn::bytesMethodReaderBuilder, "bytesMethodReaderBuilder()"),
                NamedConsumer.of(BytesIn::bytesMethodReader, "bytesMethodReader()"),
                NamedConsumer.of(b -> b.bytesMethodWriter(Object.class), "bytesMethodWriter(Object.class)"),
                // Certain write operations
                NamedConsumer.of(b -> b.writeBigInteger(BigInteger.ONE), "writeBigInteger()"),
                NamedConsumer.of(b -> b.writeBigDecimal(BigDecimal.ONE), "writeBigDecimal()"),
                NamedConsumer.of(b -> b.writeHistogram(new Histogram()), "writeHistogram()"),
                NamedConsumer.of(ByteStringAppender::writer, "writer()"),
                NamedConsumer.of(b -> b.bytesMethodWriter(Object.class), "bytesMethodWriter()")
        );
    }

    /**
     * Checks that methods throws ClosedIllegalStateException and does not change the state of the Bytes
     */
    @TestFactory
    Stream<DynamicTest> nonPerformanceCriticalOperators() {
        final AtomicReference<BytesInitialInfo> initialInfo = new AtomicReference<>();
        return cartesianProductTest(BytesFactoryUtil::provideBytesObjects,
                BytesReleaseInvariantNonPerformantMethodsTest::provideNonPerformantOperations,
                (args, bytes, nc) -> {
                    if (bytes.refCount() > 0) {
                        if (isReadWrite(args)) {
                            bytes.write(SILLY_NAME);
                        }
                        initialInfo.set(new BytesInitialInfo(bytes));
                        bytes.releaseLast();
                    }
                    final String name = createCommand(args) + "->" + bytes(args).getClass().getSimpleName() + "." + nc.name();

                    assertThrows(ClosedIllegalStateException.class, () -> nc.accept(bytes), name);

                    // Unable to check actual size for released MappedBytes
                    if ((Bytes) bytes instanceof MappedBytes || bytes instanceof EmbeddedBytes)
                        return;
                    final BytesInitialInfo info = new BytesInitialInfo(bytes);
                    assertEquals(initialInfo.get(), info, name);
                }
        );
    }

    /**
     * Checks the bytes.toDebugString() works with released resources
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void toDebugString(final Bytes<?> bytes, final boolean readWrite) {
        toDebug(bytes, readWrite, Bytes::toDebugString);
    }

    /**
     * Checks the bytes.toDebugString(10) works with released resources
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void toDebugString10(final Bytes<?> bytes, final boolean readWrite) {
        toDebug(bytes, readWrite, b -> b.toDebugString(10));
    }

    // Rather than throwing an exception, toDebug provides a String instead
    private void toDebug(final Bytes<?> bytes, final boolean readWrite, Function<? super Bytes<?>, String> operation) {
        if (readWrite) {
            bytes.append(SILLY_NAME);
        }
        releaseAndAssertReleased(bytes);
        final String actual = operation.apply(bytes);
        assertEquals("<released>", actual);
    }
}