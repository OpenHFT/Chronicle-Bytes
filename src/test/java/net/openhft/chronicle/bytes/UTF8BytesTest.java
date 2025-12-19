/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class UTF8BytesTest extends BytesTestCommon {

    private static final String MESSAGE = "awésome-message-1";

    @Test
    public void testUtfEncoding() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        File f = Files.createTempFile("testUtfEncoding", "data").toFile();
        f.deleteOnExit();
        final MappedBytes bytes = MappedBytes.mappedBytes(f, 256, 0);
        int len = (int) AppendableUtil.findUtf8Length(MESSAGE);
        bytes.appendUtf8(MESSAGE);

        StringBuilder sb = new StringBuilder();
        bytes.parseUtf8(sb, true, len);
        assertEquals(MESSAGE, sb.toString(), "UTF-8 round-trip via appendUtf8/parseUtf8 should preserve string with non-ASCII characters");
        bytes.releaseLast();
    }
}
