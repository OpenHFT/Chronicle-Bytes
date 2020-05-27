package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IOTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConcurrentRafAccessTest {

    private static final String MODE = "rw";
    private static final String BASE_DIR = "rafs";
    private static final long INITIAL_LENGTH = 64L;
    private static final int RUNS = 64;
    private static final int NO_FILES = ForkJoinPool.commonPool().getPoolSize();

    private List<Worker> workers;

    @Before
    public void setup() {
        workers = IntStream.range(0, NO_FILES)
                .mapToObj(i -> {
                    final File file = fileFromInt(i);
                    try {
                        final RandomAccessFile raf = new RandomAccessFile(file, MODE);
                        raf.setLength(INITIAL_LENGTH);
                        final FileChannel fc = raf.getChannel();
                        return new Worker(file, raf, fc);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                        return null;
                    }

                })
                .collect(Collectors.toList());
    }

    @After
    public void cleanup() {
        IOTools.deleteDirWithFiles(BASE_DIR);
    }

    @Test
    public void testParallel2() {
        final LongSummaryStatistics summaryStatistics = IntStream.range(0, RUNS)
                .mapToLong(i -> test("testSequential" + i, ForkJoinPool.commonPool()))
                .skip(4)
                .summaryStatistics();

        System.out.println("testParallel2: " + summaryStatistics);
    }


    @Test
    public void testSequential() {
        final LongSummaryStatistics summaryStatistics = IntStream.range(0, RUNS)
                .mapToLong(i -> test("testSequential" + i, Executors.newSingleThreadExecutor()))
                .skip(4)
                .summaryStatistics();

        System.out.println("testSequential: " + summaryStatistics);

    }

    @Test
    public void testParallel() {
        final LongSummaryStatistics summaryStatistics = IntStream.range(0, RUNS)
                .mapToLong(i -> test("testSequential" + i, ForkJoinPool.commonPool()))
                .skip(4)
                .summaryStatistics();

        System.out.println("testParallel: " + summaryStatistics);
    }

    @Test
    public void testSequential2() {
        final LongSummaryStatistics summaryStatistics = IntStream.range(0, RUNS)
                .mapToLong(i -> test("testSequential" + i, Executors.newSingleThreadExecutor()))
                .skip(4)
                .summaryStatistics();

        System.out.println("testSequential2: " + summaryStatistics);

    }




    private long test(final String name, final ExecutorService executor) {
        final long beginNs = System.nanoTime();

        workers.forEach(executor::submit);
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        final long elapsedNs = System.nanoTime() - beginNs;
        System.out.format("%s: elapsedNs = %,d%n", name, elapsedNs);
        return elapsedNs;
    }


    private static final class Worker implements Runnable {

        private final File f;
        private final RandomAccessFile raf;
        private final FileChannel fc;

        public Worker(final File f, final RandomAccessFile raf, final FileChannel fc) {
            this.f = f;
            this.raf = raf;
            this.fc = fc;
        }

        @Override
        public void run() {
            final long beginNs = System.nanoTime();
            for (int i = 0; i < 24; i++) {
                try {
                    bumpSize(f, raf, fc);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
            final long elapsedNs = System.nanoTime() - beginNs;
            System.out.format("%s: elapsedNs = %,d%n", Thread.currentThread().getName(), elapsedNs);
        }

    }

    private static void bumpSize(File file, final RandomAccessFile raf, final FileChannel fc) throws IOException {
        final long currentSize = fc.size();
        raf.setLength(currentSize * 2);
    }

    private File fileFromInt(int i) {
        return new File(BASE_DIR + "/" + Integer.toString(i));
    }

}