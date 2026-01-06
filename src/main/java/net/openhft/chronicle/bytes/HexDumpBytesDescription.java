/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;

import static net.openhft.chronicle.core.Jvm.uncheckedCast;

/**
 * Describes optional behaviours for {@link Bytes} implementations that emit hex dumps, such
 * as retaining comments and adjusting indentation for readability.
 *
 * @param <B> self type for fluent chaining
 */
public interface HexDumpBytesDescription<B extends HexDumpBytesDescription<B>> {
    /**
     * Indicates whether the producer should retain human-readable comments in the hex dump.
     *
     * @return {@code true} if comments are retained for later inclusion in the hex dump
     */
    default boolean retainedHexDumpDescription() {
        return false;
    }

    /**
     * Adds {@code comment} to the output, either as a full line (if starting with {@code '#'}) or
     * appended to the current line.
     *
     * @param comment text to add to the dump
     * @return this for chaining
     */
    default B writeHexDumpDescription(CharSequence comment)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        return uncheckedCast(this);
    }

    /**
     * Adjusts the indentation level for subsequent dump lines.
     *
     * @param n indentation delta to apply
     * @return this for chaining
     */
    default B adjustHexDumpIndentation(int n)
            throws IllegalStateException {
        return uncheckedCast(this);
    }
}
