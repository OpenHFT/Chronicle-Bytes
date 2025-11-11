/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Defines byte codes used by Chronicle's binary wire protocol. Each constant
 * indicates the type or structure of the data that follows in the stream.
 */
public interface BinaryWireCode {

    /** Code for a byte sequence with a length in the following one byte. */
    int BYTES_LENGTH8 = 0x80;

    /** Code for a byte sequence with a 2 byte length prefix. */
    int BYTES_LENGTH16 = 0x81;

    /** Code for a byte sequence with a 4 byte length prefix. */
    int BYTES_LENGTH32 = 0x82;

    /** Code referencing a previously written field. */
    int FIELD_ANCHOR = 0x87;

    /** Anchor for cyclic references. */
    int ANCHOR = 0x88;

    /** Indicates an alias update. */
    int UPDATED_ALIAS = 0x89;

    /** Code for an array of unsigned bytes. */
    int U8_ARRAY = 0x8A;
    /** Code for an array of signed 64-bit integers. */
    int I64_ARRAY = 0x8D;

    /** 32-bit padding marker. */
    int PADDING32 = 0x8E;

    /** Generic padding marker. */
    int PADDING = 0x8F;

    /** 32-bit float value. */
    int FLOAT32 = 0x90;

    /** 64-bit float value. */
    int FLOAT64 = 0x91;
    /** Float encoded with 2 decimal places using stop bits. */
    int FLOAT_STOP_2 = 0x92;

    /** Float encoded with 4 decimal places. */
    int FLOAT_STOP_4 = 0x94;

    /** Float encoded with 6 decimal places. */
    int FLOAT_STOP_6 = 0x96;

    /** Float value scaled by 1. */
    int FLOAT_SET_LOW_0 = 0x9A;

    /** Float value scaled by 10^2. */
    int FLOAT_SET_LOW_2 = 0x9B;

    /** Float value scaled by 10^4. */
    int FLOAT_SET_LOW_4 = 0x9C;
    // 0x98 - 0x9F

    /** Universally unique identifier. */
    int UUID = 0xA0;

    /** Unsigned 8-bit integer value. */
    int UINT8 = 0xA1;

    /** Unsigned 16-bit integer value. */
    int UINT16 = 0xA2;

    /** Unsigned 32-bit integer value. */
    int UINT32 = 0xA3;

    /** Signed 8-bit integer value. */
    int INT8 = 0xA4;

    /** Signed 16-bit integer value. */
    int INT16 = 0xA5;

    /** Signed 32-bit integer value. */
    int INT32 = 0xA6;

    /** Signed 64-bit integer value. */
    int INT64 = 0xA7;

    /** Set low 8-bit integer value. */
    int SET_LOW_INT8 = 0xA8;

    /** Set low 16-bit integer value. */
    int SET_LOW_INT16 = 0xA9;

    /** Stop bit encoded integer. */
    int STOP_BIT = 0xAE;

    /** 64-bit integer formatted as hexadecimal. */
    int INT64_0x = 0xAF;

    /** Boolean false value. */
    int FALSE = 0xB0;

    /** Boolean true value. */
    int TRUE = 0xB1;

    /** Millisecond time of day. */
    int TIME = 0xB2;

    /** Date (days since epoch). */
    int DATE = 0xB3;

    /** Date and time without zone. */
    int DATE_TIME = 0xB4;

    /** Zoned date and time. */
    int ZONED_DATE_TIME = 0xB5;

    /** Type prefix marker. */
    int TYPE_PREFIX = 0xB6;

    /** Field name encoded as text. */
    int FIELD_NAME_ANY = 0xB7;

    /** Arbitrary string value. */
    int STRING_ANY = 0xB8;

    /** Event name string. */
    int EVENT_NAME = 0xB9;

    /** Field number encoded as stop bit. */
    int FIELD_NUMBER = 0xBA;

    /** Null marker. */
    int NULL = 0xBB;

    /** Type literal string. */
    int TYPE_LITERAL = 0xBC;

    /** Event object encoded in binary. */
    int EVENT_OBJECT = 0xBD;

    /** Comment text. */
    int COMMENT = 0xBE;

    /** Hint for optimisation. */
    int HINT = 0xBF;

    /** Field name with zero length. */
    int FIELD_NAME0 = 0xC0;
    // ...

    /** Field name exactly 31 bytes long. */
    int FIELD_NAME31 = 0xDF;

    /** String of length zero. */
    int STRING_0 = 0xE0;
    // ...
    /** String exactly 31 bytes long. */
    int STRING_31 = 0xFF;

    /** Lookup table mapping codes to their textual name, useful for debugging. */
    String[] STRING_FOR_CODE = _stringForCode(BinaryWireCode.class);

    /**
     * Builds {@link #STRING_FOR_CODE} by reflecting over constant fields.
     */
    static String[] _stringForCode(Class<?> clazz) {
        String[] stringForCode = new String[256];
        try {
            for (@NotNull Field field : clazz.getDeclaredFields()) {
                if (field.getType() == int.class)
                    stringForCode[field.getInt(null)] = field.getName();
                else if (field.getType() == byte.class)
                    stringForCode[field.getByte(null) & 0xFF] = field.getName();
            }
            for (int i = FIELD_NAME0; i <= FIELD_NAME31; i++)
                stringForCode[i] = "FIELD_" + i;
            for (int i = STRING_0; i <= STRING_31; i++)
                stringForCode[i] = "STRING_" + i;
            for (int i = 0; i < stringForCode.length; i++) {
                if (stringForCode[i] == null) {
                    if (i <= ' ' || i >= 127) {
                        stringForCode[i] = "Unknown_0x" + Integer.toHexString(i).toUpperCase();
                    } else {
                        stringForCode[i] = "Unknown_" + (char) i;
                    }
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
        return stringForCode;
    }
}
