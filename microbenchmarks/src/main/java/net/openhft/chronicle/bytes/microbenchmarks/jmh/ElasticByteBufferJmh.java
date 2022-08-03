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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ElasticByteBufferJmh {

    static final int SIZE = 1024;

    @State(Scope.Benchmark)
    public static class TestState {

        private final Bytes<ByteBuffer> elasticBufferFrom;
        private final Bytes<Void> elasticDirectFrom;
        private final Bytes<ByteBuffer> elasticMarshalFrom;
        private final Bytes<ByteBuffer> elasticMarshalTo;
        private final MappedBytes mappedMarshalFrom;
        private final MappedBytes mappedMarshalTo;

        private final Bytes<ByteBuffer> elasticBuffer;
        private final Bytes<ByteBuffer> elasticBufferEquals1;
        private final Bytes<ByteBuffer> elasticBufferEquals2;

        public TestState() {
            byte[] bytes = new byte[SIZE];
            new Random(1).nextBytes(bytes);
            elasticMarshalFrom = Bytes.elasticByteBuffer();
            elasticMarshalTo = Bytes.elasticByteBuffer();
            mappedMarshalFrom = mappedBytes();
            mappedMarshalTo = mappedBytes();
            elasticBuffer = Bytes.elasticByteBuffer(SIZE);
            elasticBufferEquals1 = Bytes.elasticByteBuffer(SIZE);
            elasticBufferEquals2 = Bytes.elasticByteBuffer(SIZE);
            elasticBufferFrom = Bytes.elasticByteBuffer(SIZE).write(bytes);
            elasticDirectFrom = Bytes.allocateElasticDirect(SIZE).write(bytes);
            elasticMarshalFrom.write8bit(elasticBufferFrom);
            mappedMarshalFrom.write8bit(elasticBufferFrom);
            elasticBufferEquals1.write(bytes);
            elasticBufferEquals2.write(bytes);
        }
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void append8bit_ElasticByteBuffer(final Blackhole blackhole, final TestState state) {
        blackhole.consume(state.elasticBuffer.clear().append8bit(state.elasticBufferFrom));
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void append8bit_ElasticDirect(final Blackhole blackhole, final TestState state) {
        blackhole.consume(state.elasticBuffer.clear().append8bit(state.elasticDirectFrom));
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void read8bit_elasticBytes(final Blackhole blackhole, final TestState state) {
        state.elasticMarshalFrom.readPosition(0).read8bit(state.elasticBuffer.clear());
        blackhole.consume(state.elasticBuffer);
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void read8bit_mappedBytes(final Blackhole blackhole, final TestState state) {
        state.mappedMarshalFrom.readPosition(0).read8bit(state.elasticBuffer.clear());
        blackhole.consume(state.elasticBuffer);
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
        blackhole.consume(state.elasticBufferEquals1.equals(state.elasticBufferEquals2));
    }

    static MappedBytes mappedBytes() {
        try {
            File tempFile = File.createTempFile("mapped", "bytes");
            tempFile.deleteOnExit();
            return MappedBytes.mappedBytes(tempFile, 64 << 10, 64 << 10);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create MappedBytes");
        }
    }
}