/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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
import net.openhft.chronicle.core.time.LongTime;
import net.openhft.chronicle.core.time.TimeProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DistributedUniqueTimeProviderTest extends BytesTestCommon {

    static volatile long blackHole;

    @BeforeClass
    public static void checks() throws IOException {
        System.setProperty("timestamp.dir", OS.getTarget());
        final File file = new File(BytesUtil.TIME_STAMP_PATH);
        deleteIfPossible(file);
        file.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(file)) {
        }
    }

    @Test
    public void currentTimeMicros() {
        TimeProvider tp = DistributedUniqueTimeProvider.instance();
        long last = 0;
        for (int i = 0; i < 100_000; i++) {
            long time = tp.currentTimeMicros();
            assertTrue(time > last);
            assertEquals(LongTime.toMicros(time), time);
            last = time;
        }
    }

    @Test
    public void currentTimeMicrosPerf() {
        TimeProvider tp = DistributedUniqueTimeProvider.instance();
        long start = System.currentTimeMillis();
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = tp.currentTimeMicros();
            count += 1000;
        } while (System.currentTimeMillis() < start + 500);
        System.out.println("currentTimeMicrosPerf count/sec: " + count * 2);
        assertTrue(count > 128_000 / 2); // half the speed of Rasberry Pi
    }

    @Test
    public void currentTimeNanosPerf() {
        TimeProvider tp = DistributedUniqueTimeProvider.instance();
        long start = System.currentTimeMillis();
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = tp.currentTimeNanos();
            count += 1000;
        } while (System.currentTimeMillis() < start + 500);
        System.out.println("currentTimeNanosPerf count/sec: " + count * 2);
        assertTrue(count > 202_000 / 2); // half the speed of Rasberry Pi
    }

    @Test
    public void currentTimeNanos() {
        TimeProvider tp = DistributedUniqueTimeProvider.instance();
        long start = tp.currentTimeNanos();
        long last = start;
        int count = 0;
        long runTime = Jvm.isArm() ? 3_000_000_000L : 500_000_000L;
        for (; ; ) {
            long now = tp.currentTimeNanos();
            assertEquals(LongTime.toNanos(now), now);
            if (now > start + runTime)
                break;
            // check the times are different after shifting by 5 bits.
            assertTrue((now >>> 5) > (last >>> 5));
            last = now;
            count++;
            if (count >= 10_000_000)
                break;
        }
        System.out.printf("count: %,d%n", count);
        assertTrue(count > 1_000_000);
    }

    @Test
    public void concurrentTimeNanos() {
        finishedNormally = false;
        long start0 = System.nanoTime();
        final int runTimeUS = 5_000_000;
        final int threads = Jvm.isArm() ? 4 : 16;
        final int stride = Jvm.isArm() ? 1 : threads;
        IntStream.range(0, threads)
                .parallel()
                .forEach(i -> {
                    try (DistributedUniqueTimeProvider tp = DistributedUniqueTimeProvider.forHostId(i)) {
                        long last = 0;
                        for (int j = 0; j < runTimeUS; j += stride) {
                            long now = tp.currentTimeNanos();
                            assertEquals(i, DistributedUniqueTimeProvider.hostIdFor(now));
                            assertTrue(now > last);
                            last = now;
                        }
                    }
                });
        long time0 = System.nanoTime() - start0;
        System.out.printf("Time: %,d ms%n", time0 / 1_000_000);
        assertTrue(Jvm.isArm() || Jvm.isCodeCoverage()
                || time0 < runTimeUS * 1000L);
        finishedNormally = true;
    }

    @Test
    public void testMonotonicallyIncreasing() {
        TimeProvider tp = DistributedUniqueTimeProvider.instance();
        long last = 0;
        for (int i = 0; i < 10_000; i++) {
            long now = DistributedUniqueTimeProvider.timestampFor(tp.currentTimeNanos());
            assertTrue(now > last);
            last = now;
        }
    }
}