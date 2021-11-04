package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.ChunkedMappedBytes;
import net.openhft.chronicle.bytes.internal.EmbeddedBytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

final class BytesJavaDocComplianceTest extends BytesTestCommon {

    private static final Map<String, BytesInitialInfo> INITIAL_INFO_MAP = new LinkedHashMap<>();

    /**
     * Builds a Map with info on the initial state allowing us asserting it did not change for
     * illegal operations.
     * This method is using @BeforeEach rather than @BeforeAll so that the base class test can run
     */
    @BeforeEach
    void beforeEach() {
        if (INITIAL_INFO_MAP.isEmpty()) {
            AtomicInteger count = new AtomicInteger(0);
            provideBytesObjects()
                    .forEach(args -> {
                        count.incrementAndGet();
                        final Bytes<Object> bytes = bytes(args);
                        INITIAL_INFO_MAP.put(createCommand(args), new BytesInitialInfo(bytes));
                        releaseAndAssertReleased(bytes);
                        Jvm.pause(100);
                    });
            // Make sure we have unique keys
            assertEquals(count.get(), INITIAL_INFO_MAP.size());
        }
    }

    /**
     * Prints out the various Bytes classes tested on the logs. To be removed later
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void printTypesTested(final Bytes<?> bytes) {
        System.out.println(bytes.getClass().getName());
        releaseAndAssertReleased(bytes);
    }

    /**
     * Checks the Bytes::unchecked method
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    @Disabled("Check why  SingleMappedFile Discarded without being released by [SingleMappedBytes@5 refCount=1 closed=false]")
    void unchecked(final Bytes<?> bytes) {
        assertEquals(bytes.getClass().getSimpleName().contains("Unchecked"), bytes.unchecked());
        releaseAndAssertReleased(bytes);
    }

    /**
     * Checks the Bytes::readWrite method.
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
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
        // Checks if the actual writing ability corresponds to the reality
        assertEquals(readWrite, writeable);

        // Checks that bytes reflects this
        assertEquals(writeable, bytes.readWrite());
        releaseAndAssertReleased(bytes);
    }

    /**
     * Checks that ByteBuffers that are read only cannot be wrapped.
     */
    @Test
    void wrapForReadCannotTakeReadOnlyByteBuffers() {
        final ByteBuffer bb = ByteBuffer.allocate(10).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () ->
                Bytes.wrapForRead(bb)
        );
    }

    /**
     * Checks that ByteBuffers that are read only cannot be wrapped
     */
    @Test
    void wrapForWriteCannotTakeReadOnlyByteBuffers() {
        final ByteBuffer bb = ByteBuffer.allocate(10).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () ->
                Bytes.wrapForWrite(bb)
        );
    }


    // Todo: Do some write operations so that we know we have content then try operations

    /**
     * Checks that methods with objet references that are annotated with @NotNull throw a NullPointerException when null is provided
     * and that no modification of the Bytes object's internal state is made in such cases.
     */
    @TestFactory
    Stream<DynamicTest> nonNullableOperators() {
        return cartesianProductTest(BytesFactoryUtil::provideBytesObjects,
                BytesJavaDocComplianceTest::provideThrowsMullPointerExceptionOperations,
                (args, bytes, nc) -> {
                    final String name = createCommand(args) + "->" + bytes(args).getClass().getSimpleName() + "." + nc.name();

                    assertThrows(NullPointerException.class, () -> nc.accept(bytes), name);

                    if (isReadWrite(args)) {
                        if (!(ChunkedMappedBytes.class.isInstance(bytes))) {
                            // Unfortunately, inspecting a ChunkedMappedBytes may change its actualSize(), so no check there

                            if (EmbeddedBytes.class.isInstance(bytes)) {
                                int foo = 1;
                            }

                            assertNeverWrittenTo(bytes);
                        }
                    }
                    assertPropertiesNotChanged(createCommand(args), bytes);
                }
        );
    }

    /**
     * Checks that methods with objet references that are annotated with @Nullable *does not* throw a NullPointerException when null is provided
     * and that the Bytes object's internal state is indeed modified.
     */
    @TestFactory
    Stream<DynamicTest> nullableOperators() {
        return cartesianProductTest(BytesFactoryUtil::provideBytesObjects,
                BytesJavaDocComplianceTest::provideNullableOperations,
                (args, bytes, nc) -> {
                    // Any content from a previous operation is cleared
                    bytes.clear();
                    // System.out.println(bytes.getClass().getSimpleName() + " " + isReadWrite(args)+", writePosition()="+bytes.writePosition());

                    if (isReadWrite(args)) {
                        assertDoesNotThrow(() -> nc.accept(bytes));
                        // Make sure something was written
                        assertNotEquals(0, bytes.readShort(0));
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


    /**
     * This test is for manual debug
     */
    // @Test
    void manualTest() {

        // try {
        //   final File file = create(File.createTempFile("mapped-file", "bin"), SIZE);
        //    final Bytes<?> bytes = MappedBytes.mappedBytes(file, SIZE);
        //   bytes.realCapacity();
        //   System.out.println("bytes.realCapacity() = " + bytes.realCapacity());
        //    try {
        //       bytes.write((InputStream) null);
        //   } catch (Exception e) {
        //       //ignore
        //   }
        //   System.out.println("bytes.realCapacity() = " + bytes.realCapacity());
        //   bytes.releaseLast();
        //} catch (IOException ioException) {
        //   Jvm.rethrow(ioException);
        //}

        //try {
        //   Bytes.allocateDirect(SIZE)
        //            .write((InputStream) null);
        //} catch (Exception e) {
        //    throw new RuntimeException(e);
        // }

        // Bytes<?> bytes = Bytes.allocateDirect(SIZE);
        // bytes.writeUtf8(1, (CharSequence) null);
        // releaseAndAssertReleased(bytes);


        // Bytes.allocateDirect(SIZE)
        //        .readObject(null);

        // MassiveFieldHolder holder = new MassiveFieldHolder();
        // Bytes<MassiveFieldHolder> bytes = Bytes.forFieldGroup(holder, "b");
        // try {
//            bytes.write((InputStream) null);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        releaseAndAssertReleased(bytes);

    }

}
