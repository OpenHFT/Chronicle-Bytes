package net.openhft.chronicle.bytes.microbenchmarks;

import net.openhft.chronicle.bytes.DistributedUniqueTimeProvider;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class DistributedUniqueTimeProviderBenchmark {
    private DistributedUniqueTimeProvider timeProvider;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DistributedUniqueTimeProviderBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(5))
                .forks(5)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setUp() {
        timeProvider = DistributedUniqueTimeProvider.forHostId(1);
    }

    @TearDown
    public void tearDown() {
        timeProvider.close();
    }

/*
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long currentTimeNanos() {
        return timeProvider.currentTimeNanos();
    }
*/

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public UUID randomUUID() {
        return UUID.randomUUID();
    }
}
