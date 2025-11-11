/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.util.ClassLocal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

/**
 * Inspects a class to determine the layout of its primitive fields, especially
 * those annotated with {@link net.openhft.chronicle.bytes.FieldGroup}. It
 * computes contiguous memory ranges for each group and summarises the primitive
 * counts in a single integer. Results are cached per class to avoid repeated
 * reflection.
 */
public class BytesFieldInfo {
    /** cache to avoid repeated reflection for the same class */
    private static final ClassLocal<BytesFieldInfo> CACHE = ClassLocal.withInitial(BytesFieldInfo::init);
    /** sentinel field used when scanning for group boundaries */
    static final Field $END$;

    static {
        try {
            $END$ = BytesFieldInfo.class.getDeclaredField("$END$");
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private final Map<String, BFIEntry> groups = new LinkedHashMap<>();
    private final Class<?> aClass;
    private final int description;

    /**
     * Scans {@code aClass} and its super classes for fields, grouping them by
     * {@link FieldGroup} annotations and calculating their offsets.
     */
    BytesFieldInfo(Class<?> aClass) {
        this.aClass = aClass;
        List<Field> fields = fields(aClass);
        String prefix0 = "";
        BFIEntry entry = null;
        int longs = 0;
        int ints = 0;
        int shorts = 0;
        int bytes = 0;
        for (int i = 0; i <= fields.size(); i++) {
            final Field field = i == fields.size() ? $END$ : fields.get(i);
            boolean matches = false;
            String prefix = "";
            long position = 0;
            int size = 0;
            if (field.getType().isPrimitive()) {
                FieldGroup fieldGroup = Jvm.findAnnotation(field, FieldGroup.class);
                if (fieldGroup != null) {
                    prefix = fieldGroup.value();
                    position = MEMORY.getFieldOffset(field);
                    matches = prefix.equals(prefix0);
                }
                size = sizeOf(field.getType());
                switch (size) {
                    case 1:
                        bytes++;
                        break;
                    case 2:
                        shorts++;
                        break;
                    case 4:
                        ints++;
                        break;
                    case 8:
                        longs++;
                        break;
                    default:
                        throw new UnsupportedOperationException("Primitive types of size " + size + " not supported");
                }
            }
            if (matches) {
                assert entry != null;
                entry.end = position + size;

            } else if (!prefix.isEmpty()) {
                if (this.groups.containsKey(prefix)) {
                    Jvm.warn().on(aClass, "Disjoined fields starting with " + prefix);
                    prefix0 = "";

                } else {
                    entry = new BFIEntry();
                    entry.start = position;
                    entry.end = position + size;
                    this.groups.put(prefix, entry);
                    prefix0 = prefix;
                }
            }
        }
        assert longs < 256;
        assert ints < 256;
        assert shorts < 128;
        assert bytes < 256;
        int newDescription = (longs << 24) | (ints << 16) | (shorts << 8) | bytes;
        // ensure the header has an odd parity as a validity check
        if (Integer.bitCount(newDescription) % 2 == 0)
            newDescription |= 0x8000;
        this.description = newDescription;
    }

    /**
     * Computes the size of the given type in bytes.
     *
     * @param type the class representing the primitive type
     * @return the size of the type in bytes
     */
    private static int sizeOf(Class<?> type) {
        return Memory.sizeOf(type);
    }

    /**
     * @return an integer encoding counts of primitive field types with an odd
     *         parity bit as a basic validity check
     */
    public int description() {
        return description;
    }

    /** holds start and end offsets for a field group */
    static class BFIEntry {
        long start;
        long end;
    }

    /**
     * Factory method for creating a new BytesFieldInfo instance for the given class.
     *
     * @param aClass the class to analyze
     * @return a BytesFieldInfo instance
     */
    private static BytesFieldInfo init(Class<?> aClass) {
        return new BytesFieldInfo(aClass);
    }

    /**
     * Retrieves a cached {@code BytesFieldInfo} for {@code aClass} or creates
     * one if absent.
     */
    public static BytesFieldInfo lookup(Class<?> aClass) {
        return CACHE.get(aClass);
    }

    /**
     * Returns a set of group names extracted from the field annotations.
     *
     * @return a set of group names
     */
    public Set<String> groups() {
        return groups.keySet();
    }

    /**
     * @return start offset (relative to the object header) for the named group
     */
    public long startOf(String groupName) {
        final BFIEntry bfiEntry = groups.get(groupName);
        if (bfiEntry == null)
            throw new IllegalArgumentException("No groupName " + groupName + " found in " + aClass);
        return bfiEntry.start;
    }

    /**
     * @return length in bytes of the named group
     */
    public long lengthOf(String groupName) {
        final BFIEntry bfiEntry = groups.get(groupName);
        if (bfiEntry == null)
            throw new IllegalArgumentException("No groupName " + groupName + " found in " + aClass);
        return bfiEntry.end - bfiEntry.start;
    }

    /**
     * @return a human readable dump of the discovered groups and their offsets
     */
    public String dump() {
        final StringBuilder sb = new StringBuilder().append("type: ").append(getClass().getSimpleName()).append(", groups: { ");
        sb.append(groups.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue().start + " to " + e.getValue().end)
                .collect(Collectors.joining(", ")));
        return sb.append(" }").toString();
    }

    /**
     * Utility used during construction: returns all non-static, non-transient
     * fields from {@code clazz} and its super classes ordered by memory offset.
     */
    public static List<Field> fields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            Collections.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
        fields.removeIf(field -> Modifier.isStatic(field.getModifiers()));
        fields.sort(Comparator.comparingLong(MEMORY::objectFieldOffset));
        return fields;
    }
}
