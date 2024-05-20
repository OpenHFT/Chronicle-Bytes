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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
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
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class NativeBytesStoreTest extends BytesTestCommon {

    volatile int bcs;

    private static void generate(final @NotNull Bytes<?> bytes, final int t) {
        bytes.clear();
        bytes.append("hello world ");
        for (int i = 0; i <= t; i++)
            bytes.append(t);
    }

    @Test
    public void issue176StopBits() {
        final int stepLength = 23; // A prime of reasonable size (lagom) so we save some time stepping through the iterations
        final int maxLen = (1 << (7 * 2)) + stepLength;

        final NativeBytesStore<Void> bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(maxLen + 5);

        String expected = "";

        for (int i = 0; i < maxLen; i += stepLength) {
            final Bytes<byte[]> bytes = Bytes.from(expected);

            bytesStore.write8bit(0, bytes);

            // System.out.printf("0x%04x : 0x%02x, 0x%02x%n", i, bytesStore.readByte(0), bytesStore.readByte(1));

            final StringBuilder sb = new StringBuilder();
            bytesStore.readUtf8(0, sb);

            Assert.assertEquals("failed at " + i, expected, sb.toString());

            bytes.releaseLast();
            expected = expected + "a";
        }
    }

    @Test
    public void testCipherPerf()
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        final byte[] keyBytes = new SecureRandom().generateSeed(16);
        final SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        final Cipher encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        final Cipher decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        encCipher.init(Cipher.ENCRYPT_MODE, key);
        decCipher.init(Cipher.DECRYPT_MODE, key);

        final StringBuilder sb = new StringBuilder("Hello World!!");
        while (sb.length() < 100)
            sb.append(" 123456789");
        final String expected = sb.toString();

        final Bytes<?> bytes = Bytes.allocateDirect(expected.getBytes());
        final Bytes<?> enc = Bytes.allocateElasticDirect();
        final Bytes<?> dec = Bytes.allocateElasticDirect();
        try {
            final Histogram hist = new Histogram();
            for (int t = 1; t <= 4; t++) {
                for (int i = 0; i < t * 100000; i++) {
                    enc.clear();
                    dec.clear();
                    final long start = System.nanoTime();
                    bytes.cipher(encCipher, enc);
                    enc.cipher(decCipher, dec);
                    final long time = System.nanoTime() - start;
                    hist.sampleNanos(time);
                }
                assertEquals(expected, dec.toString());
//                System.out.println("Encrypt/Decrypt took " + hist.toMicrosFormat());
            }
        } finally {
            bytes.releaseLast();
            enc.releaseLast();
            dec.releaseLast();
        }
    }

    @Test
    public void testCipher()
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        assumeFalse(NativeBytes.areNewGuarded());
        final byte[] keyBytes = new SecureRandom().generateSeed(16);
        final SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        final Cipher encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        final Cipher decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        encCipher.init(Cipher.ENCRYPT_MODE, key);
        decCipher.init(Cipher.DECRYPT_MODE, key);

        final Bytes<?> bytes = Bytes.allocateElasticDirect();
        final Bytes<?> enc = Bytes.allocateElasticDirect();
        final Bytes<?> dec = Bytes.allocateElasticDirect();
        try {

            for (int t = 0; t < 9; t++) {
                final long pos = enc.writePosition();
                enc.writeByte((byte) 0);
                generate(bytes, t);
                bytes.cipher(encCipher, enc);
                final long len = enc.lengthWritten(pos) - 1;
                assertEquals(0, len % 16);
                enc.writeUnsignedByte(pos, Maths.toUInt8(len));
//                System.out.println(len);
            }
//            System.out.println("reading");
            for (int t = 0; t < 9; t++) {
                final int len = enc.readUnsignedByte();
//                System.out.println(len);
                assertEquals(0, len % 16);
                final long pos = enc.readPosition();
                enc.readPositionRemaining(pos, len);
                dec.clear();
                enc.cipher(decCipher, dec);
                generate(bytes, t);
                assertEquals(bytes.toString(), dec.toString());
                enc.readPositionRemaining(pos + len, 1);
            }
        } finally {
            bytes.releaseLast();
            enc.releaseLast();
            dec.releaseLast();
        }

    }

    @Test
    public void testElasticByteBuffer()
            throws IORuntimeException, BufferOverflowException {

        final Bytes<ByteBuffer> bbb = Bytes.elasticByteBuffer();
        try {
            assertEquals(Bytes.MAX_HEAP_CAPACITY, bbb.capacity());
            assertEquals(Bytes.DEFAULT_BYTE_BUFFER_CAPACITY, bbb.realCapacity());
            final @Nullable ByteBuffer bb = bbb.underlyingObject();
            assertNotNull(bb);

            for (int i = 0; i < 20; i++) {
                bbb.writeSkip(1000);
                bbb.writeLong(12345);
            }

            // page size on a Mac m1 is 0x4000 and not the usual 0x1000
            final long expectedRealCapacity = Jvm.isMacArm() ? 0x8000 : 0x7000;
            assertEquals(expectedRealCapacity, bbb.realCapacity());
            final @Nullable ByteBuffer bb2 = bbb.underlyingObject();
            assertNotNull(bb2);
            assertNotSame(bb, bb2);
        } finally {
            bbb.releaseLast();
        }
    }

    @Test
    public void testAppendUtf8() {
        final String hi = "Hello World";
        final char[] chars = hi.toCharArray();
        final NativeBytesStore<Void> nbs = NativeBytesStore.nativeStore(chars.length);
        try {
            nbs.appendUtf8(0, chars, 0, chars.length);
            assertEquals(hi, nbs.toString());
        } finally {
            nbs.releaseLast();
        }
    }

    @Test
    public void testToTempByteBuf() {
        final String hi = "Hello World";
        final char[] chars = hi.toCharArray();
        final NativeBytesStore<Void> bs = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(128);
        try {
            bs.appendUtf8(0, chars, 0, chars.length);
            final ByteBuffer bb = bs.toTemporaryDirectByteBuffer();
            for (int i = 0; i < chars.length; i++) {
                assertEquals(bb.get(i), (byte) chars[i]);
            }
        } finally {
            bs.releaseLast();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void perfCheckSum()
            throws IORuntimeException {
        final NativeBytesStore[] nbs = {
                NativeBytesStore.nativeStoreWithFixedCapacity(140),
                NativeBytesStore.nativeStoreWithFixedCapacity(149),
                NativeBytesStore.nativeStoreWithFixedCapacity(159),
                NativeBytesStore.nativeStoreWithFixedCapacity(194)
        };
        try {
            final Random rand = new Random();
            for (NativeBytesStore<Void> nb : nbs) {
                final byte[] bytes = new byte[(int) nb.capacity()];
                rand.nextBytes(bytes);
                nb.write(0, bytes);
                assertEquals(Bytes.wrapForRead(bytes).byteCheckSum(), nb.byteCheckSum());
            }
            for (int t = 2; t >= 0; t--) {
                int runs = 10000000;
                final long start = System.nanoTime();
                for (int i = 0; i < runs; i += 4) {
                    for (NativeBytesStore<Void> nb : nbs) {
                        bcs = nb.byteCheckSum();
                        if (bcs < 0 || bcs > 255)
                            throw new AssertionError();
                    }
                }
                long time = System.nanoTime() - start;
                if (t == 0)
                    System.out.printf("Average time was %,d ns%n", time / runs);
            }
        } finally {
            Stream.of(nbs)
                    .forEach(NativeBytesStore::releaseLast);
        }
    }

    @Test
    public void testCopyTo() {
        final Bytes<ByteBuffer> src = Bytes.elasticByteBuffer().writeUtf8("hello");
        final Bytes<ByteBuffer> dst = Bytes.elasticByteBuffer();
        try {
            dst.writePosition(src.copyTo(dst));
            assertEquals(src.toString(), dst.toString());
        } finally {
            src.releaseLast();
            dst.releaseLast();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testEquals() {
        @NotNull NativeBytesStore hbs = NativeBytesStore.from("Hello".getBytes());
        @NotNull NativeBytesStore hbs2 = NativeBytesStore.from("Hello".getBytes());
        @NotNull NativeBytesStore hbs3 = NativeBytesStore.from("He!!o".getBytes());
        @NotNull NativeBytesStore hbs4 = NativeBytesStore.from("Hi".getBytes());
        assertEquals(hbs, hbs2);
        assertEquals(hbs2, hbs);
        assertNotEquals(hbs, hbs3);
        assertNotEquals(hbs3, hbs);
        assertNotEquals(hbs, hbs4);
        assertNotEquals(hbs4, hbs);
        hbs.releaseLast();
        hbs2.releaseLast();
        hbs3.releaseLast();
        hbs4.releaseLast();
    }
}
