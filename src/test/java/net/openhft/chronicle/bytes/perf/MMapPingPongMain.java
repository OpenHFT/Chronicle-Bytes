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

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;

import java.io.File;
import java.io.FileNotFoundException;

/*
on the same Ryzen 9 5950X
Ping pong rate: 5,573,745 ping-pong/second
Ping pong rate: 5,588,202 ping-pong/second
Ping pong rate: 5,586,779 ping-pong/second
Ping pong rate: 5,584,569 ping-pong/second
Ping pong rate: 5,586,606 ping-pong/second

on the same CCX
Ping pong rate: 21,924,377 ping-pong/second
Ping pong rate: 21,724,836 ping-pong/second
Ping pong rate: 21,766,510 ping-pong/second
Ping pong rate: 21,747,684 ping-pong/second
Ping pong rate: 21,716,257 ping-pong/second

on the same CPU
Ping pong rate: 56,272,342 ping-pong/second
Ping pong rate: 52,262,765 ping-pong/second
Ping pong rate: 52,583,207 ping-pong/second
Ping pong rate: 52,590,468 ping-pong/second
Ping pong rate: 52,661,971 ping-pong/second
 */
public class MMapPingPongMain {
    static final boolean PONG = Jvm.getBoolean("pong");
    static final boolean USE_AFFINITY = Jvm.getBoolean("useAffinity");

    public static void main(String[] args) throws FileNotFoundException {
        File tmpFile = new File(OS.getTmp(), "ping-pong-" + OS.getUserName() + ".tmp");
        tmpFile.deleteOnExit();
        int from = PONG ? 0 : 1;
        int to = PONG ? 1 : 0;
        final int count = 20_000_000;
        int lastCPU = Runtime.getRuntime().availableProcessors() - 1;
        try (AffinityLock lock = USE_AFFINITY ? AffinityLock.acquireLock(PONG ? lastCPU : lastCPU / 2) : null;
             MappedBytes bytes = MappedBytes.mappedBytes(tmpFile, OS.pageSize())) {
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
