package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.ClassLocal;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BytesFieldInfo {
    private static final ClassLocal<BytesFieldInfo> CACHE = ClassLocal.withInitial(BytesFieldInfo::init);
    static final Pattern nameDigits = Pattern.compile("^(\\w*\\D)\\d+$");
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

    BytesFieldInfo(Class<?> aClass) {
        this.aClass = aClass;
        List<Field> fields = Stream.of(aClass.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .sorted(Comparator.comparingLong(UnsafeMemory.MEMORY::getFieldOffset))
                .collect(Collectors.toList());
        String prefix0 = "";
        BFIEntry entry = null;
        for (int i = 0; i <= fields.size(); i++) {
            final Field field = i == fields.size() ? $END$ : fields.get(i);
            boolean matches = false;
            String prefix = "";
            long position = 0;
            int size = 0;
            if (field.getType().isPrimitive()) {
                final Matcher matcher = nameDigits.matcher(field.getName());
                if (matcher.matches()) {
                    position = UnsafeMemory.MEMORY.getFieldOffset(field);
                    size = sizeOf(field.getType());

                    prefix = matcher.group(1);
                    matches = prefix.equals(prefix0);
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
    }

    private static int sizeOf(Class<?> type) {
        return type == boolean.class || type == byte.class ? 1
                : type == short.class || type == char.class ? 2
                : type == int.class || type == float.class ? 4
                : type == long.class || type == double.class ? 8
                : Unsafe.ARRAY_OBJECT_INDEX_SCALE;
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
}
