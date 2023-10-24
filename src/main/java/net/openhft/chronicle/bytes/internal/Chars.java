package net.openhft.chronicle.bytes.internal;

import org.jetbrains.annotations.NotNull;

public final class Chars {
    private Chars() { }
    public static final String[] charToString = createCharToString();

    /**
     * Creates a lookup table mapping byte values to their corresponding String representations.
     *
     * @return a lookup table for byte-to-String conversions.
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
