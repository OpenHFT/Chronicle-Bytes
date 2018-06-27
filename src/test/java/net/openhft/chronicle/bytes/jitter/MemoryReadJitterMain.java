package net.openhft.chronicle.bytes.jitter;

import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.util.Histogram;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class MemoryReadJitterMain {
    public static final String PROFILE_OF_THE_THREAD = "profile of the thread";

    static int runTime = Integer.getInteger("runTime", 600); // seconds
    static int size = Integer.getInteger("size", 128); // bytes
    static int padTo = Integer.getInteger("pad", 0); // bytes
    static int sampleTime = Integer.getInteger("sampleTime", 2); // micro-seconds
    static int throughput = Integer.getInteger("throughput", 20_000); // per second
    static volatile boolean running = true;

    static {
        System.setProperty("jvm.safepoint.enabled", "true");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
    }

    public static void main(String[] args) throws IOException {
        MappedFile.warmup();

        String path = OS.TMP + "/test-mem-" + System.nanoTime();
        System.out.println("Writing to " + path);
        AtomicLong lastRead = new AtomicLong();
        File file = new File(path);
        file.deleteOnExit();

        final Histogram histoRead = new Histogram();
        final Histogram histoWrite = new Histogram();
        final Histogram histoReadWrite = new Histogram();

        Thread reader = new Thread(() -> {
            try {
                MappedBytes mf = MappedBytes.mappedBytes(file, 64 << 10);
                mf.readLimit(mf.writeLimit());
                MemoryMessager mm = new MemoryMessager(mf, padTo);
                boolean found = false;
                while (running) {
                    if (found)
                        Jvm.safepoint();
                    else
                        Jvm.safepoint();
                    int length = mm.length();
                    if (length == 0x0 || length == MemoryMessager.NOT_READY) {
                        found = false;
                        Jvm.safepoint();
                        length = mm.length();
                        if (length == 0x0 || length == MemoryMessager.NOT_READY) {
                            Jvm.safepoint();
                            continue;
                        }
                    }
                    long startTimeNs = System.nanoTime();
                    Jvm.safepoint();
                    long last = mm.consumeBytes();
                    if (found)
                        Jvm.safepoint();
                    else
                        Jvm.safepoint();
                    long now = System.nanoTime();
                    histoRead.sampleNanos(now - startTimeNs);
                    histoReadWrite.sampleNanos(now - mm.firstLong());
                    lastRead.lazySet(last);
                    if (found)
                        Jvm.safepoint();
                    else
                        Jvm.safepoint();
                    found = true;
                }
                mf.release();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        reader.setDaemon(true);
        reader.start();
        Jvm.pause(100); // give it time to start

        long count = 0;
        MappedBytes mf = MappedBytes.mappedBytes(file, 64 << 10);
        MemoryMessager mm = new MemoryMessager(mf, padTo);
        long start0 = System.currentTimeMillis();
        int sampleNS = sampleTime * 1000;
        int intervalNS = (int) (1e9 / throughput);
        int subSampler = 0;
        do {
            long startTimeNs = System.nanoTime();
            mm.writeMessage(size, ++count, startTimeNs);
            histoWrite.sampleNanos(System.nanoTime() - startTimeNs);
            long start1 = System.nanoTime();
            while (System.nanoTime() < start1 + sampleNS) {
                // wait one micro-second.
            }
            if (lastRead.get() != count) {
                StackTraceElement[] stes = reader.getStackTrace();
                if (lastRead.get() != count || ++subSampler > 100) { // 1% of race condition samples arbitrarily chosen.
                    StringBuilder sb = new StringBuilder();
                    sb.append(PROFILE_OF_THE_THREAD);
                    Jvm.trimStackTrace(sb, stes);
                    System.out.println(sb);
                    subSampler = 0;
                }
            }
            while (System.nanoTime() < start1 + intervalNS) {
                Thread.yield();
            }

        } while (System.currentTimeMillis() < start0 + runTime * 1_000);
        running = false;
        mf.release();
        System.gc();// give it time to release the file so the delete on exit will work on windows.

        System.out.println("size="+size+" padTo="+padTo);
        System.out.println("histoRead     ="+histoRead.toMicrosFormat());
        System.out.println("histoWrite    ="+histoWrite.toMicrosFormat());
        System.out.println("histoReadWrite="+histoReadWrite.toMicrosFormat());
    }
}
