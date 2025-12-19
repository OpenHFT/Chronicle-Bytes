/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrewriteTest extends BytesTestCommon {
    @Test
    public void test() {
        Bytes<?> bytes = Bytes.allocateDirect(64);
        bytes.clearAndPad(64);
        bytes.prepend(1234);
        bytes.prewrite(",hi,".getBytes(ISO_8859_1));
        Bytes<?> words = Bytes.from("words");
        bytes.prewrite(words);
        bytes.prewriteByte((byte) ',');
        bytes.prewriteInt(0x34333231);
        bytes.prewriteLong(0x3837363534333231L);
        bytes.prewriteShort((short) 0x3130);
        assertEquals("01123456781234,words,hi,1234", bytes.toString(), "Prewrite operations should build content backwards from end");

        bytes.releaseLast();
        words.releaseLast();
    }
}
