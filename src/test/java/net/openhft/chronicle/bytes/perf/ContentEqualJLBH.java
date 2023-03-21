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
package net.openhft.chronicle.bytes.perf;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import net.openhft.chronicle.jlbh.TeamCityHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ContentEqualJLBH implements JLBHTask {
    private static final int ITERATIONS = 5_000_000;
    private final Bytes<?> bytes1;
    private final Bytes<?>[] comparisons;
    private JLBH jlbh;
    private int counter;

    public ContentEqualJLBH(Supplier<Bytes<?>> bytesSupplier) {
        bytes1 = bytesSupplier.get();
        comparisons = generateComparisons(bytesSupplier);
    }

    /**
     * Create a set of Bytes to compare against including best case - totally reversed and worst case - exactly the same
     */
    private Bytes<?>[] generateComparisons(Supplier<Bytes<?>> bytesSupplier) {
        final List<Bytes<?>> rv = new ArrayList<>();
        final Bytes<?> example = bytesSupplier.get();
        final int step = Math.max(1, example.length() / 8);
        for (int i = 0; i < example.length(); i += step) {
            final Bytes<?> b = bytesSupplier.get();
            BytesUtil.reverse(b, i);
            rv.add(b);
        }
        rv.add(example);
        return rv.toArray(new Bytes<?>[rv.size()]);
    }

    @Override
    public void init(JLBH jlbh) {
        this.jlbh = jlbh;
    }

    @Override
    public void complete() {
        // this is horrible - it would be easier if Bootstrap.getMajorVersion0 was exposed
        String javaVersion = Jvm.isJava9Plus() ? Jvm.isJava15Plus() ? "17" : "11" : "8";
        TeamCityHelper.teamCityStatsLastRun(getClass().getSimpleName() + "-Java" + javaVersion + "-" + bytes1.length(), jlbh, ITERATIONS, System.out);
    }

    @Override
    public void run(long startTimeNS) {
        BytesInternal.contentEqual(bytes1, comparisons[counter % comparisons.length]);
        counter++;
        jlbh.sampleNanos(System.nanoTime() - startTimeNS);
    }

    static void runWith(Supplier<Bytes<?>> bytesSupplier) {
        System.setProperty("jvm.resource.tracing", "false");
        new JLBH(new JLBHOptions()
                .warmUpIterations(50_000)
                .iterations(ITERATIONS)
                .runs(4)
                .skipFirstRun(true)
                .recordOSJitter(false)
                .throughput(500_000)
                .pauseAfterWarmupMS(100)
                .accountForCoordinatedOmission(true)
                .jlbhTask(new ContentEqualJLBH(bytesSupplier)))
                .start();
    }

    static Bytes<?> bytesFor(int length) {
        final Bytes b = Bytes.elasticHeapByteBuffer(length);
        for (int i = 0; i < length; i++) {
            b.append(i % 10);
        }
        return b;
    }
}
