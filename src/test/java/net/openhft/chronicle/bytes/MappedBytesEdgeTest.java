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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MappedBytesEdgeTest extends BytesTestCommon {
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
                {1, ReadWrite.READ, (Consumer<Bytes<?>>)(StreamingDataInput::peekUnsignedByte) },
                {4, ReadWrite.READ, (Consumer<Bytes<?>>)(StreamingDataInput::readInt) },
                {4, ReadWrite.READ, (Consumer<Bytes<?>>)(StreamingDataInput::readVolatileInt) },
                {4, ReadWrite.READ, (Consumer<Bytes<?>>)(b -> b.peekVolatileInt()) },
                {8, ReadWrite.READ, (Consumer<Bytes<?>>)(StreamingDataInput::readLong) },
                {8, ReadWrite.READ, (Consumer<Bytes<?>>)(StreamingDataInput::readDouble) },

                {1, ReadWrite.WRITE, (Consumer<Bytes<?>>)(b -> b.writeByte((byte) 99)) },
                {2, ReadWrite.WRITE, (Consumer<Bytes<?>>)(b -> b.writeShort((short) 123)) },
                {4, ReadWrite.WRITE, (Consumer<Bytes<?>>)(b -> b.writeInt(1234)) },
                {4, ReadWrite.WRITE, (Consumer<Bytes<?>>)(b -> b.writeOrderedInt(1234)) },
                {8, ReadWrite.WRITE, (Consumer<Bytes<?>>)(b -> b.writeLong(1234)) },
                {8, ReadWrite.WRITE, (Consumer<Bytes<?>>)(b -> b.writeDouble(1234)) },
                {6, ReadWrite.WRITE, (Consumer<Bytes<?>>)(b -> b.write8bit("hello")) },
                {7, ReadWrite.WRITE, (Consumer<Bytes<?>>)(b -> b.appendUtf8("doggie")) },
                {10, ReadWrite.WRITE, (Consumer<Bytes<?>>)(b -> b.write(Bytes.from("armadillo"))) },

        });
    }

    @Test
    public void testCorrectChunkResolved() throws IOException {
        final File tempMBFile = Files.createTempFile("mapped", "bytes").toFile();
        int chunk = 262144;
        int overlap = 65536;
        try (final MappedBytes bytes = MappedBytes.mappedBytes(tempMBFile, chunk, overlap)) {
            // map in the real file
            bytes.writePosition(0).writeByte((byte) 0);
            assertEquals(0, bytes.bytesStore().start());

            if (rw == ReadWrite.WRITE) {
                checkWritePosition(bytes, chunk + overlap - size, 0);
                checkWritePosition(bytes, chunk + overlap, chunk);
                checkWritePosition(bytes, chunk + overlap + size, chunk);
                // go back to just before the overlap - will still be in second chunk
                checkWritePosition(bytes, chunk + overlap - size, chunk);
                checkWritePosition(bytes, chunk - size, 0);
                if (size > 1) {
                    // now try and write over the end of the chunk
                    bytes.writePosition(chunk - 1);
                    doit.accept(bytes);
                    // and end of chunk plus offset
                    bytes.writePosition(chunk + overlap - 1);
                    doit.accept(bytes);
                }
            } else {
                // ensure WP is far ahead as Bytes generally won't allow a read past the WP
                bytes.writePosition(chunk + 1_000);
                checkReadPosition(bytes, chunk - size, 0);
                checkReadPosition(bytes, chunk, chunk);
                checkReadPosition(bytes, chunk + size, chunk);
                checkReadPosition(bytes, chunk - size, 0);
                // read over the end should work as we have the overlap
                if (size > 1) {
                    bytes.readPosition(chunk - 1);
                    doit.accept(bytes);
                }
            }
        }
    }

    private void checkWritePosition(MappedBytes bytes, int writePosition, int expectedStart) {
        bytes.writePosition(writePosition);
        doit.accept(bytes);
        assertEquals(expectedStart, bytes.bytesStore().start());
    }

    private void checkReadPosition(MappedBytes bytes, int readPosition, int expectedStart) {
        bytes.readPosition(readPosition);
        doit.accept(bytes);
        assertEquals(expectedStart, bytes.bytesStore().start());
    }

    private enum ReadWrite {
        READ,
        WRITE
    }
}
