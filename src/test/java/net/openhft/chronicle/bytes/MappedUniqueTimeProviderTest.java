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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests MappedUniqueTimeProvider performance and ordering guarantees
 * because monotonically increasing timestamps are essential for event
 * sequencing in distributed systems.
 */
@DisplayName("Mapped unique time provider performance and ordering")
public class MappedUniqueTimeProviderTest extends BytesTestCommon {

    @SuppressWarnings("EmptyMethod")
    @BeforeEach
    public void threadDump() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for time provider tests");

        super.threadDump();
    }

    @BeforeAll
    public static void checks() throws IOException {
        try {
            DistributedUniqueTimeProviderTest.checks();
        } catch (FileNotFoundException e) {
            // Windows may fail to access mapped time file due to file locking restrictions
            if (!OS.isWindows()) {
                // Non-Windows: propagate exception because file should be accessible
                throw e;
            }
        }
    }

    @Test
    @DisplayName("currentTimeMicros increases monotonically under heavy load")
    public void currentTimeMicros() {
        TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
        long last = 0;
        for (int i = 0; i < 100_000; i++) {
            long time = tp.currentTimeMicros();
            assertTrue(time > last,
                    "Micros time increases on iteration " + i);
            assertEquals(LongTime.toMicros(time), time,
                    "Micros conversion matches on iteration " + i);
            last = time;
        }
    }

    private static volatile long blackHole;

    @Test
    @DisplayName("currentTimeMillis throughput meets baseline threshold requirement")
    public void currentTimeMillisPerf() {
        long start = System.currentTimeMillis();
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = System.currentTimeMillis();
            count += 1000;
        } while (System.currentTimeMillis() < start + 500);
        System.out.println("currentTimeMillisPerf count/sec: " + count * 2);
        assertTrue(count > 1_000_000 / 2,
                "currentTimeMillis count exceeds baseline threshold"); // half the speed of Rasberry Pi
        assertTrue(blackHole > 0,
                "Black hole value " + blackHole + " should be > 0 for millis timestamp");
    }

    @Test
    @DisplayName("nanoTime throughput meets baseline threshold requirement")
    public void nanoTimePerf() {
        long start = System.currentTimeMillis();
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = System.nanoTime();
            count += 1000;
        } while (System.currentTimeMillis() < start + 500);
        System.out.println("nanoTimePerf count/sec: " + count * 2);
        assertTrue(count > 800_000 / 2,
                "nanoTime count exceeds baseline threshold"); // half the speed of Rasberry Pi
        assertTrue(blackHole > 0,
                "Black hole value " + blackHole + " should be > 0 for nano timestamp");
    }

    @Test
    @DisplayName("currentTimeMicros throughput meets baseline threshold requirement")
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
        assertTrue(count > 230_000 / 2,
                "currentTimeMicros count exceeds baseline threshold"); // half the speed of Rasberry Pi
        assertTrue(blackHole > 0,
                "Black hole value " + blackHole + " should be > 0 for micros timestamp");
    }

    @Test
    @DisplayName("currentTimeNanos throughput meets baseline threshold requirement")
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
        assertTrue(count > 320_000 / 2,
                "currentTimeNanos count exceeds baseline threshold"); // half the speed of Rasberry Pi
        assertTrue(blackHole > 0,
                "Black hole value " + blackHole + " should be > 0 for nanos timestamp");
    }

    @Test
    @DisplayName("currentTimeNanos increases with shifted ordering checks")
    public void currentTimeNanos() {
        TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
        long start = tp.currentTimeNanos();
        long last = start;
        int count = 0;
        long runTime = Jvm.isArm() ? 3_000_000_000L : 500_000_000L;
        for (; ; ) {
            long now = tp.currentTimeNanos();
            assertEquals(LongTime.toNanos(now), now,
                    "Nanos conversion matches on count " + count);
            if (now > start + runTime)
                break;
            // check the times are different after shifting by 5 bits.
            assertTrue((now >>> 5) > (last >>> 5),
                    "Shifted nanos increases on count " + count);
            last = now;
            count++;
            if (count >= 10_000_000)
                break;
        }
        System.out.printf("count: %,d%n", count);
        assertTrue(count > 1_000_000,
                "Nanos loop count " + count + " exceeds minimum " + 1_000_000);
    }

    @Test
    @DisplayName("currentTimeNanos increases across concurrent threads consistently")
    public void concurrentTimeNanos() {
        long start0 = System.nanoTime();
        final int runTimeUS = 5_000_000;
        final int threads = Jvm.isArm() ? 4 : 16;
        final int stride = Jvm.isArm() ? 1 : threads;
        IntStream.range(0, threads)
                .parallel()
                .forEach(i -> {
                    TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
                    long start = tp.currentTimeNanos();
                    long last = start;
                    for (int j = 0; j < runTimeUS; j += stride) {
                        long now = tp.currentTimeNanos();
                        // check the times are different after shifting by 5 bits.
                        assertTrue((now >>> 5) > (last >>> 5),
                                "Shifted nanos increases for thread " + i + " at step " + j);
                        last = now;
                    }
                });
        long time0 = System.nanoTime() - start0;
        System.out.printf("Time: %,d ms%n", time0 / 1_000_000);
        assertTrue(Jvm.isArm() || Jvm.isCodeCoverage()
                                || time0 < runTimeUS * 1000L,
                "Timing check respects coverage mode, Jvm.isCodeCoverage() = " + Jvm.isCodeCoverage());
    }
}
