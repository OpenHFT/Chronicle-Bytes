/*
 * Copyright 2016-2025 chronicle.software
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

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Utility for obtaining and internalising canonical file paths. This allows the
 * same physical file to be referenced by a unique {@link String} token even when
 * different {@link File} objects are used.
 */
public final class CanonicalPathUtil {

    private CanonicalPathUtil() {}

    /**
     * Returns the canonical path of {@code file} as an interned {@link String}.
     * This provides a unique monitor object for synchronising across different
     * {@code File} instances that reference the same path.
     *
     * @param file the file for which to obtain the canonical path
     * @return interned canonical path
     * @throws IORuntimeException if the canonical path cannot be resolved
     */
    public static String of(@NotNull final File file) {
        try {
            return file.getCanonicalPath().intern();
        } catch (IOException ioe) {
            throw new IORuntimeException("Unable to obtain the canonical path for " + file.getAbsolutePath(), ioe);
        }
    }
}
