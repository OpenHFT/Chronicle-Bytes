/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.microbenchmarks.jmh;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Convenience entry point for running the elastic {@link Bytes} JMH benchmarks.
 *
 * <p>This runner wires together {@link ElasticByteBufferJmh} and {@link ElasticDirectJmh} so they
 * can be executed from an IDE or command line without remembering the full JMH invocation.
 */
public class ElasticBenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ElasticByteBufferJmh.class.getSimpleName())
                .include(ElasticDirectJmh.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
