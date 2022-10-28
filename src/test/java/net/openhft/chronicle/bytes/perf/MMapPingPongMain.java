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
package net.openhft.chronicle.bytes.perf;

import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;

import java.io.File;
import java.io.FileNotFoundException;

public class MMapPingPongMain {
    static final boolean PONG = Jvm.getBoolean("pong");

    public static void main(String[] args) throws FileNotFoundException {
        File tmpFile = new File(OS.getTmp(), "ping-pong-" + OS.getUserName() + ".tmp");
        tmpFile.deleteOnExit();
        int from = PONG ? 0 : 1;
        int to = PONG ? 1 : 0;
        final int count = 20_000_000;
        try (MappedBytes bytes = MappedBytes.mappedBytes(tmpFile, OS.pageSize())) {
            // wait for the first one
            while (!bytes.compareAndSwapLong(0, from, to))
                Thread.yield();
            System.out.println("Started...");
            for (int t = 0; t < 5; t++) {
                long start = System.nanoTime();
                for (int i = 0; i < count; )
                    if (bytes.compareAndSwapLong(0, from, to))
                        i++;
                long time = System.nanoTime() - start;
                long rate = count * 1_000_000_000L / time;
                System.out.printf("Ping pong rate: %,d ping-pong/second%n", rate);
            }
        }
    }
}
