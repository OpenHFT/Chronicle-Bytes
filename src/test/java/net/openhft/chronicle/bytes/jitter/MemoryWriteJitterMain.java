package net.openhft.chronicle.bytes.jitter;

import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;

import java.io.File;
import java.io.IOException;

public class MemoryWriteJitterMain {
    public static final String PROFILE_OF_THE_THREAD = "profile of the thread";

    static int runTime = Integer.getInteger("runTime", 600); // seconds
    static int size = Integer.getInteger("size", 128); // bytes
    static int sampleTime = Integer.getInteger("sampleTime", 2); // micro-seconds
    static int throughput = Integer.getInteger("throughput", 20_000); // per second
    static volatile boolean running = true;
    static volatile boolean writing = false;
    static volatile int count = 0;

    static {
        System.setProperty("jvm.safepoint.enabled", "true");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
    }

    public static void main(String[] args) throws IOException {
        MappedFile.warmup();

        String path = OS.TMP + "/test-mem-" + System.nanoTime();
        System.out.println("Writing to " + path);

        File file = new File(path);
        file.deleteOnExit();

        Thread writer = new Thread(() -> {
            try {
                MappedBytes mf = MappedBytes.mappedBytes(file, 1 << 20);
                MemoryMessager mm = new MemoryMessager(mf);
                int intervalNS = (int) (1e9 / throughput);
                while (running) {
                    writing = true;
                    mm.writeMessage(size, ++count);
                    writing = false;
                    long start = System.nanoTime();
                    Thread.yield();
                    //noinspection StatementWithEmptyBody
                    while (System.nanoTime() < start + intervalNS) ;
                }
                mf.release();
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(-1);
            }
        });
        writer.setDaemon(true);
        writer.start();

        MappedBytes mf = MappedBytes.mappedBytes(file, 1 << 20);
        mf.readLimit(mf.writeLimit());
        MemoryMessager mm = new MemoryMessager(mf);

        long start0 = System.currentTimeMillis();
        int sampleNS = sampleTime * 1000;
        do {
            if (writing) {
                long start1 = System.nanoTime();
                while (System.nanoTime() < start1 + sampleNS) {
                    // wait one micro-second.
                }
                if (writing) {
                    StackTraceElement[] stes = writer.getStackTrace();
                    if (writing) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(PROFILE_OF_THE_THREAD);
                        Jvm.trimStackTrace(sb, stes);
                        if (sb.indexOf("MemoryWriteJitterMain.java:47") < 0
                                || sb.indexOf("MemoryWriteJitterMain.java:49") < 0)
                            System.out.println(sb);
                    }
                }
            }
            int length = mm.length();
            if (length > 0x0) {
                mm.consumeBytes();
            }

        } while (System.currentTimeMillis() < start0 + runTime * 1_000);
        running = false;
        mf.release();
        System.gc();// give it time to release the file so the delete on exit will work on windows.
    }
}
