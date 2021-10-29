package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.ReferenceCounted;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.BytesStore.wrap;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

final class BytesJavaDocComplianceTest extends BytesTestCommon {

    private static final Map<String, BytesInitialInfo> INITIAL_INFO_MAP = new LinkedHashMap<>();

    @BeforeEach
    void beforeEach() {
        // Build a Map with info on the initial state allowing us asserting it did not change for
        // illegal operations.
        // This method is using @BeforeEach rather than @BeforeAll so that the base class test can run
        if (INITIAL_INFO_MAP.isEmpty()) {
            provideBytesObjects()
                    .forEach(args -> {
                        final Bytes<Object> bytes = bytes(args);
                        INITIAL_INFO_MAP.put(createCommand(args), new BytesInitialInfo(bytes(args)));
                        releaseAndAssertReleased(bytes);
                    });
            // Make sure we have unique keys
            assertEquals(provideBytesObjects().count(), INITIAL_INFO_MAP.size());
        }
    }

    @ParameterizedTest
    @MethodSource("provideBytesObjects")
        // Prints out the various Bytes classes tested on the logs. To be removed later
    void printTypesTested(final Bytes<?> bytes) {
        System.out.println(bytes.getClass().getName());
        releaseAndAssertReleased(bytes);
    }

    @ParameterizedTest
    @MethodSource("provideBytesObjects")
        // Checks the Bytes::unchecked method
    void unchecked(final Bytes<?> bytes) {
        assertEquals(bytes.getClass().getSimpleName().contains("Unchecked"), bytes.unchecked());
        releaseAndAssertReleased(bytes);
    }

    @ParameterizedTest
    @MethodSource("provideBytesObjects")
    @Disabled("https://github.com/OpenHFT/Chronicle-Bytes/issues/239")
        // Checks the Bytes::readWrite method
    void readWrite(final Bytes<?> bytes,
                   final boolean readWrite) {

        boolean writeable = false;
        try {
            bytes.writeByte((byte) 1);
            writeable = true;
        } catch (Exception ignored) {
            // We cannot write
        }
        // Checks if the actual writing ability corresponds to the reality
        assertEquals(readWrite, writeable);

        // Checks that bytes reflects this
        assertEquals(writeable, bytes.readWrite());
        releaseAndAssertReleased(bytes);
    }

    @Test
        // Checks that ByteBuffers that are read only cannot be wrapped
    void wrapForReadCannotTakeReadOnlyByteBuffers() {
        assertThrows(ReadOnlyBufferException.class, () ->
                Bytes.wrapForRead(ByteBuffer.allocate(10).asReadOnlyBuffer())
        );
    }

    @Test
        // Checks that ByteBuffers that are read only cannot be wrapped
    void wrapForWriteCannotTakeReadOnlyByteBuffers() {
        assertThrows(ReadOnlyBufferException.class, () ->
                Bytes.wrapForWrite(ByteBuffer.allocate(10).asReadOnlyBuffer())
        );
    }

    @Test
    // This test is for manual debug
    void manualTest() {

        try {
            final File file = File.createTempFile("mapped-file", "bin");
            final Bytes<?> bytes = MappedBytes.mappedBytes(file, SIZE);
            bytes.releaseLast();
        } catch (IOException ioException) {
            Jvm.rethrow(ioException);
        }


/*        try {
            Bytes.allocateDirect(SIZE)
                    .write((InputStream) null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/


/*
        Bytes.allocateDirect(SIZE).unchecked(true)
                .read(0L, (byte[]) null, 1, 1);
*/


/*        Bytes.allocateDirect(SIZE)
                .readObject(null);*/

    }

    // Todo: Do some write operations so that we know we have content then try operations

    @TestFactory
        // Checks that methods with objet references that are annotated with @NotNull throw a NullPointerException when null is provided
        // and that no modification of the Bytes object's internal state is made in such cases.
    Stream<DynamicTest> nonNullableOperators() {
        return cartesianProductTest(BytesJavaDocComplianceTest::provideBytesObjects,
                BytesJavaDocComplianceTest::provideThrowsMullPointerExceptionOperations,
                (args, bytes, nc) -> {
                    final String name = createCommand(args) + "->" + bytes(args).getClass().getSimpleName() + "." + nc.name();
                    assertThrows(NullPointerException.class, () -> nc.accept(bytes), name);
                    if (isReadWrite(args)) {
                        assertNeverWrittenTo(bytes);
                    }
                    assertPropertiesNotChanged(createCommand(args), bytes);
                }
        );
    }

    @TestFactory
        // Checks that methods with objet references that are annotated with @Nullable *does not* throw a NullPointerException when null is provided
        // and that the Bytes object's internal state is indeed modified.
    Stream<DynamicTest> nullableOperators() {
        return cartesianProductTest(BytesJavaDocComplianceTest::provideBytesObjects,
                BytesJavaDocComplianceTest::provideNullableOperations,
                (args, bytes, nc) -> {
                    if (isReadWrite(args)) {
                        assertDoesNotThrow(() -> nc.accept(bytes));
                        // Make sure something was written
                        assertNotEquals(0, bytes.readByte(0));
                        assertNotEquals(0L, bytes.writePosition());
                    } else {
                        assertPropertiesNotChanged(createCommand(args), bytes);
                    }
                }
        );
    }

    private Stream<DynamicTest> cartesianProductTest(Supplier<Stream<Arguments>> bytesObjectSupplier,
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
                        Stream.of(dynamicTest("releaseLast", () -> bytes(arguments).releaseLast()))
                ));
    }

    private static Stream<NamedConsumer<Bytes<Object>>> provideThrowsMullPointerExceptionOperations() {

        // In some of the operations, we could use 1 as a parameter meaning some optimized versions (like unchecked)
        // will work with no explicit requireNonNull() but this is not strictly correct from a testing perspective

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
                NamedConsumer.of(bytes -> bytes.writeEnum((MyEnum) null), "writeEnum()"),
                // Read operations with reference parameters


                NamedConsumer.of(bytes -> bytes.read((ByteBuffer) null), "read(ByteBuffer)"),
                NamedConsumer.of(bytes -> bytes.read((Bytes<?>) null), "read(Bytes)"),
                NamedConsumer.of(bytes -> bytes.read((Bytes<?>) null, 0), "read(Bytes, int)"),
                NamedConsumer.of(bytes -> bytes.read((byte[]) null), "read(byte[], int)"),
                NamedConsumer.of(bytes -> bytes.read((byte[]) null, 0, 0), "read(byte[], int, int)"),
                NamedConsumer.of(bytes -> bytes.read((char[]) null, 0, 0), "read(char[], int, int)"),
                NamedConsumer.of(bytes -> bytes.read(0, (byte[]) null, 0, 0), "read(long, byte[], int, int)"),
                NamedConsumer.of(bytes -> bytes.readObject(null), "readObject(Class)"),
                NamedConsumer.of(bytes -> bytes.readMarshallableLength16(null, null), "readMarshallableLength16(Class, Object)"),
                NamedConsumer.of(bytes -> bytes.readUtf8Limited(0, null, 0), "readUtf8Limited(long, ACS, int)"),
                NamedConsumer.of(bytes -> bytes.readUtf8(0, null), "readUtf8Limited(long, ACS)"),
                NamedConsumer.of(bytes -> bytes.readUtf8((Appendable & CharSequence) null), "readUtf8(ACS)"),
                NamedConsumer.of(bytes -> bytes.readUtf8((StringBuilder) null), "readUtf8(StringBuilder)"),
                NamedConsumer.of(bytes -> bytes.readUtf8((Bytes<?>) null), "readUtf8(Bytes)")

                // readHistogram

                // Todo: add unsafe...
                // Todo: add read operations
                // Todo: add prewrite
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

    private static void releaseAndAssertReleased(ReferenceCounted referenceCounted) {
        referenceCounted.releaseLast();
        assertEquals(0, referenceCounted.refCount());
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
                    Arguments.of(new HexDumpBytes(), true, "new HexDumpBytes()"),
                    Arguments.of(Bytes.allocateDirect(SIZE).unchecked(true), true, "Bytes.allocateDirect(SIZE).unchecked(true)"),
                    Arguments.of(Bytes.allocateElasticOnHeap(SIZE).unchecked(true), true, "Bytes.allocateElasticOnHeap(SIZE).unchecked(true)"),
                    Arguments.of(new GuardedNativeBytes<>(wrap(ByteBuffer.allocate(SIZE)), SIZE), true, "new GuardedNativeBytes<>(wrap(ByteBuffer.allocate(SIZE))")
            );
        } catch (IOException ioException) {
            throw new AssertionError("Unable to create Bytes", ioException);
        }
    }

    @SuppressWarnings("unchecked")
    private static Bytes<Object> bytes(Arguments arguments) {
        return ((Bytes<Object>) arguments.get()[0]);
    }

    private static boolean isReadWrite(Arguments arguments) {
        return Boolean.FALSE.equals(arguments.get()[2]);
    }

    private static String createCommand(Arguments arguments) {
        return arguments.get()[2].toString();
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

    private void assertPropertiesNotChanged(String createCommand, Bytes<?> bytes) {
        // Make sure that there was no change to the target bytes
        final BytesInitialInfo expectedInfo = INITIAL_INFO_MAP.get(createCommand);
        final BytesInitialInfo actualInfo = new BytesInitialInfo(bytes);
        assertEquals(expectedInfo, actualInfo, createCommand);
    }

    private static final class BytesInitialInfo {
        private final long readPosition;
        private final long writePosition;
        private final long writeLimit;
        private final long capacity;
        private final long realCapacity;
        //
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
