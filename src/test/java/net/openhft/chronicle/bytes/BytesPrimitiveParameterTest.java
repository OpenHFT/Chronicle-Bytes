package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests if certain methods with constraints on primitive method parameters work as expected.
 */
final class BytesPrimitiveParameterTest extends BytesTestCommon {

    private static final String SILLY_NAME = "Tryggve";

    /**
     * Checks that methods throws IllegalArgumentException if negative parameters are provided for @NonNegative
     */
    @TestFactory
    Stream<DynamicTest> negativeParameters() {
        final AtomicReference<BytesInitialInfo> initialInfo = new AtomicReference<>();
        return cartesianProductTest(BytesFactoryUtil::provideBytesObjects,
                BytesPrimitiveParameterTest::provideNegativeNonNegativeOperations,
                (args, bytes, nc) -> {
                    if (bytes.refCount() > 0) {
                        if (isReadWrite(args)) {
                            bytes.write(SILLY_NAME);
                        }
                        initialInfo.set(new BytesInitialInfo(bytes));
                    }
                    final String name = createCommand(args) + "->" + bytes(args).getClass().getSimpleName() + "." + nc.name();

                    assertThrows(IllegalArgumentException.class, () -> nc.accept(bytes), name);

                    // Unable to check actual size for released MappedBytes
                    if (!MappedBytes.class.isInstance(bytes)) {
                        final BytesInitialInfo info = new BytesInitialInfo(bytes);
                        assertEquals(initialInfo.get(), info, name);
                    }
                }
        );
    }

    private static Stream<NamedConsumer<Bytes<Object>>> provideNegativeNonNegativeOperations() {
        final OutputStream os = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new UnsupportedEncodingException();
            }
        };
        final BytesStore<?, ?> bs = BytesStore.from(SILLY_NAME);
        final Bytes<?> bytes = Bytes.from(SILLY_NAME);
        final ByteBuffer bb = ByteBuffer.allocate(10);
        return Stream.of(
                NamedConsumer.of(b -> b.write(new byte[1], -1, 1), "write(new byte[1], -1, 1)"),
                NamedConsumer.of(b -> b.write(new byte[1], 1, -1), "write(new byte[1], 1, -1)"),
                NamedConsumer.of(b -> b.write(-1, new byte[1], 1, 1), "write(-1, new byte[1], 1, -1)"),
                NamedConsumer.of(b -> b.write(1, new byte[1], -1, 1), "write(1, new byte[1], -1, 1)"),
                NamedConsumer.of(b -> b.write(1, new byte[1], 1, -1), "write(1, new byte[1], 1, -1)")
        );
    }

    @Test
    void a() throws IOException {

        Bytes<Object> bytes = BytesFactoryUtil.provideBytesObjects()
                .map(BytesFactoryUtil::bytes)
                .filter(MappedBytes.class::isInstance)
                .findAny()
                .get();

        //Bytes<?> bytes = Bytes.allocateDirect(SIZE).unchecked(true);
        //Bytes<?> bytes = Bytes.allocateDirect(SIZE).unchecked(true) ;
        // Bytes<?> bytes = new GuardedNativeBytes<>(Bytes.allocateDirect(SIZE), 10);
        //bytes.releaseLast();

        //bytes.bytesMethodReaderBuilder();
        bytes.write(-1, new byte[1], -1, 1);

        /*        bytes.copyTo(new OutputStream() {
            @Override
            public void write(int b) throws IOException {

            }
        });*/


        /*                .unchecked(true);*/
    }

}