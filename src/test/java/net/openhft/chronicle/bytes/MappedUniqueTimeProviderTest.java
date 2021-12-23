package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.time.LongTime;
import net.openhft.chronicle.core.time.TimeProvider;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class MappedUniqueTimeProviderTest extends BytesTestCommon {

    @Test
    public void currentTimeMicros() {
        TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
        long last = 0;
        for (int i = 0; i < 100_000; i++) {
            long time = tp.currentTimeMicros();
            assertTrue(time > last);
            assertEquals(LongTime.toMicros(time), time);
            last = time;
        }
    }

    static volatile long blackHole;

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
        assertTrue(count > 1_000_000 / 2); // half the speed of Rasberry Pi
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
        assertTrue(count > 800_000 / 2); // half the speed of Rasberry Pi
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
        assertTrue(count > 230_000 / 2); // half the speed of Rasberry Pi
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
        assertTrue(count > 320_000 / 2); // half the speed of Rasberry Pi
    }

    @Test
    public void currentTimeNanos() {
        assumeTrue(null == System.getProperty("sonar.projectName"));

        TimeProvider tp = MappedUniqueTimeProvider.INSTANCE;
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
        assumeTrue(null == System.getProperty("sonar.projectName"));

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
                        if (!Jvm.isArm()) {
                            final long delay = now - (start + runTimeUS * 1000L);
                            if (delay > 128_000) { // very slow in Sonar
                                fail("Overran by " + delay + " ns.");
                            }
                        }
                        // check the times are different after shifting by 5 bits.
                        assertTrue((now >>> 5) > (last >>> 5));
                        last = now;
                    }
                });
        long time0 = System.nanoTime() - start0;
        System.out.printf("Time: %,d ms%n", time0 / 1_000_000);
        assertTrue(Jvm.isArm() || time0 < runTimeUS * 1000L);
    }
}