/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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
package net.openhft.chronicle.bytes.algo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import static net.openhft.chronicle.bytes.algo.OptimisedBytesStoreHash.*;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
public class OptimisedBytesStoreHashTest extends BytesTestCommon {

    @Test
    public void testApplyAsLong() {
        @NotNull NativeBytes b = Bytes.allocateElasticDirect(128);
        b.writeLong(0x0102030405060708L);
        b.writeLong(0x1112131415161718L);
        assertEquals(VanillaBytesStoreHash.INSTANCE.applyAsLong(b),
                OptimisedBytesStoreHash.INSTANCE.applyAsLong(b));

        while (b.readSkip(1).readRemaining() > 0) {
            long expected = VanillaBytesStoreHash.INSTANCE.applyAsLong(b);
            long actual = OptimisedBytesStoreHash.INSTANCE.applyAsLong(b);
            assertEquals("Rem: " + b.readRemaining(), expected, actual);
        }
        assertEquals(VanillaBytesStoreHash.INSTANCE.applyAsLong(b),
                OptimisedBytesStoreHash.INSTANCE.applyAsLong(b));
        b.releaseLast();
    }

    @Test
    public void sizeMatch() {
        @NotNull NativeBytesStore nb = NativeBytesStore.nativeStore(64);
        for (int i = 1; i <= 64; i++)
            nb.writeUnsignedByte(i - 1, i);
/*
        assertEquals(0L, applyAsLong1to7(nb, 0));
        for (int i = 1; i <= 7; i++)
            assertEquals(applyAsLong1to7(nb, i), applyAsLong9to16(nb, i));
        assertEquals(applyAsLong8(nb), applyAsLong1to7(nb, 8));
        assertEquals(applyAsLong8(nb), applyAsLong9to16(nb, 8));
*/
        for (int i = 1; i <= 16; i++)
            assertEquals("i: " + i, applyAsLong9to16(nb, i), applyAsLongAny(nb, i));
        for (int i = 17; i <= 32; i++)
            assertEquals("i: " + i, applyAsLong17to32(nb, i), applyAsLongAny(nb, i));
        nb.releaseLast();
    }

    //@Test
    //@Ignore("Long running, har mean score = 5436")
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
//            if (t % 50 == 0)
//                System.out.println(t + " - Score: " + score);
        }
        System.out.println("Average score: " + (long) (runs / scoreSum));
        System.out.printf("Average time %.3f us%n", time / timeCount / 1e3);
    }

    //@Test
    //@Ignore("Long running, avg score = 5414, avg time 0.043 us")
    public void testSmallRandomness()
            throws IOException {
        long time = 0, timeCount = 0;
        long scoreSum = 0;
//        StringBuilder sb = new StringBuilder();

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

/*               if (true) {
                    sb.setLength(0);
                    sb.append(b);
                    assertEquals(hashs[i], Maths.longHash(sb));
                }*/
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
        System.out.printf("Average time %.3f us%n", time / timeCount / 1e3);
    }

    //@Test
    //@Ignore("Only run for comparison, avg score = 6843")
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
    public void testReadIncompleteLong() {
        Bytes<?> bs = Bytes.allocateDirect(8);
        for (int i = 1; i <= 8; i++)
            bs.writeUnsignedByte(i);
        @NotNull Bytes<?> bs2 = Bytes.allocateDirect(9).unchecked(true);

        for (int i = 0; i <= 8; i++) {
            assertEquals("i: " + i, Long.toHexString(bs2.readLong(0)),
                    Long.toHexString(OptimisedBytesStoreHash.readIncompleteLong(bs.addressForRead(0), i)));
            bs2.writeUnsignedByte(i + 1);
        }
        bs.releaseLast();
        bs2.releaseLast();
    }
}
