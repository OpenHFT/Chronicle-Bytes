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
AppendDoubleBenchmark.appendDouble  avgt    5  47.477 ± 0.270  ns/op
AppendDoubleBenchmark.appendFloat   avgt    5  37.610 ± 0.327  ns/op
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
        float d = count++ / 1e5f;
        if (d > 10)
            count = 0;

        bytes.clear();
        bytes.append(d);
    }
}