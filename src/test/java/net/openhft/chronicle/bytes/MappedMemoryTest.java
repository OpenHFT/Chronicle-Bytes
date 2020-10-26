/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static net.openhft.chronicle.bytes.MappedBytes.mappedBytes;
import static net.openhft.chronicle.bytes.MappedFile.mappedFile;
import static org.junit.Assert.assertEquals;

public class MappedMemoryTest extends BytesTestCommon {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MappedMemoryTest.class);
    private static final long SHIFT = 27L;
    private static final long BLOCK_SIZE = 1L << SHIFT;

    @After
    public void checkRegisteredBytes() {
        AbstractReferenceCounted.assertReferencesReleased();
    }

    // on i7-3970X ~ 3.3 ns
    @Test
    public void testRawMemoryMapped() throws IOException {
        final ReferenceOwner test = ReferenceOwner.temporary("test");
        for (int t = 0; t < 5; t++) {
            final File tempFile = File.createTempFile("chronicle", "q");
            try {

                final long startTime = System.nanoTime();
                MappedFile file0;
                try (MappedFile mappedFile = mappedFile(tempFile, BLOCK_SIZE / 2, OS.pageSize())) {
                    file0 = mappedFile;
                    final MappedBytesStore bytesStore = mappedFile.acquireByteStore(test, 1);
                    final long address = bytesStore.address;

                    for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                        OS.memory().writeLong(address + i, i);
                    }
                    for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                        OS.memory().writeLong(address + i, i);
                    }
                    bytesStore.release(test);
                }
                assertEquals(file0.referenceCounts(), 0, file0.refCount());
                LOG.info("With RawMemory,\t\t time= " + 80 * (System.nanoTime() - startTime) / BLOCK_SIZE / 10.0 + " ns, number of longs written=" + BLOCK_SIZE / 8);
            } finally {
                deleteIfPossible(tempFile);
            }
        }
    }

    // on i7-3970X ~ 6.9 ns
    @Test
    public void withMappedNativeBytesTest() throws IOException {

        for (int t = 0; t < 3; t++) {
            final File tempFile = File.createTempFile("chronicle", "q");
            try {

                final long startTime = System.nanoTime();
                final Bytes<?> bytes = mappedBytes(tempFile, BLOCK_SIZE / 2);
//                bytes.writeLong(1, 1);
                for (long i = 0; i < BLOCK_SIZE; i += 8) {
                    bytes.writeLong(i);
                }
                bytes.releaseLast();
                assertEquals(0, bytes.refCount());
                LOG.info("With MappedNativeBytes,\t avg time= " + 80 * (System.nanoTime() - startTime) / BLOCK_SIZE / 10.0 + " ns, number of longs written=" + BLOCK_SIZE / 8);
            } finally {
                deleteIfPossible(tempFile);
            }
        }
    }

    // on i7-3970X ~ 6.0 ns
    @Test
    public void withRawNativeBytesTess() throws IOException {
        final ReferenceOwner test = ReferenceOwner.temporary("test");

        for (int t = 0; t < 3; t++) {
            final File tempFile = File.createTempFile("chronicle", "q");
            try {

                final long startTime = System.nanoTime();
                try (MappedFile mappedFile = mappedFile(tempFile, BLOCK_SIZE / 2, OS.pageSize())) {
                    Bytes<?> bytes = mappedFile.acquireBytesForWrite(test, 1);
                    for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                        bytes.writeLong(i);
                    }
                    bytes.releaseLast(test);

                    bytes = mappedFile.acquireBytesForWrite(test, BLOCK_SIZE / 2 + 1);
                    for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                        bytes.writeLong(i);
                    }
                    bytes.releaseLast(test);

                }
                LOG.info("With NativeBytes,\t\t time= " + 80 * (System.nanoTime() - startTime) / BLOCK_SIZE / 10.0 + " ns, number of longs written=" + BLOCK_SIZE / 8);
            } finally {
                deleteIfPossible(tempFile);
            }
        }
    }

    @Test
    public void mappedMemoryTest() throws IOException, IORuntimeException {

        final File tempFile = File.createTempFile("chronicle", "q");
        Bytes<?> bytes0;
        try {
            try (MappedBytes bytes = mappedBytes(tempFile, OS.pageSize())) {
                bytes0 = bytes;
                final ReferenceOwner test = ReferenceOwner.temporary("test");
                try {
                    assertEquals(1, bytes.refCount());
                    bytes.reserve(test);
                    assertEquals(2, bytes.refCount());
                    final char[] chars = new char[OS.pageSize() * 11];
                    Arrays.fill(chars, '.');
                    chars[chars.length - 1] = '*';
                    bytes.writeUtf8(new String(chars));
                    final String text = "hello this is some very long text";
                    bytes.writeUtf8(text);
                    final String textValue = bytes.toString();
                    assertEquals(text, textValue.substring(chars.length + 4));
                    assertEquals(2, bytes.refCount());
                } finally {
                    bytes.release(test);
                    assertEquals(1, bytes.refCount());
                }
            }
        } finally {
            deleteIfPossible(tempFile);
        }
        assertEquals(0, bytes0.refCount());
    }

    private static void deleteIfPossible(@NotNull final File file) {
        if (!file.delete()) {
            System.out.println("Unable to delete " + file.getAbsolutePath());
        }
    }


}