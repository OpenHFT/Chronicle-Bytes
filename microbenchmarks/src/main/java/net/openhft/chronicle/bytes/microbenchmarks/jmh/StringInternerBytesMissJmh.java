/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.microbenchmarks.jmh;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.util.StringInternerBytes;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Cost of a {@link StringInternerBytes} miss, the case for values that never repeat such as order IDs.
 * Every invocation interns a new unique ID of the given length. Run with {@code -prof gc} and read
 * {@code gc.alloc.rate.norm} for the bytes allocated per miss.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class StringInternerBytesMissJmh {

    @Param({"12", "30", "64"})
    int length;

    private StringInternerBytes interner;
    private Bytes<?> bytes;
    private long counter;

    @Setup
    public void setUp() {
        interner = new StringInternerBytes(1024);
        bytes = Bytes.allocateElasticDirect(128);
    }

    @TearDown
    public void tearDown() {
        bytes.releaseLast();
    }

    @Benchmark
    public String uniqueId() {
        bytes.clear();
        long value = counter++;
        // fixed-width unique text: a prefix padded with 'A' followed by 11 base-32 digits of the counter
        for (int i = 11; i < length; i++)
            bytes.writeUnsignedByte('A');
        for (int i = 0; i < 11; i++) {
            bytes.writeUnsignedByte("0123456789ABCDEFGHIJKLMNOPQRSTUV".charAt((int) (value & 31)));
            value >>>= 5;
        }
        return interner.intern(bytes, length);
    }
}
