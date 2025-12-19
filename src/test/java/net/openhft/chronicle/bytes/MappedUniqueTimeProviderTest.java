/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.time.LongTime;
import net.openhft.chronicle.core.time.TimeProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class MappedUniqueTimeProviderTest extends BytesTestCommon {

    @SuppressWarnings("EmptyMethod")
    @BeforeEach
    @Override
    public void threadDump() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        super.threadDump();
    }

    @BeforeAll
    public static void checks() throws IOException {
        try {
            DistributedUniqueTimeProviderTest.checks();
        } catch (FileNotFoundException e) {
            if (!OS.isWindows())
                throw e;
        }
    }

    @Test
    public void currentTimeMicros() {
        TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
        long last = 0;
        for (int i = 0; i < 100_000; i++) {
            long time = tp.currentTimeMicros();
            assertTrue(time > last, "currentTimeMicros should return monotonically increasing values");
            assertEquals(LongTime.toMicros(time), time, "LongTime.toMicros");
            last = time;
        }
    }

    private static volatile long blackHole;

    @Test
    public void currentTimeMillisPerf() {
        long start = System.currentTimeMillis();
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = System.currentTimeMillis();
            count += 1000;
        } while (System.currentTimeMillis() < start + 500);
        System.out.println("currentTimeMillisPerf count/sec: " + count * 2);
        assertTrue(blackHole != 0L || count > 0, "blackHole must be updated");
        assertTrue(count > 1_000_000 / 2, "System.currentTimeMillis throughput should exceed 500K ops/sec (half Raspberry Pi speed)"); // half the speed of Rasberry Pi
    }

    @Test
    public void nanoTimePerf() {
        long start = System.currentTimeMillis();
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = System.nanoTime();
            count += 1000;
        } while (System.currentTimeMillis() < start + 500);
        System.out.println("nanoTimePerf count/sec: " + count * 2);
        assertTrue(blackHole != 0L || count > 0, "blackHole must be updated");
        assertTrue(count > 800_000 / 2, "System.nanoTime throughput should exceed 400K ops/sec (half Raspberry Pi speed)"); // half the speed of Rasberry Pi
    }

    @Test
    public void currentTimeMicrosPerf() {
        TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
        long start = System.currentTimeMillis();
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = tp.currentTimeMicros();
            count += 1000;
        } while (System.currentTimeMillis() < start + 500);
        System.out.println("currentTimeMicrosPerf count/sec: " + count * 2);
        assertTrue(blackHole != 0L || count > 0, "blackHole must be updated");
        assertTrue(count > 230_000 / 2, "currentTimeMicros throughput should exceed 115K ops/sec (half Raspberry Pi speed)"); // half the speed of Rasberry Pi
    }

    @Test
    public void currentTimeNanosPerf() {
        TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
        long start = System.currentTimeMillis();
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = tp.currentTimeNanos();
            count += 1000;
        } while (System.currentTimeMillis() < start + 500);
        System.out.println("currentTimeNanosPerf count/sec: " + count * 2);
        assertTrue(blackHole != 0L || count > 0, "blackHole must be updated");
        assertTrue(count > 320_000 / 2, "currentTimeNanos throughput should exceed 160K ops/sec (half Raspberry Pi speed)"); // half the speed of Rasberry Pi
    }

    @Test
    public void currentTimeNanos() {
        TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
        long start = tp.currentTimeNanos();
        long last = start;
        int count = 0;
        long runTime = Jvm.isArm() ? 3_000_000_000L : 500_000_000L;
        for (; ; ) {
            long now = tp.currentTimeNanos();
            assertEquals(LongTime.toNanos(now), now, "LongTime.toNanos");
            if (now > start + runTime)
                break;
            // check the times are different after shifting by 5 bits.
            assertTrue((now >>> 5) > (last >>> 5), "currentTimeNanos: assertTrue");
            last = now;
            count++;
            if (count >= 10_000_000)
                break;
        }
        System.out.printf("count: %,d%n", count);
        assertTrue(count > 1_000_000, "should generate over 1M unique timestamps within run time");
    }

    @Test
    public void concurrentTimeNanos() {
        long start0 = System.nanoTime();
        final int runTimeUS = 5_000_000;
        final int threads = Jvm.isArm() ? 4 : 16;
        final int stride = Jvm.isArm() ? 1 : threads;
        IntStream.range(0, threads)
                .parallel()
                .forEach(i -> {
                    TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
                    long last = tp.currentTimeNanos();
                    for (int j = 0; j < runTimeUS; j += stride) {
                        long now = tp.currentTimeNanos();
                        // check the times are different after shifting by 5 bits.
                        assertTrue((now >>> 5) > (last >>> 5), "concurrentTimeNanos: assertTrue");
                        last = now;
                    }
                });
        long time0 = System.nanoTime() - start0;
        System.out.printf("Time: %,d ms%n", time0 / 1_000_000);
        assertTrue(Jvm.isArm() || Jvm.isCodeCoverage()
                        || time0 < runTimeUS * 1000L,
                "Jvm.isCodeCoverage() = " + Jvm.isCodeCoverage());
    }
}
