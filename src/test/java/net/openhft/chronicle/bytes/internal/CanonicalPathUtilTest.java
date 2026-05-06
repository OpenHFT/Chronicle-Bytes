/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
        try (FileWriter fw = new FileWriter(f2)) {
            fw.write("x");
        }

        String p1 = CanonicalPathUtil.of(f1);
        String p2 = CanonicalPathUtil.of(f2);

        assertEquals(p1, p2);
        assertSame("String must be interned", p1, p1.intern());
        assertSame("Same canonical path must be same instance", p1, p2);
    }
}

