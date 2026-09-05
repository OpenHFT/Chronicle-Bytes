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
import static org.junit.Assume.assumeFalse;

/**
 * Field offsets depend on the JVM: the object header is 12 bytes on 64-bit HotSpot with compressed class pointers
 * but 8 bytes on a 32-bit JVM or with compact object headers, and JDK 15 changed the field ordering
 * (JDK-8237767). What {@link BytesFieldInfo} owes its callers is that each group starts at its first field, ends
 * after its last field and contains no field of another group, so the expectations are derived from the actual
 * field offsets rather than pinned to one layout.
 */
public class BytesFieldInfoTest extends BytesTestCommon {

    //! The previous expectations pinned the field offsets of 64-bit HotSpot with compressed class pointers, where the
    //! object header is 12 bytes and the first int lands at offset 12. OpenJDK 17 i386 has an 8-byte header and lays the
    //! longs out first (Groups1 reports pad: 8 to 40 ... header: 88 to 92), so lookup() failed there although
    //! BytesFieldInfo reported the real layout; compact object headers give 64-bit JVMs the same 8-byte header, and JDK 15
    //! already forced one layout switch in this test. The expectations are derived from the JVM's own field offsets,
    //! enumerated by reflection rather than through BytesFieldInfo.fields so an enumeration error in the code under test
    //! cannot mirror into the expectation; group names and the description word stay hard-coded. This test is its own
    //! discriminator: with the pinned offsets it fails on the 32-bit JDK and passes on 64-bit HotSpot; with the derived
    //! ranges it passes on OpenJDK 21 amd64, 17 i386 and 8 amd64. The "no field of another group inside a group's range"
    //! assertion is stronger than BytesFieldInfo's contract, which extends a group across interleaved un-annotated fields;
    //! a failure there on some layout would be a real finding about the group being non-contiguous.
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
        final List<Field> fields = fieldsByOffset(type);
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
        // the dump lists the groups in offset order
        final String groups = ranges.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue()[0]))
                .map(e -> e.getKey() + ": " + e.getValue()[0] + " to " + e.getValue()[1])
                .collect(Collectors.joining(", "));
        assertEquals("type: BytesFieldInfo, groups: { " + groups + " }", lookup.dump());
    }

    /** every non-static field of {@code type} and its super classes, ordered by memory offset, enumerated by reflection */
    private static List<Field> fieldsByOffset(Class<?> type) {
        final List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass())
            for (Field field : c.getDeclaredFields())
                if (!Modifier.isStatic(field.getModifiers()))
                    fields.add(field);
        fields.sort(Comparator.comparingLong(MEMORY::getFieldOffset));
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
