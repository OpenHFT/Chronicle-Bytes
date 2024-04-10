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
import net.openhft.chronicle.core.annotation.Positive;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.text.MessageFormat.format;

/**
 * Provides file systems' page size auto-resolving.
 * Linux and hugetlbfs specific only for now.
 */
public final class PageUtil {

    public static final int DEFAULT_HUGE_PAGE_SIZE = 2 * 1024 * 1024;

    private static final Pattern PAGE_SIZE_PATTERN = Pattern.compile("pagesize=([0-9]+)([KkMmGg])");
    private static final TrieNode root = new TrieNode();

    static {
        if (OS.isLinux()) {
            List<String> mounts = readMountInfo("/proc/self/mountinfo");
            for (String mount : mounts) {
                if (mount.contains("hugetlbfs")) {
                    int size = parsePageSize(mount);
                    String path = parseMountPoint(mount);
                    insert(path, size);
                }
            }
        }
    }

    static List<String> readMountInfo(String path) {
        try {
            return Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            Jvm.warn().on(PageUtil.class, format("Error reading ''{0}'': {1}", path, e.getMessage()), e);
            return Collections.emptyList();
        }
    }

    static int parsePageSize(String mount) {
        Matcher matcher = PAGE_SIZE_PATTERN.matcher(mount);
        if (matcher.find())
            try {
                return Integer.parseInt(matcher.group(1)) * mult(matcher.group(2));
            }
        catch (Exception e) {
            Jvm.warn().on(PageUtil.class, format("Error parsing pageSize={0}: {1}", matcher.group(1), e.getMessage()));
        }
        return DEFAULT_HUGE_PAGE_SIZE;
    }

    private static int mult(String s) {
        int k = 1024;
        if (s.equalsIgnoreCase("K"))
            return k;
        if (s.equalsIgnoreCase("G"))
            return k * 1024 * 1024;

        return k * 1024;
    }

    static String parseMountPoint(String line) {
        String[] parts = line.split("\\s+");
        assert parts[4].matches("^\\S+$");
        return parts[4];
    }

    static void insert(String path, int size) {
        TrieNode curr = root;
        for (String dir : path.split("/")) {
            if (dir.isEmpty()) continue;
            curr.childs.putIfAbsent(dir, new TrieNode());
            curr = curr.childs.get(dir);
        }
        curr.isLeaf = true;
        curr.pageSize = size;
        Jvm.perf().on(PageUtil.class, format("Found pageSize={0} for mount point ''{1}''", size, path));
    }

    /**
     * Returns page size obtained from auto-scanned hugetlbfs mount points
     * or OS default page size for a given absolute file path
     * @param absolutePath file path
     */
    @Positive
    public static int getPageSize(@NotNull String absolutePath) {
        if (OS.isLinux()) {
            String[] dirs = absolutePath.split("/");
            TrieNode curr = root;
            for (int i = 0; i < dirs.length && curr != null && !curr.isLeaf; i++) {
                if (dirs[i].isEmpty()) continue;
                curr = curr.childs.get(dirs[i]);
            }
            if (curr != null && curr.isLeaf && curr.pageSize != 0)
                return curr.pageSize;
        }
        return OS.defaultOsPageSize();
    }

    /**
     * Whether given file is located on hugetlbfs
     * @param absolutePath file path
     * @return true if file is located on hugetlbfs
     */
    public static boolean isHugePage(@NotNull String absolutePath) {
        return OS.isLinux() && getPageSize(absolutePath) != OS.defaultOsPageSize();
    }

    static class TrieNode {
        boolean isLeaf = false;
        final Map<String, TrieNode> childs = new HashMap<>();

        int pageSize;
    }
}
