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
 * Field offsets depend on the JVM: the object header is 12 bytes on 64-bit HotSpot with compressed class pointers
 * but 8 bytes on a 32-bit JVM or with compact object headers, and JDK 15 changed the field ordering
 * (JDK-8237767). What {@link BytesFieldInfo} owes its callers is that each group starts at its first field and ends
 * after its last field, so the expectations are derived from the actual field offsets rather than pinned to one
 * layout. That no field of another group lies inside a group's range is a property of these fixtures, asserted as
 * well, not a promise of the API.
 */
public class BytesFieldInfoTest extends BytesTestCommon {

    //! The old expectations pinned 64-bit HotSpot offsets (12-byte header, longs first), which is not what BytesFieldInfo
    //! promises: OpenJDK 17 i386 (8-byte header, a different order) failed them while BytesFieldInfo reported the real
    //! layout, and compact object headers would do the same on 64-bit. The ranges are now derived from reflection and
    //! MEMORY.getFieldOffset, independent of BytesFieldInfo.fields so an enumeration error cannot mirror into the
    //! expectation; group names and the description word stay pinned. With the pinned offsets this test fails on the
    //! 32-bit JDK; with the derived ranges it passes on OpenJDK 21 amd64, 17 i386 and 8 amd64. The "no field of another
    //! group inside a range" assertion is a property of these fixtures, stronger than the contract, which lets a group
    //! extend across interleaved un-annotated fields.
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
            for (Field field : fields) {
                final long offset = MEMORY.getFieldOffset(field);
                if (range[0] <= offset && offset < range[1]) {
                    final FieldGroup group = field.getAnnotation(FieldGroup.class);
                    assertEquals(field.getName() + " lies inside the range of group " + name,
                            name, group == null ? null : group.value());
                }
            }
        }
        // the dump lists the groups in offset order; its exact format is not part of the contract
        final List<String> byOffset = ranges.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue()[0]))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        final String dump = lookup.dump();
        for (int i = 1; i < byOffset.size(); i++)
            assertTrue(byOffset.get(i - 1) + " before " + byOffset.get(i) + " in " + dump,
                    dump.indexOf(byOffset.get(i - 1) + ":") < dump.indexOf(byOffset.get(i) + ":"));
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
