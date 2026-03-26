/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@SuppressWarnings("OverlyStrongTypeCast")
class MappedBytesEdgeTest extends BytesTestCommon {
    private static final int CHUNK_SIZE = 262144;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(1, ReadWrite.PEEK, "peekUnsignedByte", (Consumer<Bytes<?>>) (StreamingDataInput::peekUnsignedByte)),
                Arguments.of(4, ReadWrite.READ, "readInt", (Consumer<Bytes<?>>) (StreamingDataInput::readInt)),
                Arguments.of(4, ReadWrite.READ, "readVolatileInt", (Consumer<Bytes<?>>) (StreamingDataInput::readVolatileInt)),
                Arguments.of(4, ReadWrite.PEEK, "peekVolatileInt", (Consumer<Bytes<?>>) (RandomDataInput::peekVolatileInt)),
                Arguments.of(8, ReadWrite.READ, "readLong", (Consumer<Bytes<?>>) (StreamingDataInput::readLong)),
                Arguments.of(8, ReadWrite.READ, "readDouble", (Consumer<Bytes<?>>) (StreamingDataInput::readDouble)),
                Arguments.of(256, ReadWrite.READ, "read(unmonitored(Bytes.allocateDirect(256)))", (Consumer<Bytes<?>>) (b -> b.read(unmonitored(Bytes.allocateDirect(256))))),
                Arguments.of(512, ReadWrite.READ, "read(ByteBuffer.allocate(512))", (Consumer<Bytes<?>>) (b -> b.read(ByteBuffer.allocate(512)))),
                Arguments.of(1024, ReadWrite.READ, "read(new byte[1024])", (Consumer<Bytes<?>>) (b -> b.read(new byte[1024]))),

                Arguments.of(1, ReadWrite.WRITE, "writeByte", (Consumer<Bytes<?>>) (b -> b.writeByte((byte) 99))),
                Arguments.of(2, ReadWrite.WRITE, "writeShort", (Consumer<Bytes<?>>) (b -> b.writeShort((short) 123))),
                Arguments.of(4, ReadWrite.WRITE, "writeInt", (Consumer<Bytes<?>>) (b -> b.writeInt(1234))),
                Arguments.of(4, ReadWrite.WRITE, "writeOrderedInt", (Consumer<Bytes<?>>) (b -> b.writeOrderedInt(1234))),
                Arguments.of(8, ReadWrite.WRITE, "writeLong", (Consumer<Bytes<?>>) (b -> b.writeLong(1234))),
                Arguments.of(8, ReadWrite.WRITE, "writeDouble", (Consumer<Bytes<?>>) (b -> b.writeDouble(1234))),
                Arguments.of(6, ReadWrite.WRITE, "write8bit", (Consumer<Bytes<?>>) (b -> b.write8bit("hello"))),
                Arguments.of(7, ReadWrite.WRITE, "appendUtf8", (Consumer<Bytes<?>>) (b -> b.appendUtf8("doggie"))),
                Arguments.of(10, ReadWrite.WRITE, "write", (Consumer<Bytes<?>>) (b -> b.write(Bytes.from("armadillo"))))
        );
    }

    private static Bytes<?> unmonitored(Bytes<?> bytes) {
        IOTools.unmonitor(bytes);
        return bytes;
    }

    @ParameterizedTest(name = "{2} size={0} rw={1}")
    @MethodSource("data")
    void testCorrectChunkResolved(int size, ReadWrite rw, String name, Consumer<Bytes<?>> doit) throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final File tempMBFile = Files.createTempFile("mapped", "bytes").toFile();
        final long overlap = OS.mapAlign(overlap(CHUNK_SIZE));
        if (overlap != overlap(CHUNK_SIZE)) {
            Jvm.warn().on(MappedBytesEdgeTest.class, "You configured an invalid overlap " + overlap(CHUNK_SIZE) + ", actual one used will be " + overlap);
        }
        try (final MappedBytes bytes = MappedBytes.mappedBytes(tempMBFile, CHUNK_SIZE, overlap)) {
            // map in the real file
            bytes.writeInt((3 * CHUNK_SIZE) - 4, 1);
            bytes.writePosition(0).writeByte((byte) 0);
            assertEquals(0, bytes.bytesStore().start());

            if (rw == ReadWrite.WRITE) {
                checkWritePosition(bytes, doit, size, CHUNK_SIZE + overlap - size, CHUNK_SIZE);
                checkWritePosition(bytes, doit, size, CHUNK_SIZE + overlap, CHUNK_SIZE);
                checkWritePosition(bytes, doit, size, CHUNK_SIZE + overlap + size, CHUNK_SIZE);
                // go back to just before the overlap - will still be in second chunk
                checkWritePosition(bytes, doit, size, CHUNK_SIZE + overlap - size, CHUNK_SIZE);
                checkWritePosition(bytes, doit, size, CHUNK_SIZE - size, 0);
                // now try and write over the end of the chunk
                checkWritePosition(bytes, doit, size, CHUNK_SIZE - 1, 0);
                // and end of chunk plus offset
                checkWritePosition(bytes, doit, size, CHUNK_SIZE + overlap - 1, CHUNK_SIZE);

                if (size > 1) {
                    // load the first chunk
                    checkWritePosition(bytes, doit, size, CHUNK_SIZE - 1, 0);

                    // write to just before the end of the third chunk
                    checkWritePosition(bytes, doit, size, (3 * CHUNK_SIZE) - 1, 2 * CHUNK_SIZE);

                    // we've got the third chunk loaded, now go back and write something to the end of the first chunk
                    checkWritePosition(bytes, doit, size, CHUNK_SIZE - 1, 0);
                }
            } else {
                // ensure WP is far ahead as Bytes generally won't allow a read past the WP
                bytes.writePosition(CHUNK_SIZE * 5);

                checkReadPosition(bytes, doit, size, CHUNK_SIZE - size, 0);
                checkReadPosition(bytes, doit, size, CHUNK_SIZE, 0);
                checkReadPosition(bytes, doit, size, CHUNK_SIZE + size, 0);
                checkReadPosition(bytes, doit, size, CHUNK_SIZE - size, 0);

                if (size > 1) {
                    // read over the end should work as we have the overlap
                    checkReadPosition(bytes, doit, size, CHUNK_SIZE - 1, 0);

                    // we've got the first chunk loaded, now read from just before the end of the third chunk
                    checkReadPosition(bytes, doit, size, (3 * CHUNK_SIZE) - 1, CHUNK_SIZE * 2);

                    // we've got the third chunk loaded, now read from just before the end of the first chunk
                    checkReadPosition(bytes, doit, size, CHUNK_SIZE - 1, 0);
                }
            }
        }
    }

    private long overlap(long chunkSize) {
        return chunkSize / 4;
    }

    private void checkWritePosition(MappedBytes bytes, Consumer<Bytes<?>> doit, int size, long writePosition, long expectedStart) {
        bytes.writePosition(writePosition);
        doit.accept(bytes);
        assertEquals(expectedStart, bytes.bytesStore().start());
    }

    private void checkReadPosition(MappedBytes bytes, Consumer<Bytes<?>> doit, int size, long readPosition, long expectedStart) {
        bytes.readPosition(readPosition);
        doit.accept(bytes);
        assertEquals(expectedStart, bytes.bytesStore().start());
    }

    protected enum ReadWrite {
        PEEK,
        READ,
        WRITE
    }
}
