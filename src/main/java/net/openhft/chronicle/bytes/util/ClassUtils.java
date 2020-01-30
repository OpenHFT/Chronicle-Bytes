package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

import static net.openhft.chronicle.bytes.BytesUtil.readFile;
import static net.openhft.chronicle.bytes.BytesUtil.writeFile;
import static net.openhft.chronicle.core.ClassMetrics.updateJar;

public class ClassUtils {

    private void updateClass(Class aClass, long find, long replace, String domain, @Nullable UpdateConsumer consumer) throws IOException {
        boolean wasUpdated = false;
        String file = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        String dir = file.replaceAll("target/.*", "target");
        for (File jar : new File(dir).listFiles()) {
            if (jar.getName().endsWith(".jar") && !jar.getName().contains("guarded")) {
                updateClass(aClass, find, replace, jar, domain, consumer);
                wasUpdated = true;
            }
        }

        if (!wasUpdated)
            throw new IllegalStateException("failed to update any class");
    }

    private void updateClass(Class aClass, long find, long replace, File jar, String domain, @Nullable UpdateConsumer consumer) throws IOException {
        String classPath = aClass.getName().replace('.', '/').concat(".class");
        String file = aClass.getClassLoader().getResource(classPath).getFile();
        Bytes<byte[]> bytes = readFile(file);
        long find2 = Long.reverseBytes(find);
        long replace2 = Long.reverseBytes(replace);
        for (int i = 0; i < bytes.readRemaining() - 7; i++) {
            long l = bytes.readLong(i);
            if (l == find2 || l == replace2) {

                bytes.writeLong(i, replace2);
                writeFile(file + ".tmp", bytes);
                updateJar(jar.getAbsolutePath(), file + ".tmp", classPath);
                if (consumer != null)
                    consumer.accept(jar, replace, domain, file);
                return;
            }
        }
        throw new AssertionError("Unable to find magic in " + file);
    }

    interface UpdateConsumer {
        void accept(File jar, long replace, String domain, String file);
    }
}
