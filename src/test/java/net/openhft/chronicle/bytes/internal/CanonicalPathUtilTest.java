/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Canonical path interning and normalisation checks")
public class CanonicalPathUtilTest extends BytesTestCommon {

    @Test
    @DisplayName("canonical paths return same interned instance")
    public void returnsInternedCanonicalPath() throws IOException {
        File dir = new File(OS.getTarget(), "canon-test");
        assertTrue(dir.mkdirs() || dir.isDirectory(),
                "Target directory exists or is created");
        File f1 = new File(dir, "a/.././file.txt");
        File f2 = new File(dir, "./file.txt");

        // ensure file exists
        File parent = f2.getParentFile();
        assertTrue(parent.mkdirs() || parent.isDirectory(),
                "Parent directory exists or is created");
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(f2),
                StandardCharsets.ISO_8859_1)) {
            fw.write("x");
        }

        String p1 = CanonicalPathUtil.of(f1);
        String p2 = CanonicalPathUtil.of(f2);

        assertEquals(p1, p2,
                "Canonical path strings should match");
        assertSame(p1, p1.intern(),
                "String must be interned");
        assertSame(p1, p2,
                "Same canonical path must be same instance");
    }
}
