package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;

public class ContentEqualsJLBHTest {

    // to use vectorizedMismatch you should run on java 11 or later, with the following VM args
    // --illegal-access=permit --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-exports java.base/jdk.internal.util=ALL-UNNAMED

    static {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(false);
        System.setProperty("jvm.resource.tracing", "false");
    }

    static boolean isDirect = true;

    static int size = 1024;


    private final Bytes<?> left = Bytes.allocateElasticDirect();
    private final Bytes<?> right = Bytes.allocateElasticDirect();

    private ContentEqualsJLBHTest() {
        for (int i = 0; i < size; i++) {
            left.append('x');
            right.append('x');
        }
    }

    public static void main(String[] args) {


        ContentEqualsJLBHTest benchmark = new ContentEqualsJLBHTest();
        JLBHTask task = new SimpleJLBHTask(benchmark.left, benchmark.right);
        JLBHOptions jlbhOptions = new JLBHOptions()
                .iterations(1_000_000)
                .throughput(100_000)
                .runs(4)
                .recordOSJitter(false).accountForCoordinatedOmission(true)
                .warmUpIterations(10_000)
                .jlbhTask(task);
        JLBH jlbh = new JLBH(jlbhOptions);
        jlbh.start();
    }


    private static class SimpleJLBHTask implements JLBHTask {

        private JLBH jlbh;
        private Bytes left;
        private Bytes right;

        public SimpleJLBHTask(Bytes left, Bytes right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public void init(JLBH jlbh) {
            this.jlbh = jlbh;
        }

        @Override
        public void run(long startTimeNS) {
            BytesInternal.contentEqual(left, right);
            jlbh.sampleNanos(System.nanoTime() - startTimeNS);
        }
    }
}

