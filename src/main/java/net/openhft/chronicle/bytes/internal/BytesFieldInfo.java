/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.core.ClassLocal;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

public class BytesFieldInfo {
    private static final ClassLocal<BytesFieldInfo> CACHE = ClassLocal.withInitial(BytesFieldInfo::init);
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

    BytesFieldInfo(Class<?> aClass) {
        this.aClass = aClass;
        List<Field> fields = fields(aClass);
        String prefix0 = "";
        BFIEntry entry = null;
        int longs = 0;
        int ints = 0;
        int shorts = 0;
        int bytes = 0;
        boolean nonHeader = false;
        boolean hasHeader = false;
        for (int i = 0; i <= fields.size(); i++) {
            final Field field = i == fields.size() ? $END$ : fields.get(i);
            boolean matches = false;
            String prefix = "";
            long position = 0;
            int size = 0;
            if (field.getType().isPrimitive()) {
                FieldGroup fieldGroup = field.getAnnotation(FieldGroup.class);
                if (fieldGroup != null) {
                    prefix = fieldGroup.value();
                    if (prefix.equals(FieldGroup.HEADER)) {
                        hasHeader = true;
                        if (nonHeader)
                            throw new IllegalStateException("The " + FieldGroup.HEADER + " FieldGroup must be at the start");
                        continue;
                    }
                    nonHeader = true;
                    position = MEMORY.getFieldOffset(field);
                    matches = prefix.equals(prefix0);
                }
                size = sizeOf(field.getType());
                switch (size) {
                    case 1:
                        bytes++;
                        break;
                    case 2:
                        assert !hasHeader || bytes == 0;
                        shorts++;
                        break;
                    case 4:
                        assert !hasHeader || (shorts == 0 && bytes == 0);
                        ints++;
                        break;
                    case 8:
                        assert !hasHeader || (ints == 0 && shorts == 0 && bytes == 0);
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

    private static int sizeOf(Class<?> type) {
        return Memory.sizeOf(type);
    }

    public int description() {
        return description;
    }

    static class BFIEntry {
        long start;
        long end;
    }

    private static BytesFieldInfo init(Class<?> aClass) {
        return new BytesFieldInfo(aClass);
    }

    public static BytesFieldInfo lookup(Class<?> aClass) {
        return CACHE.get(aClass);
    }

    public Set<String> groups() {
        return groups.keySet();
    }

    public long startOf(String groupName) {
        final BFIEntry bfiEntry = groups.get(groupName);
        if (bfiEntry == null)
            throw new IllegalArgumentException("No groupName " + groupName + " found in " + aClass);
        return bfiEntry.start;
    }

    public long lengthOf(String groupName) {
        final BFIEntry bfiEntry = groups.get(groupName);
        if (bfiEntry == null)
            throw new IllegalArgumentException("No groupName " + groupName + " found in " + aClass);
        return bfiEntry.end - bfiEntry.start;
    }

    public String dump() {
        final StringBuilder sb = new StringBuilder().append("type: ").append(getClass().getSimpleName()).append(", groups: { ");
        sb.append(groups.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue().start + " to " + e.getValue().end)
                .collect(Collectors.joining(", ")));
        return sb.append(" }").toString();
    }

    // internal only
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
