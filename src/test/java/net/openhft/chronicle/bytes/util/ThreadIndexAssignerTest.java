package net.openhft.chronicle.bytes.util;

import net.openhft.affinity.Affinity;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.ref.BinaryIntArrayReference;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.util.ThreadIndexAssigner;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class ThreadIndexAssignerTest {
    @Test
    public void assignTwo() throws InterruptedException {
        assumeTrue(OS.isLinux() && !Jvm.isArm());
        BlockingQueue t0started = new LinkedBlockingQueue();
        BlockingQueue t1started = new LinkedBlockingQueue();
        BlockingQueue t2started = new LinkedBlockingQueue();
        BlockingQueue testDone = new LinkedBlockingQueue();

        Bytes bytes = Bytes.allocateDirect(64);
        BinaryIntArrayReference iav = new BinaryIntArrayReference(2);
        // write the template
        iav.writeMarshallable(bytes);
        // bind to the template
        iav.readMarshallable(bytes);
        ThreadIndexAssigner ta = new ThreadIndexAssigner(iav) {
            final int next = 0;

            @Override
            protected int nextIndex(int size) {
                return next % size;
            }
        };
        BlockingQueue<Throwable> throwables = new LinkedBlockingQueue<>();
        Thread t0 = new Thread(() -> {
            try {
                System.out.println("0 started tid: " + Affinity.getThreadId());
                Assert.assertEquals(0, ta.getId());
                t0started.put("");
                testDone.poll(10, TimeUnit.SECONDS);
            } catch (Throwable ex) {
                ex.printStackTrace();
                throwables.add(ex);
            }
            System.out.println("0 stopped tid: " + Affinity.getThreadId());
        });
        t0.start();

        t0started.poll(1, TimeUnit.SECONDS);

        Thread t1 = new Thread(() -> {
            try {
                System.out.println("1 started tid: " + Affinity.getThreadId());
                Assert.assertEquals(1, ta.getId());
                t1started.put("");
                t2started.poll(1, TimeUnit.SECONDS);
            } catch (Throwable ex) {
                throwables.add(ex);
            }
            System.out.println("1 stopped tid: " + Affinity.getThreadId());
        });
        t1.start();
        t1started.poll(1, TimeUnit.SECONDS);
        try {
            System.out.println("id=" + ta.getId());
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
        t2started.put("");
        t1.join();
        for (int i = 1; i <= 5; i++) {
            Jvm.pause(i);
            try {
                Assert.assertEquals(1, ta.getId());
                break;
            } catch (IllegalStateException e) {
                if (i == 5)
                    throw e;
            }
        }
        testDone.put("");
        t0.join();

        // unchanged
        Assert.assertEquals(1, ta.getId());
        bytes.releaseLast();

        Throwable th = throwables.poll();
        if (th != null)
            throw Jvm.rethrow(th);
    }
}