/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.perf;

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
/**
 * Memory-mapped ping-pong benchmark because measuring inter-core latency
 * via CAS operations is essential for tuning CPU affinity in low-latency
 * applications.
 */
public class MMapPingPongMain {
    private static final boolean PONG = Jvm.getBoolean("pong");
    private static final boolean USE_AFFINITY = Jvm.getBoolean("useAffinity");

    public static void main(String[] args) throws FileNotFoundException {
        File tmpFile = new File(OS.getTmp(), "ping-pong-" + OS.getUserName() + ".tmp");
        tmpFile.deleteOnExit();
        int from = PONG ? 0 : 1;
        int to = PONG ? 1 : 0;
        final int count = 20_000_000;
        // CPU affinity: select core based on available processors
        int lastCPU = Runtime.getRuntime().availableProcessors() - 1;

        // Synchronisation: acquire optional affinity lock and mapped bytes for ping-pong
        try (AffinityLock ignored = USE_AFFINITY ? AffinityLock.acquireLock(PONG ? lastCPU : lastCPU / 2) : null;
             MappedBytes bytes = MappedBytes.mappedBytes(tmpFile, OS.pageSize())) {
            // Synchronisation: wait for peer to complete its initial CAS handshake
            while (!bytes.compareAndSwapLong(0, from, to))
                // Yield: spin waiting for peer CAS completion
                Thread.yield();
            // Benchmark progress: announce handshake completion
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
            assertNotNull(ignored, "Affinity lock handle should be retained for scope"); // keep compiler happy.
        }
    }
}
