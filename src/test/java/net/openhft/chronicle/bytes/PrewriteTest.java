/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PrewriteTest extends BytesTestCommon {
    @Test
    public void test() {
        Bytes<?> bytes = Bytes.allocateDirect(64);
        bytes.clearAndPad(64);
        bytes.prepend(1234);
        bytes.prewrite(",hi,".getBytes());
        Bytes<?> words = Bytes.from("words");
        bytes.prewrite(words);
        bytes.prewriteByte((byte) ',');
        bytes.prewriteInt(0x34333231);
        bytes.prewriteLong(0x3837363534333231L);
        bytes.prewriteShort((short) 0x3130);
        assertEquals("01123456781234,words,hi,1234", bytes.toString());

        bytes.releaseLast();
        words.releaseLast();
    }
}
