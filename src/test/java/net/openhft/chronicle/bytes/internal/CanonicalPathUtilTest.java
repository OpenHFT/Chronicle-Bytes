/*
 * Copyright 2016-2025 chronicle.software
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

