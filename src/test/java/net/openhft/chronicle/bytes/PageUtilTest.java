/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.DisplayName;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests PageUtil page size parsing and mount info detection because
 * correct huge page configuration is essential for optimal memory-mapped
 * file performance on Linux systems.
 */
@SuppressWarnings("MMOverusedWord") // page domain terminology
@DisplayName("PageUtil parses page sizes and mount info")
class PageUtilTest {

    @Test
    @DisplayName("default page size derived from OS value")
    void getDefaultPageSize() throws Exception {
        File file = Files.createTempFile("page-util", "file").toFile();
        file.deleteOnExit();
        assertEquals(OS.defaultOsPageSize(), PageUtil.getPageSize(file.getAbsolutePath()),
                "Page size matches OS default for temp file");
    }

    @Test
    @DisplayName("page size from huge mount path")
    void getPageSize() {
        assumeTrue(OS.isLinux(),
                "Linux platform required for huge page detection");
        assumeTrue(Files.exists(Paths.get("/mnt/huge")),
                "Huge page mount must exist for size check");
        assertEquals(DEFAULT_HUGE_PAGE_SIZE, PageUtil.getPageSize("/mnt/huge"),
                "Huge mount page size matches default huge size");
    }

    @Test
    @DisplayName("huge page detection from mount path")
    void isHugePage() {
        assumeTrue(OS.isLinux(),
                "Linux platform required for huge page mount checks");
        assumeTrue(Files.exists(Paths.get("/mnt/huge")),
                "Huge page mount must exist for huge check");
        assertTrue(PageUtil.isHugePage("/mnt/huge"),
                "Mount path should be detected as huge page");
    }

    @Test
    @DisplayName("mount info read returns full expected line list")
    void readMountInfo() throws Exception {
        File file = Files.createTempFile("mountinfo", "file").toFile();
        file.deleteOnExit();
        List<String> lines = asList(
                "133 162 253:2 / /home rw,relatime shared:74 - xfs /dev/mapper/rl-home rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,noquota",
                "136 162 253:2 /local /mnt/local rw,relatime shared:74 - xfs /dev/mapper/rl-home rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,noquota",
                "1110 162 0:61 / /mnt/huge rw,relatime shared:591 - hugetlbfs nodev rw,seclabel,pagesize=2M,size=68719476");
        Files.write(file.toPath(), lines);

        List<String> result = PageUtil.readMountInfo(file.getAbsolutePath());
        assertEquals(lines, result,
                "Mount info read returns full expected mount lines");
    }

    @SuppressWarnings("MMAnnotationTestOrder") // ParameterizedTest before Test is intentional
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
    @DisplayName("parse actual page size from mount info")
    void parseActualPageSize(String line, int expected) {
        int result = PageUtil.parsePageSize(line);
        assertEquals(expected, result,
                "Parsed page size matches expected mount value");
    }

    @Test
    @DisplayName("parse default page size when missing")
    void parseDefaultPageSize() throws Exception {
        String line = "136 162 253:2 /local /mnt/local rw,relatime shared:74 - xfs /dev/mapper/rl-home rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,noquota";

        int result = PageUtil.parsePageSize(line);
        assertEquals(DEFAULT_HUGE_PAGE_SIZE, result,
                "Default huge page size used when missing");
    }

    @Test
    @DisplayName("parse mount point from mount info line")
    void parseMountPoint() throws Exception {
        String line = "1110 162 0:61 / /mnt/huge rw,relatime shared:591 - hugetlbfs nodev rw,seclabel,pagesize=4M,size=68719476";

        String result = PageUtil.parseMountPoint(line);
        assertEquals("/mnt/huge", result,
                "Parsed mount point matches expected path");
    }

    @Test
    @DisplayName("trie insert stores page size by path")
    void insertTest() throws Exception {
        int gib = 1 << 30;
        Field field = Jvm.getField(PageUtil.class, "root");
        field.setAccessible(true);
        PageUtil.TrieNode root = (PageUtil.TrieNode) field.get(null);

        PageUtil.insert("/mnt/huge", gib);

        assertNotNull(root,
                "Root trie node is available after reflection");
        assertNotNull(root.childs.get("mnt"),
                "Trie contains mnt child node after insert");
        assertNotNull(root.childs.get("mnt").childs.get("huge"),
                "Trie contains huge child under mnt node");
        assertEquals(gib, root.childs.get("mnt").childs.get("huge").pageSize,
                "Trie node page size matches inserted value");
    }
}
