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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.ReferenceCounted;
import net.openhft.chronicle.core.util.ThrowingConsumer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.BytesStore.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

final class BytesFactoryUtil {

    static final int SIZE = 128;
    static final int CHUNK_SIZE = 64 << 10;
    private static final AtomicInteger CNT = new AtomicInteger();
    private BytesFactoryUtil() {
    }

    static Stream<Arguments> provideBytesObjects() {
        final ByteBuffer heapByteBuffer = ByteBuffer.allocate(SIZE);
        final ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(SIZE);

        try {
            final File file = create(File.createTempFile("mapped-file" + CNT.getAndIncrement(), "bin"), CHUNK_SIZE * 2);
            file.deleteOnExit();
            final File fileRo = create(File.createTempFile("mapped-file-ro" + CNT.getAndIncrement(), "bin"), CHUNK_SIZE * 2);
            fileRo.deleteOnExit();
            final File singleFile = create(File.createTempFile("single-mapped-file" + CNT.getAndIncrement(), "bin"), CHUNK_SIZE * 2);
            singleFile.deleteOnExit();
            // Must be filled to the CHUNK_SIZE
            final File singleFileRo = create(File.createTempFile("single-mapped-file-ro" + CNT.getAndIncrement(), "bin"), CHUNK_SIZE * 2);
            singleFileRo.deleteOnExit();
            final MassiveFieldHolder holder = new MassiveFieldHolder();
            return Stream.of(
                    Arguments.of(Bytes.allocateDirect(SIZE), true, "Bytes.allocateDirect(SIZE)"),
                    Arguments.of(Bytes.allocateElasticOnHeap(SIZE), true, "Bytes.allocateElasticOnHeap(SIZE)"),
                    Arguments.of(wipe(Bytes.allocateElasticDirect()), true, "Bytes.allocateElasticDirect()"),
                    Arguments.of(Bytes.wrapForWrite(heapByteBuffer), true, "Bytes.wrapForWrite(heapByteBuffer)"),
                    Arguments.of(Bytes.wrapForWrite(directByteBuffer), true, "Bytes.wrapForWrite(directByteBuffer)"),
                    Arguments.of(MappedBytes.mappedBytes(file, CHUNK_SIZE), true, "MappedBytes.mappedBytes(file, CHUNK_SIZE)"),
                    Arguments.of(MappedBytes.mappedBytes(fileRo, CHUNK_SIZE, 0, true), false, "MappedBytes.mappedBytes(fileRo, CHUNK_SIZE, 0, true)"),
                    Arguments.of(MappedBytes.singleMappedBytes(singleFile, CHUNK_SIZE), true, "MappedBytes.singleMappedBytes(singleFile, CHUNK_SIZE)"),
                    Arguments.of(MappedBytes.singleMappedBytes(singleFileRo, CHUNK_SIZE, true), false, "MappedBytes.singleMappedBytes(singleFileRo, CHUNK_SIZE, true)"),

                     Arguments.of(Bytes.forFieldGroup(holder, "a"), true, "Bytes.forFieldGroup(holder, \"a\")"),

                    Arguments.of(new UncheckedBytes<>(Bytes.allocateDirect(SIZE)), true, "new UncheckedBytes<>(Bytes.allocateDirect(SIZE))"),
                    Arguments.of(new UncheckedBytes<>(Bytes.wrapForWrite(new byte[SIZE])), true, "new UncheckedBytes<>(Bytes.wrapForWrite(new byte[SIZE]))"),
                    Arguments.of(new HexDumpBytes(), true, "new HexDumpBytes()"),
                    Arguments.of(Bytes.allocateDirect(SIZE).unchecked(true), true, "Bytes.allocateDirect(SIZE).unchecked(true)"),
                    Arguments.of(Bytes.allocateElasticOnHeap(SIZE).unchecked(true), true, "Bytes.allocateElasticOnHeap(SIZE).unchecked(true)"),
                    Arguments.of(new GuardedNativeBytes<>(wrap(ByteBuffer.allocate(SIZE)), SIZE), true, "new GuardedNativeBytes<>(wrap(ByteBuffer.allocate(SIZE))")

            );
/*                    // Avoids java.io.IOException: Not enough storage is available to process this command
                    .filter(arguments -> !(OS.isWindows() && !isReadWrite(arguments)));*/
        } catch (IOException ioException) {
            System.out.flush();
            System.err.flush();
            ioException.printStackTrace();
            throw new AssertionError("Unable to create Bytes", ioException);
        }
    }

    static Bytes<Object> bytes(Arguments arguments) {
        return ((Bytes<Object>) arguments.get()[0]);
    }

    static boolean isReadWrite(Arguments arguments) {
        return Boolean.TRUE.equals(arguments.get()[1]);
    }

    static String createCommand(Arguments arguments) {
        return arguments.get()[2].toString();
    }

    static <B extends Bytes<U>, U> B wipe(B bytes) {
        for (int i = 0; i < SIZE; i++) {
            bytes.writeByte(i, (byte) 0);
        }
        return bytes;
    }

    static void releaseAndAssertReleased(ReferenceCounted referenceCounted) {
        referenceCounted.releaseLast();
        assertEquals(0, referenceCounted.refCount());
    }

    static void assertNeverWrittenTo(final Bytes<Object> bytes) {
        assertTrue(bytes.isClear());
        for (int i = 0; i < SIZE; i++) {
            assertEquals(0, bytes.readByte(i), "at " + i);
        }
    }

    static File truncate(final File file) {
        try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
            fc.truncate(0);
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static File create(final File file, int byteSize) {
        try {
            Files.write(file.toPath(), new byte[byteSize], StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return file;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create: " + file.getAbsolutePath(), e);
        }
    }

    static Stream<DynamicTest> cartesianProductTest(Supplier<Stream<Arguments>> bytesObjectSupplier,
                                                    Supplier<Stream<NamedConsumer<Bytes<Object>>>> operationsSupplier,
                                                    TriConsumer<Arguments, Bytes<Object>, NamedConsumer<Bytes<Object>>> test) {
        return bytesObjectSupplier.get()
                .flatMap(arguments -> Stream.concat(
                        operationsSupplier.get()
                                .map(operation -> dynamicTest(createCommand(arguments) + "." + operation.name(), () -> {
                                                    @SuppressWarnings("unchecked") final Bytes<Object> bytes = (Bytes<Object>) arguments.get()[0];
                                                    test.accept(arguments, bytes, operation);
                                                }
                                        )
                                ),
                        Stream.of(dynamicTest("---" + createCommand(arguments) + ".releaseLast() if not released (" + bytes(arguments).getClass().getSimpleName() + ")", () -> {
                            if (bytes(arguments).refCount() > 0)
                                bytes(arguments).releaseLast();
                        }))
                ));
    }

    interface HasName {
        String name();
    }

    @FunctionalInterface
    interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    static final class MassiveFieldHolder {
        // need an extra byte for the length
        @FieldGroup("a")
        long a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, aa, ab, ac, ad, ae, af, ag;
    }

    static class NamedConsumer<T> implements HasName, Consumer<T> {

        private final Consumer<T> consumer;
        private final String name;

        NamedConsumer(Consumer<T> consumer, String name) {
            this.consumer = consumer;
            this.name = name;
        }

        static <T> NamedConsumer<T> of(Consumer<T> consumer, String name) {
            return new NamedConsumer<>(consumer, name);
        }

        static <O, T extends Throwable> NamedConsumer<O> ofThrowing(ThrowingConsumer<O, T> consumer, String name) {
            return new NamedConsumer<>(new ThrowingConsumerWrapper<>(consumer), name);
        }

        @Override
        public void accept(T t) {
            consumer.accept(t);
        }

        @Override
        public String name() {
            return name;
        }

    }

    private static final class ThrowingConsumerWrapper<O, T extends Throwable> implements Consumer<O> {

        private final ThrowingConsumer<O, T> delegate;

        public ThrowingConsumerWrapper(ThrowingConsumer<O, T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(O o) {
            try {
                delegate.accept(o);
            } catch (Throwable e) {
                Jvm.rethrow(e);
            }
        }
    }

    static final class BytesInitialInfo {
        private final long readPosition;
        private final long writePosition;
        private final long writeLimit;
        private final long capacity;
        private final long realCapacity;
        private final boolean directMemory;
        private final boolean elastic;

        public BytesInitialInfo(Bytes<?> bytes) {
            this.readPosition = bytes.readPosition();
            this.writePosition = bytes.writePosition();
            this.writeLimit = bytes.writeLimit();
            this.capacity = bytes.capacity();
            this.realCapacity = bytes.realCapacity();
            this.directMemory = bytes.isDirectMemory();
            this.elastic = bytes.isElastic();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BytesInitialInfo that = (BytesInitialInfo) o;

            if (readPosition != that.readPosition) return false;
            if (writePosition != that.writePosition) return false;
            if (writeLimit != that.writeLimit) return false;
            if (capacity != that.capacity) return false;
            if (realCapacity != that.realCapacity) return false;
            if (directMemory != that.directMemory) return false;
            return elastic == that.elastic;
        }

        @Override
        public int hashCode() {
            int result = (int) (readPosition ^ (readPosition >>> 32));
            result = 31 * result + (int) (writePosition ^ (writePosition >>> 32));
            result = 31 * result + (int) (writeLimit ^ (writeLimit >>> 32));
            result = 31 * result + (int) (capacity ^ (capacity >>> 32));
            result = 31 * result + (int) (realCapacity ^ (realCapacity >>> 32));
            result = 31 * result + (directMemory ? 1 : 0);
            result = 31 * result + (elastic ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "BytesInitialInfo{" +
                    "readPosition=" + readPosition +
                    ", writePosition=" + writePosition +
                    ", writeLimit=" + writeLimit +
                    ", capacity=" + capacity +
                    ", realCapacity=" + realCapacity +
                    ", directMemory=" + directMemory +
                    ", elastic=" + elastic +
                    '}';
        }
    }
}