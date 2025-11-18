/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class CanonicalPathUtilTest extends BytesTestCommon {

    @Test
    public void returnsInternedCanonicalPath() throws IOException {
        File dir = new File(OS.getTarget(), "canon-test");
        assertTrue(dir.mkdirs() || dir.isDirectory());
        File f1 = new File(dir, "a/.././file.txt");
        File f2 = new File(dir, "./file.txt");

        // ensure file exists
        File parent = f2.getParentFile();
        assertTrue(parent.mkdirs() || parent.isDirectory());
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f2), StandardCharsets.ISO_8859_1)) {
            writer.write("x");
        }

        String p1 = CanonicalPathUtil.of(f1);
        String p2 = CanonicalPathUtil.of(f2);

        assertEquals(p1, p2);
        assertSame("String must be interned", p1, p1.intern());
        assertSame("Same canonical path must be same instance", p1, p2);
    }
}
