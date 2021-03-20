package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IOTools;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.fail;

/*
    Averages from TeamCity logs:

    Type.        Linux [us]   Windows [us]   ARM [us]
    ===========================================================
    Sequential   15           49             120
    Parallel      3.5         15              51
*/

@Ignore("This is a performance test and should not be run as a part of the normal build")
public class ConcurrentRafAccessTest extends BytesTestCommon {

    private static final String MODE = "rw";
    private static final String BASE_DIR = "rafs";
    private static final long INITIAL_LENGTH = 64L;
    private static final int RUNS = 64;
    private static final int NO_FILES = ForkJoinPool.commonPool().getPoolSize();

    private List<Worker> workers;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private static void bumpSize(File file, final RandomAccessFile raf, final FileChannel fc)
            throws IOException {
        final long currentSize = fc.size();
        raf.setLength(currentSize * 2);
    }

    @Before
    public void setup()
            throws IOException {
        Files.createDirectories(Paths.get(BASE_DIR));

        workers = IntStream.range(0, NO_FILES)
                .mapToObj(i -> {
                    try {
                        final File file = fileFromInt(i);
                        final RandomAccessFile raf = new RandomAccessFile(file, MODE);
                        raf.setLength(INITIAL_LENGTH);
                        final FileChannel fc = raf.getChannel();
                        return new Worker(file, raf, fc);
                    } catch (IOException e) {
                        e.printStackTrace();
                        fail("unable to create file for " + i);
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
                .mapToLong(i -> test("testParallel2 " + i, ForkJoinPool.commonPool()))
                .skip(4)
                .summaryStatistics();

//        System.out.println("testParallel2: " + summaryStatistics);
    }

    @Test
    public void testSequential() {
        final LongSummaryStatistics summaryStatistics = IntStream.range(0, RUNS)
                .mapToLong(i -> test("testSequential " + i, Executors.newSingleThreadExecutor()))
                .skip(4)
                .summaryStatistics();

//        System.out.println("testSequential: " + summaryStatistics);

    }

    @Test
    public void testParallel() {
        final LongSummaryStatistics summaryStatistics = IntStream.range(0, RUNS)
                .mapToLong(i -> test("testParallel " + i, ForkJoinPool.commonPool()))
                .skip(4)
                .summaryStatistics();

//        System.out.println("testParallel: " + summaryStatistics);
    }

    @Test
    public void testSequential2() {
        final LongSummaryStatistics summaryStatistics = IntStream.range(0, RUNS)
                .mapToLong(i -> test("testSequential2 " + i, Executors.newSingleThreadExecutor()))
                .skip(4)
                .summaryStatistics();

//        System.out.println("testSequential2: " + summaryStatistics);

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
//        System.out.format("%s: elapsedNs = %,d%n", name, elapsedNs);
        return elapsedNs;
    }

    private File fileFromInt(int i)
            throws IOException {
        return tmpDir.newFile(Integer.toString(i));
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
//            System.out.format("%s: elapsedNs = %,d%n", Thread.currentThread().getName(), elapsedNs);
        }
    }
}