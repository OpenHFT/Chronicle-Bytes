/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.algo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import static net.openhft.chronicle.bytes.algo.OptimisedBytesStoreHash.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the optimised BytesStore hashing implementation because verified consistency with
 * VanillaBytesStoreHash is required to avoid unexpected collisions in production workloads.
 */
@SuppressWarnings("rawtypes")
@DisplayName("OptimisedBytesStoreHash - consistency with VanillaBytesStoreHash across sizes")
public class OptimisedBytesStoreHashTest extends BytesTestCommon {

    @Test
    @DisplayName("optimised algorithm produces identical hash to vanilla implementation")
    public void testApplyAsLong() {
        @NotNull NativeBytes b = Bytes.allocateElasticDirect(128);
        b.writeLong(0x0102030405060708L);
        b.writeLong(0x1112131415161718L);
        assertEquals(VanillaBytesStoreHash.INSTANCE.applyAsLong(b),
                OptimisedBytesStoreHash.INSTANCE.applyAsLong(b),
                "Initial hash from optimised should equal vanilla result");

        while (b.readSkip(1).readRemaining() > 0) {
            long remaining = b.readRemaining();
            long expected = VanillaBytesStoreHash.INSTANCE.applyAsLong(b);
            long actual = OptimisedBytesStoreHash.INSTANCE.applyAsLong(b);
            assertEquals(expected, actual,
                    "Hash value should be identical with " + remaining + " remaining");
        }
        assertEquals(VanillaBytesStoreHash.INSTANCE.applyAsLong(b),
                OptimisedBytesStoreHash.INSTANCE.applyAsLong(b),
                "Final hash should be identical after consuming all data");
        b.releaseLast();
    }

    @Test
    @DisplayName("applyAsLongAny dispatches correctly to size-specific implementations")
    public void sizeMatch() {
        @NotNull NativeBytesStore nb = NativeBytesStore.nativeStore(64);
        for (int i = 1; i <= 64; i++)
            nb.writeUnsignedByte(i - 1, i);
        for (int i = 1; i <= 16; i++)
            assertEquals(applyAsLong9to16(nb, i), applyAsLongAny(nb, i),
                    "Dispatcher should select 9-16 implementation for size " + i);
        for (int i = 17; i <= 32; i++)
            assertEquals(applyAsLong17to32(nb, i), applyAsLongAny(nb, i),
                    "Dispatcher should select 17-32 implementation for size " + i);
        nb.releaseLast();
    }

    public void testRandomness() {
        @NotNull SecureRandom rand = new SecureRandom();

        long time = 0, timeCount = 0;
        double scoreSum = 0;
        int runs = 500;
        for (int t = 0; t < runs; t++) {
            @NotNull long[] hashs = new long[8192];
            @NotNull byte[] init = new byte[hashs.length / 8];
            Bytes<?> b = Bytes.allocateDirect(init.length);
            rand.nextBytes(init);
            for (int i = 0; i < hashs.length; i++) {
                b.clear();
                b.write(init);

                long prev = b.readLong(i >> 6 << 3);
                b.writeLong(i >> 6 << 3, prev ^ (1L << i));

                b.readLimit(init.length);
                long start = System.nanoTime();
                hashs[i] = VanillaBytesStoreHash.INSTANCE.applyAsLong(b);

                time += System.nanoTime() - start;
                timeCount++;
            }
            long score = 0;
            for (int i = 0; i < hashs.length - 1; i++)
                for (int j = i + 1; j < hashs.length; j++) {
                    long diff = hashs[j] ^ hashs[i];
                    int diffBC = Long.bitCount(diff);
                    if (diffBC <= 17) {
                        long d = 1L << (17 - diffBC);
                        score += d;
                    }
                }
            scoreSum += 1.0 / score;
        }
        double avgScore = runs / scoreSum;
        System.out.println("Average score: " + (long) avgScore);
        double avgTimeMicros = (double) time / timeCount / 1e3;
        System.out.printf("Average time %.3f us%n", avgTimeMicros);
    }

    @Test
    @Disabled("Long running, avg score = 5414, avg time 0.043 us")
    @DisplayName("small randomness test for optimised hash")
    public void testSmallRandomness() {
        long time = 0, timeCount = 0;
        long scoreSum = 0;

        for (int t = 0; t < 500; t++) {
            @NotNull long[] hashs = new long[8192];
            @NotNull NativeBytes b = Bytes.allocateElasticDirect(8);
            for (int i = 0; i < hashs.length; i++) {
                b.clear();
                b.append(t);
                b.append('-');
                b.append(i);
                long start = System.nanoTime();
                hashs[i] = OptimisedBytesStoreHash.INSTANCE.applyAsLong(b);
                time += System.nanoTime() - start;
                timeCount++;

            }
            long score = 0;
            for (int i = 0; i < hashs.length - 1; i++)
                for (int j = i + 1; j < hashs.length; j++) {
                    long diff = hashs[j] ^ hashs[i];
                    int diffBC = Long.bitCount(diff);
                    if (diffBC < 18) {
                        long d = 1L << (17 - diffBC);
                        score += d;
                    }
                }
            scoreSum += score;
            if (t % 50 == 0)
                System.out.println(t + " - Score: " + score);
        }
        System.out.println("Average score: " + scoreSum / 500);
        double avgTimeMicros = (double) time / timeCount / 1e3;
        System.out.printf("Average time %.3f us%n", avgTimeMicros);
    }

    public void testSecureRandomness() {
        long scoreSum = 0;
        for (int t = 0; t < 500; t++) {
            @NotNull Random rand = new SecureRandom();
            @NotNull long[] hashs = new long[8192];
            for (int i = 0; i < hashs.length; i++) {
                hashs[i] = rand.nextLong();
            }
            int score = 0;
            for (int i = 0; i < hashs.length - 1; i++)
                for (int j = i + 1; j < hashs.length; j++) {
                    long diff = hashs[j] ^ hashs[i];
                    int diffBC = Long.bitCount(diff);
                    if (diffBC < 18) {
                        int d = 1 << (17 - diffBC);
                        score += d;
                    }
                }
            scoreSum += score;
            if (t % 50 == 0)
                System.out.println(t + " - Score: " + score);
        }
        System.out.println("Average score: " + scoreSum / 500);
    }

    @Test
    @DisplayName("readIncompleteLong produces expected hex values for partial length reads")
    public void testReadIncompleteLong() {
        Bytes<?> bs = Bytes.allocateDirect(8);
        for (int i = 1; i <= 8; i++)
            bs.writeUnsignedByte(i);
        @NotNull Bytes<?> bs2 = Bytes.allocateDirect(9).unchecked(true);

        for (int i = 0; i <= 8; i++) {
            assertEquals(Long.toHexString(bs2.readLong(0)),
                    Long.toHexString(OptimisedBytesStoreHash.readIncompleteLong(bs.addressForRead(0), i)),
                    "Incomplete long hex should be identical for length " + i);
            bs2.writeUnsignedByte(i + 1);
        }
        bs.releaseLast();
        bs2.releaseLast();
    }
}
