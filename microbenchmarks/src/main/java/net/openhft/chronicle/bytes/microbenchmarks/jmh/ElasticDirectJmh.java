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
package net.openhft.chronicle.bytes.microbenchmarks.jmh;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static net.openhft.chronicle.bytes.microbenchmarks.jmh.ElasticByteBufferJmh.SIZE;
import static net.openhft.chronicle.bytes.microbenchmarks.jmh.ElasticByteBufferJmh.mappedBytes;

public class ElasticDirectJmh {

    @State(Scope.Benchmark)
    public static class TestState {

        private final Bytes<Void> elasticDirectFrom;
        private final Bytes<ByteBuffer> elasticBufferFrom;
        private final Bytes<Void> elasticMarshalFrom;
        private final Bytes<Void> elasticMarshalTo;
        private final MappedBytes mappedMarshalFrom;
        private final MappedBytes mappedMarshalTo;

        private final Bytes<Void> elasticDirect;
        private final Bytes<Void> elasticDirectEquals1;
        private final Bytes<Void> elasticDirectEquals2;

        public TestState() {
            byte[] bytes = new byte[SIZE];
            new Random(1).nextBytes(bytes);
            elasticMarshalFrom = Bytes.allocateElasticDirect();
            elasticMarshalTo = Bytes.allocateElasticDirect();
            mappedMarshalFrom = mappedBytes();
            mappedMarshalTo = mappedBytes();
            elasticDirect = Bytes.allocateElasticDirect(SIZE);
            elasticDirectEquals1 = Bytes.allocateElasticDirect(SIZE);
            elasticDirectEquals2 = Bytes.allocateElasticDirect(SIZE);
            elasticDirectFrom = Bytes.allocateElasticDirect(SIZE).write(bytes);
            elasticBufferFrom = Bytes.elasticByteBuffer(SIZE).write(bytes);
            elasticMarshalFrom.write8bit(elasticBufferFrom);
            mappedMarshalFrom.write8bit(elasticBufferFrom);
            elasticDirectEquals1.write(bytes);
            elasticDirectEquals2.write(bytes);
        }
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void append8bit_ElasticByteBuffer(final Blackhole blackhole, final TestState state) {
        blackhole.consume(state.elasticDirect.clear().append8bit(state.elasticBufferFrom));
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void append8bit_ElasticDirect(final Blackhole blackhole, final TestState state) {
        blackhole.consume(state.elasticDirect.clear().append8bit(state.elasticDirectFrom));
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void read8bit_elasticBytes(final Blackhole blackhole, final TestState state) {
        state.elasticMarshalFrom.readPosition(0).read8bit(state.elasticDirect.clear());
        blackhole.consume(state.elasticDirect);
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void read8bit_mappedBytes(final Blackhole blackhole, final TestState state) {
        state.mappedMarshalFrom.readPosition(0).read8bit(state.elasticDirect.clear());
        blackhole.consume(state.elasticDirect);
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void write8bit_elasticBytes(final Blackhole blackhole, final TestState state) {
        state.elasticMarshalTo.clear().write8bit(state.elasticBufferFrom);
        blackhole.consume(state.elasticMarshalTo);
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void write8bit_mappedBytes(final Blackhole blackhole, final TestState state) {
        state.mappedMarshalTo.clear().write8bit(state.elasticBufferFrom);
        blackhole.consume(state.mappedMarshalTo);
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void equals(final Blackhole blackhole, final TestState state) {
        blackhole.consume(state.elasticDirectEquals1.equals(state.elasticDirectEquals2));
    }
}
