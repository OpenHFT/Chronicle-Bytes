/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import org.jetbrains.annotations.NotNull;

/**
 * Lookup utilities for rendering bytes as characters, used in hex dumps and
 * similar operations.
 */
public final class Chars {
    private Chars() { }
    /** lookup table for byte value to String */
    public static final String[] charToString = createCharToString();

    /**
     * Populates {@link #charToString} with printable representations for all
     * byte values. Control characters are encoded as escape sequences.
     */
    @NotNull
    public static String[] createCharToString() {
        @NotNull String[] charToString = new String[256];
        charToString[0] = "\u0660";
        for (int i = 1; i < 21; i++)
            charToString[i] = Character.toString((char) (i + 0x2487));
        for (int i = ' '; i < 256; i++)
            charToString[i] = Character.toString((char) i);
        for (int i = 21; i < ' '; i++)
            charToString[i] = "\\u00" + Integer.toHexString(i).toUpperCase();
        for (int i = 0x80; i < 0xA0; i++)
            charToString[i] = "\\u00" + Integer.toHexString(i).toUpperCase();
        return charToString;
    }
}
