/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.jitter;

import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.core.util.Time;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class MemoryReadJitterMain {
    private static final String PROFILE_OF_THE_THREAD = "profile of the thread";

    private static final int runTime = Integer.getInteger("runTime", 600); // seconds
    private static final int size = Integer.getInteger("size", 128); // bytes
    private static final int padTo = Integer.getInteger("pad", 0); // bytes
    private static final int sampleTime = Integer.getInteger("sampleTime", 2); // micro-seconds
    private static final int throughput = Integer.getInteger("throughput", 20_000); // per second
    private static volatile boolean running = true;

    static {
        System.setProperty("jvm.safepoint.enabled", "true");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
    }

    public static void main(String[] args)
            throws IOException {
        MappedFile.warmup();

        String path = "test-mem-" + Time.uniqueId();
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
                while (running) {
                    Jvm.safepoint();
                    int length = mm.length();
                    if (length == 0x0 || length == MemoryMessager.NOT_READY) {
                        Jvm.safepoint();
                        length = mm.length();
                        if (length == 0x0 || length == MemoryMessager.NOT_READY) {
                            Jvm.safepoint();
                            continue;
                        }
                    }
                    long readDurationNs = consumeAndMeasure(mm, lastRead);
                    long now = System.nanoTime();
                    histoRead.sampleNanos(readDurationNs);
                    histoReadWrite.sampleNanos(now - mm.firstLong());
                    Jvm.safepoint();
                }
                mf.releaseLast();
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
                Jvm.safepoint();
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
        } while (System.currentTimeMillis() < start0 + runTime * 1_000L);
        running = false;
        mf.releaseLast();
        System.gc();// give it time to release the file so the delete on exit will work on windows.

        System.out.println("size=" + size + " padTo=" + padTo);
        System.out.println("histoRead     =" + histoRead.toMicrosFormat());
        System.out.println("histoWrite    =" + histoWrite.toMicrosFormat());
        System.out.println("histoReadWrite=" + histoReadWrite.toMicrosFormat());
    }

    private static long consumeAndMeasure(MemoryMessager mm, java.util.concurrent.atomic.AtomicLong lastRead) {
        long start = System.nanoTime();
        Jvm.safepoint();
        long value = mm.consumeBytes();
        lastRead.lazySet(value);
        return System.nanoTime() - start;
    }
}
