package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        boolean writeable= false;
        try {
            bytes.writeByte((byte)1);
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
    void from() {
        final Bytes<byte[]> from = Bytes.from("A");
        System.out.println("from = " + from);
        System.out.println("from.getClass = " + from.getClass());
        System.out.println("from.underlyingObject().getClass() = " + from.underlyingObject().getClass());
    }


    private static final int SIZE = 128;

    private static Stream<Arguments> provideBytesObjects() throws IOException {
        final ByteBuffer heapByteBuffer = ByteBuffer.allocate(SIZE);
        final ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(SIZE);
        // Bytes objects cannot be created from these read only
        final ByteBuffer readOnlyHeapByteBuffer = ByteBuffer.allocate(SIZE).asReadOnlyBuffer();
        final ByteBuffer directReadOnlyByteBuffer = ByteBuffer.allocateDirect(SIZE);

        final File file = File.createTempFile("mapped-file", "bin");
        final File fileRo = File.createTempFile("mapped-file-ro", "bin");
        final File singleFile = File.createTempFile("single-mapped-file", "bin");
        final File singleFileRo = File.createTempFile("single-mapped-file-ro", "bin");

        final MassiveFieldHolder holder = new MassiveFieldHolder();

        return Stream.of(
                Arguments.of(Bytes.allocateDirect(SIZE), true),
                Arguments.of(Bytes.allocateElasticOnHeap(SIZE), true),
                Arguments.of(Bytes.allocateElasticDirect(), true),
                Arguments.of(Bytes.wrapForWrite(heapByteBuffer), true),
                Arguments.of(Bytes.wrapForWrite(directByteBuffer), true),
                Arguments.of(MappedBytes.mappedBytes(file, SIZE), true),
                Arguments.of(MappedBytes.mappedBytes(fileRo, SIZE, 0, true), false),
                Arguments.of(MappedBytes.singleMappedBytes(singleFile, SIZE), true),
                Arguments.of(MappedBytes.singleMappedBytes(singleFileRo, SIZE, true), false),
                Arguments.of(Bytes.forFieldGroup(holder, "b"), true),
                Arguments.of(new UncheckedBytes<>(Bytes.allocateDirect(SIZE)), true),
                Arguments.of(new UncheckedBytes<>(Bytes.wrapForWrite(new byte[SIZE])), true)
        );
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

}
