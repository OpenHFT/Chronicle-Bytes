/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

public final class CanonicalPathUtil {

    private CanonicalPathUtil() {}

    /**
     * Returns an internalized String that can be used to synchronize on across classloaders
     *
     * @param file to use
     * @return internalized String
     */
    public static String of(@NotNull final File file) {
        try {
            return file.getCanonicalPath().intern();
        } catch (IOException ioe) {
            throw new IORuntimeException("Unable to obtain the canonical path for " + file.getAbsolutePath(), ioe);
        }
    }
}
