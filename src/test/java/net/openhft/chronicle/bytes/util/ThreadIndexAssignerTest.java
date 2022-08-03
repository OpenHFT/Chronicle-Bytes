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
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadIndexAssignerTest extends BytesTestCommon {
    @Test
    public void assignTwo()
            throws InterruptedException {
        assumeTrue(OS.isLinux() && !Jvm.isArm());
        final BlockingQueue<String> t0started = new LinkedBlockingQueue<>();
        final BlockingQueue<String> t1started = new LinkedBlockingQueue<>();
        final BlockingQueue<String> t2started = new LinkedBlockingQueue<>();
        final BlockingQueue<String> testDone = new LinkedBlockingQueue<>();

        final Bytes<?> bytes = new HexDumpBytes();
        final BinaryIntArrayReference iav = new BinaryIntArrayReference(2);
        // write the template
        iav.writeMarshallable(bytes);
        assertEquals("" +
                "                                                # BinaryIntArrayReference\n" +
                "02 00 00 00 00 00 00 00                         # capacity\n" +
                "00 00 00 00 00 00 00 00                         # used\n" +
                "00 00 00 00 00 00 00 00                         # values\n", bytes.toHexString());
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
//                System.out.println("0 started tid: " + Affinity.getThreadId());
                Assert.assertEquals(0, ta.getId());
                t0started.put("");
                testDone.poll(10, TimeUnit.SECONDS);
            } catch (Throwable ex) {
                ex.printStackTrace();
                throwables.add(ex);
            }
//            System.out.println("0 stopped tid: " + Affinity.getThreadId());
        });
        t0.start();

        t0started.poll(1, TimeUnit.SECONDS);

        Thread t1 = new Thread(() -> {
            try {
//                System.out.println("1 started tid: " + Affinity.getThreadId());
                Assert.assertEquals(1, ta.getId());
                t1started.put("");
                t2started.poll(1, TimeUnit.SECONDS);
            } catch (Throwable ex) {
                throwables.add(ex);
            }
//            System.out.println("1 stopped tid: " + Affinity.getThreadId());
        });
        t1.start();
        t1started.poll(1, TimeUnit.SECONDS);
        try {
            int id = ta.getId();
            fail();
        } catch (IllegalStateException ignore) {
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
        iav.close();
        bytes.releaseLast();

        Throwable th = throwables.poll();
        if (th != null)
            throw Jvm.rethrow(th);
    }
}