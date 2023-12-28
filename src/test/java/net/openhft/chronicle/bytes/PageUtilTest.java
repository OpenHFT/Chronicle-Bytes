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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;
import static net.openhft.chronicle.bytes.PageUtil.DEFAULT_HUGE_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    void readMountInfo() throws Exception {
        File file = Files.createTempFile("mountinfo", "file").toFile();
        file.deleteOnExit();
        List<String> lines = asList(
                "133 162 253:2 / /home rw,relatime shared:74 - xfs /dev/mapper/rl-home rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,noquota",
                "136 162 253:2 /local /mnt/local rw,relatime shared:74 - xfs /dev/mapper/rl-home rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,noquota",
                "1110 162 0:61 / /mnt/huge rw,relatime shared:591 - hugetlbfs nodev rw,seclabel,pagesize=2M,size=68719476");
        Files.write(file.toPath(), lines);

        List<String> result = PageUtil.readMountInfo(file.getAbsolutePath());
        assertEquals(lines, result);
    }

    @CsvSource(delimiter = '|',
            value = {
                    "hugetlbfs nodev rw,seclabel,pagesize=512K,size=68719476|524288",
                    "hugetlbfs nodev rw,seclabel,pagesize=2M,size=68719476|2097152",
                    "hugetlbfs nodev rw,seclabel,pagesize=4M,size=68719476|4194304",
                    "hugetlbfs nodev rw,seclabel,pagesize=512M,size=68719476|536870912",
                    "hugetlbfs nodev rw,seclabel,pagesize=1024M,size=68719476|1073741824",
                    "hugetlbfs nodev rw,seclabel,pagesize=1G,size=68719476|1073741824"
            })
    @ParameterizedTest
    void parseActualPageSize(String line, int expected) {
        int result = PageUtil.parsePageSize(line);
        assertEquals(expected, result);
    }

    @Test
    void parseDefaultPageSize() throws Exception {
        String line = "136 162 253:2 /local /mnt/local rw,relatime shared:74 - xfs /dev/mapper/rl-home rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,noquota";

        int result = PageUtil.parsePageSize(line);
        assertEquals(DEFAULT_HUGE_PAGE_SIZE, result);
    }

    @Test
    void parseMountPoint() throws Exception {
        String line = "1110 162 0:61 / /mnt/huge rw,relatime shared:591 - hugetlbfs nodev rw,seclabel,pagesize=4M,size=68719476";

        String result = PageUtil.parseMountPoint(line);
        assertEquals("/mnt/huge", result);
    }

    @Test
    void insertTest() throws Exception {
        int G = 1 << 30;
        Field field = Jvm.getField(PageUtil.class, "root");
        field.setAccessible(true);
        PageUtil.TrieNode root = (PageUtil.TrieNode) field.get(null);

        PageUtil.insert("/mnt/huge", G);

        assertNotNull(root);
        assertNotNull(root.childs.get("mnt"));
        assertNotNull(root.childs.get("mnt").childs.get("huge"));
        assertEquals(G, root.childs.get("mnt").childs.get("huge").pageSize);
    }
}
