/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes.algo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.VanillaBytes;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import static net.openhft.chronicle.bytes.algo.OptimisedBytesStoreHash.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 28/06/15.
 */
public class OptimisedBytesStoreHashTest {

    @Test
    public void testApplyAsLong() {
        NativeBytes b = Bytes.allocateElasticDirect(128);
        b.writeLong(0x0102030405060708L);
        b.writeLong(0x1112131415161718L);
        while (b.readRemaining() > 0) {
            assertEquals("Rem: " + b.readRemaining(),
                    VanillaBytesStoreHash.INSTANCE.applyAsLong(b),
                    OptimisedBytesStoreHash.INSTANCE.applyAsLong(b));
            b.readSkip(1);
        }
        assertEquals(VanillaBytesStoreHash.INSTANCE.applyAsLong(b),
                OptimisedBytesStoreHash.INSTANCE.applyAsLong(b));
    }

    @Test
    public void sizeMatch() {
        NativeBytes nb = Bytes.allocateElasticDirect(64);
        for (int i = 1; i <= 64; i++)
            nb.writeUnsignedByte(i);
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
    }

    @Test
    @Ignore("Long running, har mean score = 5436")
    public void testRandomness() {
        SecureRandom rand = new SecureRandom();

        long time = 0, timeCount = 0;
        double scoreSum = 0;
        int runs = 500;
        for (int t = 0; t < runs; t++) {
            long[] hashs = new long[8192];
            byte[] init = new byte[hashs.length / 8];
            VanillaBytes b = Bytes.allocateDirect(init.length);
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
            if (t % 50 == 0)
                System.out.println(t + " - Score: " + score);
        }
        System.out.println("Average score: " + (long) (runs / scoreSum));
        System.out.printf("Average time %.3f us%n", time / timeCount / 1e3);
    }

    @Test
    @Ignore("Long running, avg score = 5414, avg time 0.043 us")
    public void testSmallRandomness() throws IOException {
        long time = 0, timeCount = 0;
        long scoreSum = 0;
//        StringBuilder sb = new StringBuilder();

        for (int t = 0; t < 500; t++) {
            long[] hashs = new long[8192];
            NativeBytes b = Bytes.allocateElasticDirect(8);
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

    @Test
    @Ignore("Only run for comparison, avg score = 6843")
    public void testSecureRandomness() {
        long scoreSum = 0;
        for (int t = 0; t < 500; t++) {
            Random rand = new SecureRandom();
            long[] hashs = new long[8192];
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

}