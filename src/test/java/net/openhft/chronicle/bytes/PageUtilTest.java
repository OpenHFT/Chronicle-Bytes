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

import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static net.openhft.chronicle.bytes.PageUtil.DEFAULT_HUGE_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PageUtilTest {

    @Test
    void getDefaultPageSize() throws Exception {
        File file = Files.createTempFile("page-util", "file").toFile();
        file.deleteOnExit();
        assertEquals(OS.defaultOsPageSize(), PageUtil.getPageSize(file.getAbsolutePath()));
    }

    @Test
    void getPageSize() {
        assumeTrue(OS.isLinux());
        assumeTrue(Files.exists(Paths.get("/mnt/huge")));
        assertEquals(DEFAULT_HUGE_PAGE_SIZE, PageUtil.getPageSize("/mnt/huge"));
    }

    @Test
    void isHugePage() {
        assumeTrue(OS.isLinux());
        assumeTrue(Files.exists(Paths.get("/mnt/huge")));
        assertTrue(PageUtil.isHugePage("/mnt/huge"));
    }
}