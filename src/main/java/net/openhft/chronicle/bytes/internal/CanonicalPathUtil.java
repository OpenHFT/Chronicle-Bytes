package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

public final class CanonicalPathUtil {

    // This is unlikely to collide with other internalized strings and is seldom the same across JVMs
    private static final String SUFFIX = CanonicalPathUtil.class.getName() + Math.abs(new SecureRandom().nextInt(1 << 30));

    private CanonicalPathUtil() {}

    /**
     * Returns an internalized String that is unlikely to collide with other internalized strings in the JVM.
     *
     * @param file to use
     * @return internalized String
     */
    public static String of(@NotNull final File file) {
        try {
            return SUFFIX + "::" + file.getCanonicalPath().intern();
        } catch (IOException ioe) {
            throw new IORuntimeException("Unable to obtain the canonical path for " + file.getAbsolutePath(), ioe);
        }
    }
}