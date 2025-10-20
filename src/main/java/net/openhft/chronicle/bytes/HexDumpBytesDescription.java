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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;

import static net.openhft.chronicle.core.Jvm.uncheckedCast;

/**
 * Provides hooks for adding comments and controlling indentation when generating hex dumps.
 */
public interface HexDumpBytesDescription<B extends HexDumpBytesDescription<B>> {
    /**
     * @return {@code true} if comments are retained for later inclusion in the hex dump
     */
    default boolean retainedHexDumpDescription() {
        return false;
    }

    /**
     * Adds {@code comment} to the output, either as a full line (if starting with {@code '#'}) or
     * appended to the current line.
     */
    default B writeHexDumpDescription(CharSequence comment)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        return uncheckedCast(this);
    }

    /**
     * Adjusts the indentation level for subsequent dump lines.
     */
    default B adjustHexDumpIndentation(int n)
            throws IllegalStateException {
        return uncheckedCast(this);
    }
}
