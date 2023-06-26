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
import net.openhft.chronicle.core.Jvm;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/*
= Before optimisation
Benchmark                                        Mode  Cnt    Score    Error  Units
AppendDoubleBenchmark.appendDouble               avgt    5   58.959 ±  0.956  ns/op
AppendDoubleBenchmark.appendDoubleHeap           avgt    5  211.099 ± 14.790  ns/op
AppendDoubleBenchmark.appendDoubleUnchecked      avgt    5   92.551 ±  0.366  ns/op
AppendDoubleBenchmark.appendDoubleUncheckedHeap  avgt    5  150.123 ±  3.988  ns/op
AppendDoubleBenchmark.appendFloat                avgt    5  124.263 ±  2.739  ns/op
AppendDoubleBenchmark.appendFloatHeap            avgt    5  255.406 ±  1.420  ns/op
AppendDoubleBenchmark.appendFloatUnchecked       avgt    5  156.489 ±  0.779  ns/op
AppendDoubleBenchmark.appendFloatUncheckedHeap   avgt    5  221.035 ±  0.972  ns/op

= After optimisation
Benchmark                                        Mode  Cnt   Score   Error  Units
AppendDoubleBenchmark.appendDouble               avgt    5  37.314 ± 0.436  ns/op
AppendDoubleBenchmark.appendDoubleHeap           avgt    5  48.061 ± 3.128  ns/op
AppendDoubleBenchmark.appendDoubleUnchecked      avgt    5  36.062 ± 0.430  ns/op
AppendDoubleBenchmark.appendDoubleUncheckedHeap  avgt    5  46.214 ± 0.996  ns/op
AppendDoubleBenchmark.appendFloat                avgt    5  33.567 ± 0.395  ns/op
AppendDoubleBenchmark.appendFloatHeap            avgt    5  50.290 ± 6.359  ns/op
AppendDoubleBenchmark.appendFloatUnchecked       avgt    5  39.253 ± 0.223  ns/op
AppendDoubleBenchmark.appendFloatUncheckedHeap   avgt    5  50.138 ± 5.663  ns/op
 */

@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class AppendDoubleBenchmark {

    static final double[] SIZES = {1, 1e1, 1e2, 1e3, 1e4, 1e6, 1e8}; // 7 sizes
    static final float[] SIZES_F = {1f, 1e1f, 1e2f, 1e3f, 1e4f, 1e6f, 1e8f}; // 7 sizes
    private Bytes<?> bytes, bytes2;
    private Bytes<?> uncheckedBytes, uncheckedBytes2;
    private int count, size;

    public static void main(String[] args) throws RunnerException, InvocationTargetException, IllegalAccessException {
        if (Jvm.isDebug()) {
            AppendDoubleBenchmark ben = new AppendDoubleBenchmark();
            for (Method method : ben.getClass().getMethods()) {
                if (method.getAnnotation(Benchmark.class) == null)
                    continue;
                ben.setUp();
                method.invoke(ben);
            }
        } else {
            Options options = new OptionsBuilder()
                    .include(AppendDoubleBenchmark.class.getSimpleName())
                    .build();

            new Runner(options).run();
        }
    }

    @Setup
    public void setUp() {
        bytes = Bytes.allocateElasticDirect(256);
        bytes2 = Bytes.allocateElasticOnHeap(256);
        uncheckedBytes = Bytes.allocateElasticDirect(256).unchecked(true);
        uncheckedBytes2 = Bytes.allocateElasticOnHeap(256).unchecked(true);
        count = 0;
    }

    private double nextDouble(Bytes bytes) {
        size++;
        if (size >= SIZES.length) {
            size = 0;
            bytes.clear();
        }
        double d = count++ / SIZES[size];
        if (count > 10_000_000)
            count = 0;
        return d;
    }

    private float nextFloat(Bytes bytes) {
        size++;
        if (size >= SIZES.length) {
            size = 0;
            bytes.clear();
        }
        float f = count++ / SIZES_F[size];
        if (count > 1_000_000)
            count = 0;
        return f;
    }

    @Benchmark
    public void appendDouble() {
        double d = nextDouble(bytes);

        bytes.append(d);
    }

    @Benchmark
    public void appendFloat() {
        float f = nextFloat(bytes);

        bytes.append(f);
    }


    @Benchmark
    public void appendDoubleHeap() {
        double d = nextDouble(bytes2);

        bytes2.append(d);
    }

    @Benchmark
    public void appendFloatHeap() {
        float f = nextFloat(bytes2);

        bytes2.append(f);
    }

    @Benchmark
    public void appendDoubleUnchecked() {
        double d = nextDouble(uncheckedBytes);

        uncheckedBytes.append(d);
    }

    @Benchmark
    public void appendFloatUnchecked() {
        float f = nextFloat(uncheckedBytes);

        uncheckedBytes.append(f);
    }

    @Benchmark
    public void appendDoubleUncheckedHeap() {
        double d = nextDouble(uncheckedBytes2);

        uncheckedBytes2.append(d);
    }

    @Benchmark
    public void appendFloatUncheckedHeap() {
        float f = nextFloat(uncheckedBytes2);

        uncheckedBytes2.append(f);
    }
}