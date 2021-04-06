package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.core.ClassLocal;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

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
        int longs = 0, ints = 0, shorts = 0, bytes = 0;
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
                    position = UnsafeMemory.MEMORY.getFieldOffset(field);
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
                }
            }
            if (matches) {
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
        int description = (longs << 24) | (ints << 16) | (shorts << 8) | bytes;
        // ensure the header has an odd parity as a validity check
        if (Integer.bitCount(description) % 2 == 0)
            description |= 0x8000;
        this.description = description;
    }

    private static int sizeOf(Class<?> type) {
        return type == boolean.class || type == byte.class ? 1
                : type == short.class || type == char.class ? 2
                : type == int.class || type == float.class ? 4
                : type == long.class || type == double.class ? 8
                : Unsafe.ARRAY_OBJECT_INDEX_SCALE;
    }

    public int description() {
        return description;
    }

    static class BFIEntry {
        long start, end;
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
    public static List<Field> fields(Class clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            Collections.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
        fields.removeIf(field -> Modifier.isStatic(field.getModifiers()));
        fields.sort(Comparator.comparingLong(UnsafeMemory.UNSAFE::objectFieldOffset));
        return fields;
    }
}
