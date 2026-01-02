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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Comprehensive tests for {@link NativeBytesStore}, including encryption
 * support, histogram utilities, and reference counting edge cases for native
 * stores.
 */
@DisplayName("Native bytes store behaviours including cipher and checksums")
public class NativeBytesStoreTest extends BytesTestCommon {

    private volatile int bcs;

    @BeforeEach
    public void hasDirectMemory() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for native bytes store tests");
    }

    private static void generate(final @NotNull Bytes<?> bytes, final int t) {
        bytes.clear();
        bytes.append("hello world ");
        for (int i = 0; i <= t; i++)
            bytes.append(t);
    }

    @Test
    @DisplayName("stop bits round trip across long strings")
    public void issue176StopBits() {
        final int stepLength = 23; // A prime of reasonable size (lagom) so we save some time stepping through the iterations
        final int maxLen = (1 << (7 * 2)) + stepLength;

        final NativeBytesStore<Void> bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(maxLen + 5);

        StringBuilder expected = new StringBuilder();

        for (int i = 0; i < maxLen; i += stepLength) {
            final String expectedText = expected.toString();
            final Bytes<byte[]> bytes = Bytes.from(expectedText);

            bytesStore.write8bit(0, bytes);

            final StringBuilder sb = new StringBuilder();
            bytesStore.readUtf8(0, sb);

            assertEquals(expectedText, sb.toString(),
                    "Stop bit round trip matches expected text at index " + i);

            bytes.releaseLast();
            expected.append("aaaaaaaaaaaaaaaaaaaaaaa"); // 23 characters
        }
    }

    @Test
    @DisplayName("cipher performance preserves plaintext output correctly")
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

        final Bytes<?> bytes = Bytes.allocateDirect(expected.getBytes(StandardCharsets.ISO_8859_1));
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
                assertEquals(expected, dec.toString(),
                        "Cipher round trip matches expected plaintext for run " + t);
            }
        } finally {
            bytes.releaseLast();
            enc.releaseLast();
            dec.releaseLast();
        }
    }

    @Test
    @DisplayName("cipher round trip preserves generated text")
    public void testCipher()
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        assumeFalse(NativeBytes.areNewGuarded(),
                "Native bytes guards must be disabled for cipher test");
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
                assertEquals(0, len % 16,
                        "Encrypted block length aligns to sixteen for run " + t);
                enc.writeUnsignedByte(pos, Maths.toUInt8(len));
            }
            for (int t = 0; t < 9; t++) {
                final int len = enc.readUnsignedByte();
                assertEquals(0, len % 16,
                        "Decoded block length aligns to sixteen for run " + t);
                final long pos = enc.readPosition();
                enc.readPositionRemaining(pos, len);
                dec.clear();
                enc.cipher(decCipher, dec);
                generate(bytes, t);
                assertEquals(bytes.toString(), dec.toString(),
                        "Decrypted text matches generated content for run " + t);
                enc.readPositionRemaining(pos + len, 1);
            }
        } finally {
            bytes.releaseLast();
            enc.releaseLast();
            dec.releaseLast();
        }
    }

    @Test
    @DisplayName("elastic byte buffer grows and replaces backing buffer")
    public void testElasticByteBuffer()
            throws IORuntimeException, BufferOverflowException {

        final Bytes<ByteBuffer> bbb = Bytes.elasticByteBuffer();
        try {
            assertEquals(Bytes.MAX_HEAP_CAPACITY, bbb.capacity(),
                    "Elastic buffer capacity equals max heap capacity");
            assertEquals(Bytes.DEFAULT_BYTE_BUFFER_CAPACITY, bbb.realCapacity(),
                    "Elastic buffer real capacity equals default size");
            final @Nullable ByteBuffer bb = bbb.underlyingObject();
            assertNotNull(bb,
                    "Elastic buffer exposes underlying ByteBuffer instance");

            for (int i = 0; i < 20; i++) {
                bbb.writeSkip(1000);
                bbb.writeLong(12345);
            }

            // page size on a Mac m1 is 0x4000 and not the usual 0x1000
            final long expectedRealCapacity = Jvm.isMacArm() ? 0x8000 : 0x7000;
            assertEquals(expectedRealCapacity, bbb.realCapacity(),
                    "Elastic buffer grows to expected page sized capacity");
            final @Nullable ByteBuffer bb2 = bbb.underlyingObject();
            assertNotNull(bb2,
                    "Elastic buffer exposes updated ByteBuffer after growth");
            assertNotSame(bb, bb2,
                    "Elastic buffer replaces backing ByteBuffer on growth");
        } finally {
            bbb.releaseLast();
        }
    }

    @Test
    @DisplayName("native bytes store appends UTF8 characters")
    public void testAppendUtf8() {
        final String hi = "Hello World";
        final char[] chars = hi.toCharArray();
        final NativeBytesStore<Void> nbs = NativeBytesStore.nativeStore(chars.length);
        try {
            nbs.appendUtf8(0, chars, 0, chars.length);
            assertEquals(hi, nbs.toString(),
                    "Native bytes store returns appended UTF8 text");
        } finally {
            nbs.releaseLast();
        }
    }

    @Test
    @DisplayName("temporary byte buffer mirrors stored UTF8 bytes")
    public void testToTempByteBuf() {
        final String hi = "Hello World";
        final char[] chars = hi.toCharArray();
        final NativeBytesStore<Void> bs = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(128);
        try {
            bs.appendUtf8(0, chars, 0, chars.length);
            final ByteBuffer bb = bs.toTemporaryDirectByteBuffer();
            for (int i = 0; i < chars.length; i++) {
                assertEquals((byte) chars[i], bb.get(i),
                        "Temporary buffer byte matches char at index " + i);
            }
        } finally {
            bs.releaseLast();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    @DisplayName("byte checksum stays within expected range")
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
                assertEquals(Bytes.wrapForRead(bytes).byteCheckSum(), nb.byteCheckSum(),
                        "Checksum matches expected value for capacity " + nb.capacity());
            }
            for (int t = 2; t >= 0; t--) {
                int runs = 10000000;
                final long start = System.nanoTime();
                for (int i = 0; i < runs; i += 4) {
                    for (NativeBytesStore<Void> nb : nbs) {
                        bcs = nb.byteCheckSum();
                        if (bcs < 0 || bcs > 255)
                            throw new AssertionError("Checksum outside expected byte range");
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
    @DisplayName("copyTo transfers content between byte buffers")
    public void testCopyTo() {
        final Bytes<ByteBuffer> src = Bytes.elasticByteBuffer().writeUtf8("hello");
        final Bytes<ByteBuffer> dst = Bytes.elasticByteBuffer();
        try {
            dst.writePosition(src.copyTo(dst));
            assertEquals(src.toString(), dst.toString(),
                    "copyTo preserves source content in destination");
        } finally {
            src.releaseLast();
            dst.releaseLast();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    @DisplayName("native bytes store equality compares content")
    public void testEquals() {
        @NotNull NativeBytesStore hbs = NativeBytesStore.from("Hello".getBytes(StandardCharsets.ISO_8859_1));
        @NotNull NativeBytesStore hbs2 = NativeBytesStore.from("Hello".getBytes(StandardCharsets.ISO_8859_1));
        @NotNull NativeBytesStore hbs3 = NativeBytesStore.from("He!!o".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(hbs, hbs2,
                "Equal content stores compare equal in forward check");
        assertEquals(hbs2, hbs,
                "Equal content stores compare equal in reverse check");
        assertNotEquals(hbs, hbs3,
                "Different content store differs from base store");
        assertNotEquals(hbs3, hbs,
                "Base store differs from different content store");
        @NotNull NativeBytesStore hbs4 = NativeBytesStore.from("Hi".getBytes(StandardCharsets.ISO_8859_1));
        assertNotEquals(hbs, hbs4,
                "Shorter content store differs from base store");
        assertNotEquals(hbs4, hbs,
                "Base store differs from shorter content store");
        hbs.releaseLast();
        hbs2.releaseLast();
        hbs3.releaseLast();
        hbs4.releaseLast();
    }
}
