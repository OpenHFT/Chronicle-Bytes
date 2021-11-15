package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.*;
import static net.openhft.chronicle.bytes.BytesStore.wrap;
import static org.junit.jupiter.api.Assertions.fail;

final class ReproduceBuildProblemTest extends BytesTestCommon {

    private static final AtomicInteger CNT = new AtomicInteger();

    @Test
    void reproduce() {

        BytesFactoryUtil.provideBytesObjects()
                .forEach(args -> {
                    final Bytes<Object> bytes = bytes(args);
                    try {
                        System.out.println("Using " + createCommand(args) + " -> " + bytes.getClass().getName());
                        System.out.flush();
                        System.err.flush();
                        if (isReadWrite(args)) {
                            bytes.write("A");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.flush();
                        System.err.flush();
                        fail(e);
                    } finally {
                        bytes.releaseLast();
                    }

                });

    }

    void reproduce2() {
        final ByteBuffer heapByteBuffer = ByteBuffer.allocate(SIZE);
        final ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(SIZE);

        try {
            final File file = create(File.createTempFile("mapped-file" + CNT.getAndIncrement(), "bin"), SIZE);
            file.deleteOnExit();
            final File fileRo = create(File.createTempFile("mapped-file-ro" + CNT.getAndIncrement(), "bin"), SIZE);
            fileRo.deleteOnExit();
            final File singleFile = create(File.createTempFile("single-mapped-file" + CNT.getAndIncrement(), "bin"), SIZE);
            singleFile.deleteOnExit();
            final File singleFileRo = create(File.createTempFile("single-mapped-file-ro" + CNT.getAndIncrement(), "bin"), SIZE);
            singleFileRo.deleteOnExit();
    
            Arguments.of(Bytes.allocateDirect(SIZE), true, "Bytes.allocateDirect(SIZE)");
            Arguments.of(Bytes.allocateElasticOnHeap(SIZE), true, "Bytes.allocateElasticOnHeap(SIZE)");
            Arguments.of(wipe(Bytes.allocateElasticDirect()), true, "Bytes.allocateElasticDirect()");
            Arguments.of(Bytes.wrapForWrite(heapByteBuffer), true, "Bytes.wrapForWrite(heapByteBuffer)");
            Arguments.of(Bytes.wrapForWrite(directByteBuffer), true, "Bytes.wrapForWrite(directByteBuffer)");
            Arguments.of(MappedBytes.mappedBytes(file, CHUNK_SIZE), true, "MappedBytes.mappedBytes(file, SIZE)");
            Arguments.of(MappedBytes.mappedBytes(fileRo, CHUNK_SIZE, 0, true), false, "MappedBytes.mappedBytes(fileRo, SIZE, 0, true)");
            Arguments.of(MappedBytes.singleMappedBytes(singleFile, CHUNK_SIZE), true, "MappedBytes.singleMappedBytes(singleFile, SIZE)");
            Arguments.of(MappedBytes.singleMappedBytes(singleFileRo, CHUNK_SIZE, true), false, "MappedBytes.singleMappedBytes(singleFileRo, SIZE, true)");

            // Todo: reactivate this one once https://github.com/OpenHFT/Chronicle-Bytes/issues/254 is fixed
            // Arguments.of(Bytes.forFieldGroup(holder, "b"), true, "Bytes.forFieldGroup(holder, \"b\")"),

            Arguments.of(new UncheckedBytes<>(Bytes.allocateDirect(SIZE)), true, "new UncheckedBytes<>(Bytes.allocateDirect(SIZE))");
            Arguments.of(new UncheckedBytes<>(Bytes.wrapForWrite(new byte[SIZE])), true, "new UncheckedBytes<>(Bytes.wrapForWrite(new byte[SIZE]))");
            Arguments.of(new HexDumpBytes(), true, "new HexDumpBytes()");
            Arguments.of(Bytes.allocateDirect(SIZE).unchecked(true), true, "Bytes.allocateDirect(SIZE).unchecked(true)");
            Arguments.of(Bytes.allocateElasticOnHeap(SIZE).unchecked(true), true, "Bytes.allocateElasticOnHeap(SIZE).unchecked(true)");
            Arguments.of(new GuardedNativeBytes<>(wrap(ByteBuffer.allocate(SIZE)), SIZE), true, "new GuardedNativeBytes<>(wrap(ByteBuffer.allocate(SIZE))");

        } catch (IOException ioException) {
            System.out.flush();
            System.err.flush();
            ioException.printStackTrace();
            throw new AssertionError("Unable to create Bytes", ioException);
        }

    }


}