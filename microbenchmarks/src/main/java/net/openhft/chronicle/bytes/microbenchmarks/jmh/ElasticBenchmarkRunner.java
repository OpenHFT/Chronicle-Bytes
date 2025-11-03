/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.microbenchmarks.jmh;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

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
