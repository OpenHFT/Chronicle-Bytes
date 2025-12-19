/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Comprehensive tests for {@link NativeBytesStore}, including encryption
 * support, histogram utilities, and reference counting edge cases for native
 * stores.
 */
@SuppressWarnings("deprecation")
public class NativeBytesStoreTest extends BytesTestCommon {

    private volatile int bcs;

    @BeforeEach
    public void hasDirectMemory() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

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

        StringBuilder expected = new StringBuilder(maxLen);

        for (int i = 0; i < maxLen; i += stepLength) {
            final Bytes<byte[]> bytes = Bytes.from(expected.toString());

            bytesStore.write8bit(0, bytes);

            final StringBuilder sb = new StringBuilder(maxLen);
            bytesStore.readUtf8(0, sb);

            assertEquals(expected.toString(), sb.toString(), "failed at " + i);

            bytes.releaseLast();
            expected.append("aaaaaaaaaaaaaaaaaaaaaaa"); // 23 characters
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

        final Bytes<?> bytes = Bytes.allocateDirect(expected.getBytes(ISO_8859_1));
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
                assertEquals(expected, dec.toString(), "decrypted bytes should match original plaintext after encrypt/decrypt cycle");
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
                assertEquals(0, len % 16, "encrypted length should be multiple of 16 (AES block size)");
                enc.writeUnsignedByte(pos, Maths.toUInt8(len));
//                System.out.println(len);
            }
//            System.out.println("reading");
            for (int t = 0; t < 9; t++) {
                final int len = enc.readUnsignedByte();
//                System.out.println(len);
                assertEquals(0, len % 16, "stored encrypted length should be multiple of 16 (AES block size)");
                final long pos = enc.readPosition();
                enc.readPositionRemaining(pos, len);
                dec.clear();
                enc.cipher(decCipher, dec);
                generate(bytes, t);
                assertEquals(bytes.toString(), dec.toString(), "decrypted bytes should match original generated content for iteration " + t);
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
            assertEquals(Bytes.MAX_HEAP_CAPACITY, bbb.capacity(), "elastic ByteBuffer capacity should be max heap capacity before any writes");
            assertEquals(Bytes.DEFAULT_BYTE_BUFFER_CAPACITY, bbb.realCapacity(), "elastic ByteBuffer real capacity should be default size before expansion");
            final @Nullable ByteBuffer bb = bbb.underlyingObject();
            assertNotNull(bb, "elastic ByteBuffer should have non-null underlying ByteBuffer");

            for (int i = 0; i < 20; i++) {
                bbb.writeSkip(1000);
                bbb.writeLong(12345);
            }

            // page size on a Mac m1 is 0x4000 and not the usual 0x1000
            final long expectedRealCapacity = Jvm.isMacArm() ? 0x8000 : 0x7000;
            assertEquals(expectedRealCapacity, bbb.realCapacity(), "elastic ByteBuffer real capacity should expand to " + expectedRealCapacity + " after writing 20KB");
            final @Nullable ByteBuffer bb2 = bbb.underlyingObject();
            assertNotNull(bb2, "resized elastic ByteBuffer should have non-null underlying ByteBuffer");
            assertNotSame(bb, bb2, "elastic ByteBuffer should allocate new underlying buffer after resize");
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
            assertEquals(hi, nbs.toString(), "NativeBytesStore toString should match original string after appendUtf8");
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
                assertEquals(bb.get(i), (byte) chars[i], "bb.get");
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
            final Random rand = new Random(1L);
            for (NativeBytesStore<Void> nb : nbs) {
                final byte[] bytes = new byte[(int) nb.capacity()];
                rand.nextBytes(bytes);
                nb.write(0, bytes);
                assertEquals(Bytes.wrapForRead(bytes).byteCheckSum(), nb.byteCheckSum(), "NativeBytesStore byteCheckSum should match wrapped byte array checksum for capacity " + nb.capacity());
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
            assertEquals(src.toString(), dst.toString(), "destination bytes should match source after copyTo operation");
        } finally {
            src.releaseLast();
            dst.releaseLast();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testEquals() {
        @NotNull NativeBytesStore hbs = NativeBytesStore.from("Hello".getBytes(ISO_8859_1));
        @NotNull NativeBytesStore hbs2 = NativeBytesStore.from("Hello".getBytes(ISO_8859_1));
        @NotNull NativeBytesStore hbs3 = NativeBytesStore.from("He!!o".getBytes(ISO_8859_1));
        @NotNull final NativeBytesStore hbs4 = NativeBytesStore.from("Hi".getBytes(ISO_8859_1));
        assertEquals(hbs, hbs2, "NativeBytesStores with same content should be equal");
        assertEquals(hbs2, hbs, "equality should be symmetric for NativeBytesStores");
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
