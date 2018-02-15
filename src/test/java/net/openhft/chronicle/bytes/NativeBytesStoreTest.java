/*
 * Copyright 2016 higherfrequencytrading.com
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

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.threads.ThreadDump;
import net.openhft.chronicle.core.util.Histogram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import static org.junit.Assert.*;

/*
 * Created by peter.lawrey on 27/02/15.
 */
public class NativeBytesStoreTest {
    volatile int bcs;
    private ThreadDump threadDump;

    private static void generate(@NotNull Bytes bytes, int t) {
        bytes.clear();
        bytes.append("hello world ");
        for (int i = 0; i <= t; i++)
            bytes.append(t);
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @Test
    public void testCipherPerf() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        byte[] keyBytes = new SecureRandom().generateSeed(16);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        Cipher decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        encCipher.init(Cipher.ENCRYPT_MODE, key);
        decCipher.init(Cipher.DECRYPT_MODE, key);

        String expected = "Hello World!!";
        while (expected.length() < 100)
            expected += " 123456789";
        Bytes bytes = Bytes.allocateDirect(expected.getBytes());
        Bytes enc = Bytes.allocateElasticDirect();
        Bytes dec = Bytes.allocateElasticDirect();
        Histogram hist = new Histogram();
        for (int t = 1; t <= 4; t++) {
            for (int i = 0; i < t * 100000; i++) {
                enc.clear();
                dec.clear();
                long start = System.nanoTime();
                bytes.cipher(encCipher, enc);
                enc.cipher(decCipher, dec);
                long time = System.nanoTime() - start;
                hist.sampleNanos(time);
            }

            assertEquals(expected, dec.toString());
            System.out.println("Encrypt/Decrypt took " + hist.toMicrosFormat());
        }

        bytes.release();
        enc.release();
        dec.release();
    }

    @Test
    public void testCipher() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        byte[] keyBytes = new SecureRandom().generateSeed(16);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        Cipher decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        encCipher.init(Cipher.ENCRYPT_MODE, key);
        decCipher.init(Cipher.DECRYPT_MODE, key);

        Bytes bytes = Bytes.allocateElasticDirect();
        Bytes enc = Bytes.allocateElasticDirect();
        Bytes dec = Bytes.allocateElasticDirect();

        for (int t = 0; t < 9; t++) {
            long pos = enc.writePosition();
            enc.writeByte((byte) 0);
            generate(bytes, t);
            bytes.cipher(encCipher, enc);
            long len = enc.writePosition() - pos - 1;
            assertEquals(0, len % 16);
            enc.writeUnsignedByte(pos, Maths.toUInt8(len));
            System.out.println(len);
        }
        System.out.println("reading");
        for (int t = 0; t < 9; t++) {
            int len = enc.readUnsignedByte();
            System.out.println(len);
            assertEquals(0, len % 16);
            long pos = enc.readPosition();
            enc.readPositionRemaining(pos, len);
            dec.clear();
            enc.cipher(decCipher, dec);
            generate(bytes, t);
            assertEquals(bytes.toString(), dec.toString());
            enc.readPositionRemaining(pos + len, 1);
        }
    }

    @Test
    public void testElasticByteBuffer() throws IORuntimeException, BufferOverflowException {
        Bytes<ByteBuffer> bbb = Bytes.elasticByteBuffer();
        assertEquals(Bytes.MAX_CAPACITY, bbb.capacity());
        assertEquals(Bytes.DEFAULT_BYTE_BUFFER_CAPACITY, bbb.realCapacity());
        @Nullable ByteBuffer bb = bbb.underlyingObject();
        assertNotNull(bb);

        for (int i = 0; i < 20; i++) {
            bbb.writeSkip(1000);
            bbb.writeLong(12345);
        }
        assertEquals(OS.pageSize() * 5, bbb.realCapacity());
        @Nullable ByteBuffer bb2 = bbb.underlyingObject();
        assertNotNull(bb2);
        assertNotSame(bb, bb2);

        bbb.release();
    }

    @Test
    public void testAppendUtf8() {
        @NotNull String hi = "Hello World";
        @NotNull char[] chars = hi.toCharArray();
        @NotNull NativeBytesStore nbs = NativeBytesStore.nativeStore(chars.length);
        nbs.appendUtf8(0, chars, 0, chars.length);
        assertEquals(hi, nbs.toString());
    }

    //@Test
    //@Ignore("Long running test")
    public void perfCheckSum() throws IORuntimeException {
        @NotNull NativeBytesStore[] nbs = {
                NativeBytesStore.nativeStoreWithFixedCapacity(140),
                NativeBytesStore.nativeStoreWithFixedCapacity(149),
                NativeBytesStore.nativeStoreWithFixedCapacity(159),
                NativeBytesStore.nativeStoreWithFixedCapacity(194)};
        @NotNull Random rand = new Random();
        for (@NotNull NativeBytesStore nb : nbs) {
            @NotNull byte[] bytes = new byte[(int) nb.capacity()];
            rand.nextBytes(bytes);
            nb.write(0, bytes);
            assertEquals(Bytes.wrapForRead(bytes).byteCheckSum(), nb.byteCheckSum());
        }
        for (int t = 0; t < 3; t++) {
            int runs = 10000000;
            long start = System.nanoTime();
            for (int i = 0; i < runs; i += 4) {
                for (@NotNull NativeBytesStore nb : nbs) {
                    bcs = nb.byteCheckSum();
                    if (bcs < 0 || bcs > 255)
                        throw new AssertionError();
                }
            }
            long time = System.nanoTime() - start;
            System.out.printf("Average time was %,d ns%n", time / runs);
        }
    }

    @Test
    public void testCopyTo() {
        @NotNull Bytes<ByteBuffer> src = Bytes.elasticByteBuffer().writeUtf8("hello");
        Bytes<ByteBuffer> dst = Bytes.elasticByteBuffer();

        dst.writePosition(src.copyTo(dst));
        assertEquals(src, dst);

        src.release();
        dst.release();
    }
}
