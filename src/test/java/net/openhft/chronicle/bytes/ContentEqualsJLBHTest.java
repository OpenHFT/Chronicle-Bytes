/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;

public class ContentEqualsJLBHTest extends BytesTestCommon {

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
        private final Bytes<?> left;
        private final Bytes<?> right;

        public SimpleJLBHTask(Bytes<?> left, Bytes<?> right) {
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

