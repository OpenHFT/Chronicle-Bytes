/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Checks group ranges against independently enumerated fields in the current JVM layout.
 */
public class BytesFieldInfoTest extends BytesTestCommon {

    @Test
    public void lookup() {
        assumeFalse(Jvm.isArm() || Jvm.isAzulZing());
        assertGroups(Groups1.class, "a070000", "header", "pad", "other", "hi", "add");
        assertGroups(Groups2.class, "4050000", "pad", "hi", "add");
        assertGroups(Groups3.class, "4050000", "pad", "hi", "add");
    }

    private static void assertGroups(Class<?> type, String description, String... groupNames) {
        final BytesFieldInfo lookup = BytesFieldInfo.lookup(type);
        assertEquals(description, Integer.toHexString(lookup.description()));
        assertEquals(new HashSet<>(Arrays.asList(groupNames)), lookup.groups());

        // each group's range as this JVM laid it out: from its first field to the end of its last field
        final Map<String, long[]> ranges = new LinkedHashMap<>();
        final List<Field> fields = instanceFields(type);
        for (Field field : fields) {
            final FieldGroup group = field.getAnnotation(FieldGroup.class);
            if (group == null)
                continue;
            final long start = MEMORY.getFieldOffset(field);
            final long end = start + Memory.sizeOf(field.getType());
            final long[] range = ranges.computeIfAbsent(group.value(), name -> new long[]{start, end});
            range[0] = Math.min(range[0], start);
            range[1] = Math.max(range[1], end);
        }
        for (String name : groupNames) {
            final long[] range = ranges.get(name);
            assertEquals(name + " start", range[0], lookup.startOf(name));
            assertEquals(name + " length", range[1] - range[0], lookup.lengthOf(name));
        }
        // the dump lists the groups in offset order; its exact format is not part of the contract
        final List<String> byOffset = ranges.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue()[0]))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        final String dump = lookup.dump();
        int previousIndex = -1;
        for (String group : byOffset) {
            final int index = dump.indexOf(group + ":");
            assertTrue(group + " is missing from " + dump, index >= 0);
            assertTrue(group + " must follow the previous group in " + dump, index > previousIndex);
            previousIndex = index;
        }
    }

    /** every non-static field of {@code type} and its super classes, enumerated by reflection; order is irrelevant */
    private static List<Field> instanceFields(Class<?> type) {
        final List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass())
            for (Field field : c.getDeclaredFields())
                if (!Modifier.isStatic(field.getModifiers()))
                    fields.add(field);
        return fields;
    }

    private static class Groups1 {
        @FieldGroup("header")
        int header;
        @FieldGroup("pad")
        long pad0, pad1, pad3, pad2;
        double d1, d2;
        @FieldGroup("other")
        long pad10, pad11, pad13, pad12;
        @FieldGroup("hi")
        int hi99;
        float f;
        @FieldGroup("add")
        int add1, add2, add3, add4;
    }

    private static class Groups2 {
        @FieldGroup("pad")
        long pad0, pad1, pad3, pad2;
        @FieldGroup("hi")
        int hi99;
        @FieldGroup("add")
        int add1, add2, add3, add4;
    }

    static class GroupsBase {
        @FieldGroup("pad")
        long pad0, pad1, pad3, pad2;
    }

    private static class Groups3 extends GroupsBase {
        @FieldGroup("hi")
        int hi99;
        @FieldGroup("add")
        int add1, add2, add3, add4;
    }
}
