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
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MappedFileMultiThreadTest extends BytesTestCommon {
    private static final int CORES = Integer.getInteger("cores", Runtime.getRuntime().availableProcessors());
    private static final int RUNTIME_MS = Integer.getInteger("runtimems", 2_000);
    private static final String TMP_FILE = System.getProperty("file", IOTools.createTempFile("testMultiThreadLock").getAbsolutePath());

    @Test
    public void testMultiThreadLock()
            throws Exception {
        final List<String> garbage = Collections.synchronizedList(new ArrayList<>());
        final long chunkSize = OS.isWindows() ? 64 << 10 : 4 << 10;
        try (MappedFile mf = MappedFile.mappedFile(TMP_FILE, chunkSize, 0)) {
            assertEquals("refCount: 1", mf.referenceCounts());

            final List<Future<?>> futures = new ArrayList<>();
            final ExecutorService es = Executors.newFixedThreadPool(CORES);
            final AtomicBoolean running = new AtomicBoolean(true);
            for (int i = 0; i < CORES; i++) {
                int finalI = i;
                futures.add(es.submit(() -> {
                    long offset = 1;
                    final ReferenceOwner test = ReferenceOwner.temporary("test" + finalI);
                    while (running.get()) {
                        garbage.add(test.referenceName() + offset);
                        MappedBytesStore bs = null;
                        Bytes<?> bytes = null;
                        try {
                            bs = mf.acquireByteStore(test, chunkSize * offset);
                            bytes = bs.bytesForRead();
                            assertNotNull(bytes.toString()); // show it doesn't blow up.
                            assertNotNull(bs.toString()); // show it doesn't blow up.
                            ++offset;
                        } catch (IOException e) {
                            throw Jvm.rethrow(e);
                        } finally {
                            if (bytes != null) bytes.releaseLast();
                            if (bs != null) bs.release(test);
                        }
                        if (finalI == 0 && offset % 1_000 == 0) {
                            garbage.clear();
                            System.gc();
                        }
                    }
                }));
            }

            Jvm.pause(RUNTIME_MS);
            running.set(false);
            for (Future<?> f : futures)
                f.get(5, TimeUnit.SECONDS);
            es.shutdownNow();
            es.awaitTermination(1, TimeUnit.SECONDS);
        }
        IOTools.deleteDirWithFiles(TMP_FILE);
    }
}