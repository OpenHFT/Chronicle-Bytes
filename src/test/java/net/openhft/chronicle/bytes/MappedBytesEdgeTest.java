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
import net.openhft.chronicle.core.OS;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MappedBytesEdgeTest extends BytesTestCommon {
    private static final int CHUNK_SIZE = 262144;
    private final int size;
    private final ReadWrite rw;
    private final Consumer<Bytes<?>> doit;

    public MappedBytesEdgeTest(int size, ReadWrite rw, Consumer<Bytes<?>> doit) {
        this.size = size;
        this.rw = rw;
        this.doit = doit;
    }

    @Parameterized.Parameters(name = "size {0} rw {1} lambda {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {1, ReadWrite.READ, (Consumer<Bytes<?>>) (StreamingDataInput::peekUnsignedByte)},
                {4, ReadWrite.READ, (Consumer<Bytes<?>>) (StreamingDataInput::readInt)},
                {4, ReadWrite.READ, (Consumer<Bytes<?>>) (StreamingDataInput::readVolatileInt)},
                {4, ReadWrite.READ, (Consumer<Bytes<?>>) (RandomDataInput::peekVolatileInt)},
                {8, ReadWrite.READ, (Consumer<Bytes<?>>) (StreamingDataInput::readLong)},
                {8, ReadWrite.READ, (Consumer<Bytes<?>>) (StreamingDataInput::readDouble)},
                {256, ReadWrite.READ, (Consumer<Bytes<?>>) (b -> b.read(Bytes.allocateDirect(256)))},
                {512, ReadWrite.READ, (Consumer<Bytes<?>>) (b -> b.read(ByteBuffer.allocate(512)))},
                {1024, ReadWrite.READ, (Consumer<Bytes<?>>) (b -> b.read(new byte[1024]))},

                {1, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.writeByte((byte) 99))},
                {2, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.writeShort((short) 123))},
                {4, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.writeInt(1234))},
                {4, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.writeOrderedInt(1234))},
                {8, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.writeLong(1234))},
                {8, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.writeDouble(1234))},
                {6, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.write8bit("hello"))},
                {7, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.appendUtf8("doggie"))},
                {10, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.write(Bytes.from("armadillo")))},
                // {1024, ReadWrite.WRITE, (Consumer<Bytes<?>>) (b -> b.write(new byte[1024]))}, // <-- this behaves differently, it won't start a write in the offset

        });
    }

    @Test
    public void testCorrectChunkResolved() throws IOException {
        final File tempMBFile = Files.createTempFile("mapped", "bytes").toFile();
        final long overlap = OS.mapAlign(overlap(CHUNK_SIZE));
        if (overlap != overlap(CHUNK_SIZE)) {
            Jvm.warn().on(MappedBytesEdgeTest.class, "You configured an invalid overlap " + overlap(CHUNK_SIZE) + ", actual one used will be " + overlap);
        }
        try (final MappedBytes bytes = MappedBytes.mappedBytes(tempMBFile, CHUNK_SIZE, overlap)) {
            // map in the real file
            bytes.writePosition(0).writeByte((byte) 0);
            assertEquals(0, bytes.bytesStore().start());

            if (rw == ReadWrite.WRITE) {
                checkWritePosition(bytes, CHUNK_SIZE + overlap - size, 0);
                checkWritePosition(bytes, CHUNK_SIZE + overlap, CHUNK_SIZE);
                checkWritePosition(bytes, CHUNK_SIZE + overlap + size, CHUNK_SIZE);
                // go back to just before the overlap - will still be in second chunk
                checkWritePosition(bytes, CHUNK_SIZE + overlap - size, CHUNK_SIZE);
                checkWritePosition(bytes, CHUNK_SIZE - size, 0);
                // now try and write over the end of the chunk
                checkWritePosition(bytes, CHUNK_SIZE - 1, 0);
                // and end of chunk plus offset
                checkWritePosition(bytes, CHUNK_SIZE + overlap - 1, size > 1 ? CHUNK_SIZE : 0);

                if (size > 1) {
                    // load the first chunk
                    checkWritePosition(bytes, CHUNK_SIZE - 1, 0);

                    // write to just before the end of the third chunk
                    checkWritePosition(bytes, (3 * CHUNK_SIZE) - 1, 2 * CHUNK_SIZE);

                    // we've got the third chunk loaded, now go back and write something to the end of the first chunk
                    checkWritePosition(bytes, CHUNK_SIZE - 1, 0);
                }
            } else {
                // ensure WP is far ahead as Bytes generally won't allow a read past the WP
                bytes.writePosition(CHUNK_SIZE * 5);

                checkReadPosition(bytes, CHUNK_SIZE - size, 0);
                checkReadPosition(bytes, CHUNK_SIZE, CHUNK_SIZE);
                checkReadPosition(bytes, CHUNK_SIZE + size, CHUNK_SIZE);
                checkReadPosition(bytes, CHUNK_SIZE - size, 0);

                if (size > 1) {
                    // read over the end should work as we have the overlap
                    checkReadPosition(bytes, CHUNK_SIZE - 1, 0);

                    // we've got the first chunk loaded, now read from just before the end of the third chunk
                    checkReadPosition(bytes, (3 * CHUNK_SIZE) - 1, CHUNK_SIZE * 2);

                    // we've got the third chunk loaded, now read from just before the end of the first chunk
                    checkReadPosition(bytes, CHUNK_SIZE - 1, 0);
                }
            }
        }
    }

    protected long overlap(long chunkSize) {
        return chunkSize / 4;
    }

    public int size() {
        return size;
    }

    private void checkWritePosition(MappedBytes bytes, long writePosition, long expectedStart) {
        bytes.writePosition(writePosition);
        doit.accept(bytes);
        assertEquals(expectedStart, bytes.bytesStore().start());
    }

    private void checkReadPosition(MappedBytes bytes, long readPosition, long expectedStart) {
        bytes.readPosition(readPosition);
        doit.accept(bytes);
        assertEquals(expectedStart, bytes.bytesStore().start());
    }

    protected enum ReadWrite {
        READ,
        WRITE
    }
}
