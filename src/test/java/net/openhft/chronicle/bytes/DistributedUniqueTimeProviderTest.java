package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.time.LongTime;
import net.openhft.chronicle.core.time.TimeProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

public class DistributedUniqueTimeProviderTest extends BytesTestCommon {

    static volatile long blackHole;

    @BeforeClass
    public static void checks() {
        try {
            System.setProperty("timestamp.dir", OS.getTarget());
            final File file = new File(DistributedUniqueTimeProvider.TIME_STAMP_PATH);
            file.delete();
            file.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(file)) {
            }
        } catch (Throwable ioe) {
            assumeNoException(ioe.getMessage(), ioe);
        }
    }

    @Test
    public void currentTimeMicros() {
        TimeProvider tp = DistributedUniqueTimeProvider.INSTANCE;
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
        TimeProvider tp = DistributedUniqueTimeProvider.INSTANCE;
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
        TimeProvider tp = DistributedUniqueTimeProvider.INSTANCE;
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
        TimeProvider tp = DistributedUniqueTimeProvider.INSTANCE;
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
    }

    @Test
    public void testMonotonicallyIncreasing() {
        TimeProvider tp = DistributedUniqueTimeProvider.INSTANCE;
        long last = 0;
        for (int i = 0; i < 10_000; i++) {
            long now = DistributedUniqueTimeProvider.timestampFor(tp.currentTimeNanos());
            assertTrue(now > last);
            last = now;
        }
    }
}