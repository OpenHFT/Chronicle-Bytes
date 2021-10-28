package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

final class BytesJavaDocComplianceTest {

    @ParameterizedTest
    @MethodSource("provideBytesObjects")
    void printTypesTested(final Bytes<?> bytes) {
        System.out.println(bytes.getClass().getName());
    }

    @ParameterizedTest
    @MethodSource("provideBytesObjects")
    void unchecked(final Bytes<?> bytes) {
        assertEquals(bytes.getClass().getSimpleName().contains("Unchecked"), bytes.unchecked());
    }

    @ParameterizedTest
    @MethodSource("provideBytesObjects")
    @Disabled("https://github.com/OpenHFT/Chronicle-Bytes/issues/239")
    void readWrite(final Bytes<?> bytes,
                   final boolean readWrite) {

        boolean writeable = false;
        try {
            bytes.writeByte((byte) 1);
            writeable = true;
        } catch (Exception ignored) {
            // We cannot write
        }
        // Check if the actual writing ability corresponds to the reality
        assertEquals(readWrite, writeable);

        // Check that bytes reflects this
        assertEquals(writeable, bytes.readWrite());
    }

    @Test
    void wrapForReadCannotTakeReadOnlyByteBuffers() {
        assertThrows(ReadOnlyBufferException.class, () ->
                Bytes.wrapForRead(ByteBuffer.allocate(10).asReadOnlyBuffer())
        );
    }

    @Test
    void wrapForWriteCannotTakeReadOnlyByteBuffers() {
        assertThrows(ReadOnlyBufferException.class, () ->
                Bytes.wrapForWrite(ByteBuffer.allocate(10).asReadOnlyBuffer())
        );
    }

    @Test
    void write() {
/*
        try {
            final File file = File.createTempFile("mapped-file", "bin");
            MappedBytes.mappedBytes(file, SIZE)
                .write8bit(0L, (BytesStore) null);
        } catch (IOException ioException) {
            Jvm.rethrow(ioException);
        }
*/

/*        try {
            Bytes.allocateDirect(SIZE)
                    .write((InputStream) null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/
/*
        Bytes.allocateElasticOnHeap(SIZE)
                .write8bit(0L, (BytesStore) null);

        Bytes.allocateDirect(SIZE)
                .writeSome((Bytes) null);*/

    }

    // Todo: initialize a Map with capacity, start, stop etc. and then assert that they are not changed in any null/-1 operation

    // Todo: Do some write operations so that we know we have content then try operations

    // Todo: Create with NativeBytes.setNewGuarded

    @TestFactory
    Stream<DynamicTest> nonNullableOperators() {
        return provideBytesObjects()
                .flatMap(arguments -> provideThrowsMullPointerExceptionOperations()
                        .map(operation -> {
                                    final String name = arguments.get()[2].toString() + "->" + arguments.get()[0].getClass().getSimpleName() + "." + operation.name();
                                    return dynamicTest(name, () -> {
                                                @SuppressWarnings("unchecked") final Bytes<Object> bytes = (Bytes<Object>) arguments.get()[0];
                                                // System.out.println(name);
                                                assertThrows(NullPointerException.class, () -> operation.accept(bytes), name);
                                                if (Boolean.FALSE.equals(arguments.get()[2])) {
                                                    assertNeverWrittenTo(bytes);
                                                }
                                            }
                                    );
                                }
                        )
                );
    }

    @TestFactory
    Stream<DynamicTest> nullableOperators() {
        return cartesianProductTest(BytesJavaDocComplianceTest::provideBytesObjects,
                BytesJavaDocComplianceTest::provideNullableOperations,
                (args, bytes, nc) -> {
                    if (Boolean.FALSE.equals(args.get()[2])) {
                        assertDoesNotThrow(() -> nc.accept(bytes));
                        // Make sure something was written
                        assertNotEquals(0, bytes.readByte(0));
                        assertNotEquals(0L, bytes.writePosition());
                    }
                }
        );
    }

    private Stream<DynamicTest> cartesianProductTest(Supplier<Stream<Arguments>> bytesObjectSupplier,
                                                     Supplier<Stream<NamedConsumer<Bytes<Object>>>> operationsSupplier,
                                                     TriConsumer<Arguments, Bytes<Object>, NamedConsumer<Bytes<Object>>> test) {
        return bytesObjectSupplier.get()
                .flatMap(arguments -> operationsSupplier.get()
                        .map(operation -> dynamicTest(arguments.get()[2].toString() + "." + operation.name(), () -> {
                                            @SuppressWarnings("unchecked") final Bytes<Object> bytes = (Bytes<Object>) arguments.get()[0];
                                            test.accept(arguments, bytes, operation);
                                        }
                                )
                        )
                );
    }

    private static Stream<NamedConsumer<Bytes<Object>>> provideThrowsMullPointerExceptionOperations() {
        return Stream.of(
                // Write operations with reference parameters
                NamedConsumer.ofThrowing(bytes -> bytes.write((InputStream) null), "write(InputStream)"),
                NamedConsumer.of(bytes -> bytes.writeMarshallableLength16(null), "writeMarshallableLength16()"),
                NamedConsumer.of(bytes -> bytes.write(0L, (byte[]) null, 0, 0), "write(long, byte[], int, int)"),
                NamedConsumer.of(bytes -> bytes.write((byte[]) null, 0, 0), "write(byte[], int, int)"),
                NamedConsumer.of(bytes -> bytes.write((byte[]) null), "write(byte[])"),
                NamedConsumer.of(bytes -> bytes.write(1L, (byte[]) null), "write(long, byte[])"),
                NamedConsumer.of(bytes -> bytes.write((CharSequence) null, 0, 0), "write(CharSequence, int, int)"),
                NamedConsumer.of(bytes -> bytes.write((CharSequence) null), "write(CharSequence)"),
                NamedConsumer.of(bytes -> bytes.write(1L, (BytesStore) null, 0L, 0L), "write(long, BytesStore, long, long)"),
                NamedConsumer.of(bytes -> bytes.write((BytesStore<?, ?>) null, 0L, 0L), "write(BytesStore, long, long)"),
                NamedConsumer.of(bytes -> bytes.write((BytesStore<?, ?>) null), "write(BytesStore)"),
                NamedConsumer.of(bytes -> bytes.write(0L, (BytesStore<?, ?>) null), "write(long, BytesStore)"),
                NamedConsumer.of(bytes -> bytes.write(0L, (RandomDataInput) null, 0L, 0L), "write(long, RandomDataInput, long, long)"),
                NamedConsumer.of(bytes -> bytes.write((RandomDataInput) null, 0L, 0L), "write(RandomDataInput, long, long)"),
                NamedConsumer.of(bytes -> bytes.write((RandomDataInput) null), "write(RandomDataInput)"),
                NamedConsumer.of(bytes -> bytes.write(0, (RandomDataInput) null, 0, 0), "write(long, RandomDataInput, long, long)"),
                NamedConsumer.of(bytes -> bytes.write8bit((CharSequence) null, 0, 0), "write8bit(CharSequence, int, int)"),
                NamedConsumer.of(bytes -> bytes.write8bit((String) null, 0, 0), "write8bit(String, int, int)"),
                NamedConsumer.of(bytes -> bytes.write8bit(0L, (BytesStore) null), "write8bit(long, BytesStore)"),
                NamedConsumer.of(bytes -> bytes.write8bit(0L, (String) null, 0, 0), "write8bit(long, String, int, int)"),
                NamedConsumer.of(bytes -> bytes.writeBigDecimal(null), "writeBigDecimal()"),
                NamedConsumer.of(bytes -> bytes.writeBigInteger(null), "writeBigInteger()"),
                NamedConsumer.of(bytes -> bytes.writeHistogram(null), "writeHistogram()"),
                NamedConsumer.of(bytes -> bytes.writeSome((Bytes<?>) null), "writeSome(Bytes)"),
                NamedConsumer.of(bytes -> bytes.writeSome((ByteBuffer) null), "writeSome(ByteBuffer)"),
                NamedConsumer.of(bytes -> bytes.writeEnum((MyEnum) null), "writeEnum()")
                // Read operations with reference parameters

                // Todo: add unsafe...

                // Todo: add read operations
                // Todo: add prewrite
                // Todo: Add writeBigInteger et al.
        );
    }

    private static Stream<NamedConsumer<Bytes<Object>>> provideNullableOperations() {
        return Stream.of(
                NamedConsumer.of(bytes -> bytes.writeUtf8((CharSequence) null), "writeUtf8(CharSequence)"),
                NamedConsumer.of(bytes -> bytes.writeUtf8((String) null), "writeUtf8(String)"),
                NamedConsumer.of(bytes -> bytes.writeUtf8(0L, (CharSequence) null), "writeUtf8(long, CharSequence)"),
                NamedConsumer.of(bytes -> bytes.writeUtf8Limited(0L, (CharSequence) null, 0), "writeUtf8Limited(long, CharSequence, int)"),
                NamedConsumer.of(bytes -> bytes.write8bit((String) null), "write8bit(String)"),
                NamedConsumer.of(bytes -> bytes.write8bit((CharSequence) null), "write8bit(CharSequence)")
        );
    }

    enum MyEnum {INSTANCE}

    private static final int SIZE = 128;

    private interface HasName {
        String name();
    }

    @FunctionalInterface
    private interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    private static class NamedConsumer<T> implements HasName, Consumer<T> {

        private final Consumer<T> consumer;
        private final String name;

        private NamedConsumer(Consumer<T> consumer, String name) {
            this.consumer = consumer;
            this.name = name;
        }

        @Override
        public void accept(T t) {
            consumer.accept(t);
        }

        @Override
        public String name() {
            return name;
        }

        static <T> NamedConsumer<T> of(Consumer<T> consumer, String name) {
            return new NamedConsumer<>(consumer, name);
        }

        static <T> NamedConsumer<T> ofThrowing(ThrowingConsumer<T, ?> consumer, String name) {
            return new NamedConsumer<>(wrapThrowing(consumer), name);
        }

    }


    @FunctionalInterface
    private interface ThrowingConsumer<T, X extends Exception> {
        void accept(T t) throws X;
    }

    private static <T> Consumer<T> wrapThrowing(ThrowingConsumer<T, ?> consumer) {
        return new ThrowingConsumerWrapper<>(consumer);
    }

    private static final class ThrowingConsumerWrapper<T> implements Consumer<T> {

        private final ThrowingConsumer<T, ?> delegate;

        public ThrowingConsumerWrapper(ThrowingConsumer<T, ?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(T t) {
            try {
                delegate.accept(t);
            } catch (Exception e) {
                Jvm.rethrow(e);
            }
        }
    }

    private static Stream<Arguments> provideBytesObjects() {
        final ByteBuffer heapByteBuffer = ByteBuffer.allocate(SIZE);
        final ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(SIZE);

        try {
            final File file = File.createTempFile("mapped-file", "bin");
            final File fileRo = File.createTempFile("mapped-file-ro", "bin");
            final File singleFile = File.createTempFile("single-mapped-file", "bin");
            final File singleFileRo = File.createTempFile("single-mapped-file-ro", "bin");
            final MassiveFieldHolder holder = new MassiveFieldHolder();
            return Stream.of(
                    Arguments.of(Bytes.allocateDirect(SIZE), true, "Bytes.allocateDirect(SIZE)"),
                    Arguments.of(Bytes.allocateElasticOnHeap(SIZE), true, "Bytes.allocateElasticOnHeap(SIZE)"),
                    Arguments.of(wipe(Bytes.allocateElasticDirect()), true, "Bytes.allocateElasticDirect()"),
                    Arguments.of(Bytes.wrapForWrite(heapByteBuffer), true, "Bytes.wrapForWrite(heapByteBuffer)"),
                    Arguments.of(Bytes.wrapForWrite(directByteBuffer), true, "Bytes.wrapForWrite(directByteBuffer)"),
                    Arguments.of(MappedBytes.mappedBytes(file, SIZE), true, "MappedBytes.mappedBytes(file, SIZE)"),
                    Arguments.of(MappedBytes.mappedBytes(fileRo, SIZE, 0, true), false, "MappedBytes.mappedBytes(fileRo, SIZE, 0, true)"),
                    Arguments.of(MappedBytes.singleMappedBytes(singleFile, SIZE), true, "MappedBytes.singleMappedBytes(singleFile, SIZE)"),
                    Arguments.of(MappedBytes.singleMappedBytes(singleFileRo, SIZE, true), false, "MappedBytes.singleMappedBytes(singleFileRo, SIZE, true)"),
                    Arguments.of(Bytes.forFieldGroup(holder, "b"), true, "Bytes.forFieldGroup(holder, \"b\")"),
                    Arguments.of(new UncheckedBytes<>(Bytes.allocateDirect(SIZE)), true, "new UncheckedBytes<>(Bytes.allocateDirect(SIZE))"),
                    Arguments.of(new UncheckedBytes<>(Bytes.wrapForWrite(new byte[SIZE])), true, "new UncheckedBytes<>(Bytes.wrapForWrite(new byte[SIZE]))"),
                    Arguments.of(new HexDumpBytes(), true, "new HexDumpBytes()")
            );
        } catch (IOException ioException) {
            HexDumpBytes h = new HexDumpBytes();
            throw new AssertionError("Unable to create Bytes", ioException);
        }
    }

    private static final class MassiveFieldHolder {

        @FieldGroup("b")
        long a0, a1, a2, a3, a4, a5, a6, a7;
        long b0, b1, b2, b3, b4, b5, b6, b7;
        //final byte[] bytes = new byte[SIZE];

        static {
            // Fields above must match
            assert SIZE == Long.BYTES * 8 * 2;
        }

    }

    private static <B extends Bytes<U>, U> B wipe(B bytes) {
        for (int i = 0; i < SIZE; i++) {
            bytes.writeByte((byte) 0);
        }
        return bytes;
    }

    private void assertNeverWrittenTo(final Bytes<Object> bytes) {
        assertTrue(bytes.isClear());
        for (int i = 0; i < SIZE; i++) {
            assertEquals(0, bytes.readByte(i));
        }
    }


}
