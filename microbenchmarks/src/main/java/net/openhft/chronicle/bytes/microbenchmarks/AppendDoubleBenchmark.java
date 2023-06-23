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
package net.openhft.chronicle.bytes.microbenchmarks;

import net.openhft.chronicle.bytes.Bytes;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/*
= Before optimisation
Benchmark                           Mode  Cnt    Score    Error  Units
AppendDoubleBenchmark.appendDouble  avgt    5   76.108 ± 18.943  ns/op
AppendDoubleBenchmark.appendFloat   avgt    5  121.027 ±  2.328  ns/op

= After optimisation
Benchmark                           Mode  Cnt   Score   Error  Units
AppendDoubleBenchmark.appendDouble  avgt    5  50.598 ± 0.185  ns/op
AppendDoubleBenchmark.appendFloat   avgt    5  43.135 ± 4.113  ns/op
 */

@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class AppendDoubleBenchmark {

    private Bytes<?> bytes;
    private int count;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(AppendDoubleBenchmark.class.getSimpleName())
                .build();

        new Runner(options).run();
    }

    @Setup
    public void setUp() {
        bytes = Bytes.allocateElasticDirect(32);
        count = 0;
    }

    @Benchmark
    public void appendDouble() {
        double d = count++ / 1e6;
        if (d > 10)
            count = 0;

        bytes.clear();
        bytes.append(d);
    }

    @Benchmark
    public void appendFloat() {
        float f = count++ / 1e5f;
        if (f > 10)
            count = 0;

        bytes.clear();
        bytes.append(f);
    }
}