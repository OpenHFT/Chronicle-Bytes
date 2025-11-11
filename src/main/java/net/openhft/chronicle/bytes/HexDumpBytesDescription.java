/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
