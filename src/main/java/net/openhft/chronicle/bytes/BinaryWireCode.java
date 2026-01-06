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

    /** Code marking an anchor used for cyclic references. */
    int ANCHOR = 0x88;

    /** Code indicating a field alias update in the stream. */
    int UPDATED_ALIAS = 0x89;

    /** Code for an array of unsigned bytes. */
    int U8_ARRAY = 0x8A;
    /** Code for an array of signed 64-bit integers. */
    int I64_ARRAY = 0x8D;

    /** Code marking 32-bit padding inserted into the stream. */
    int PADDING32 = 0x8E;

    /** Code marking generic padding inserted into the stream. */
    int PADDING = 0x8F;

    /** Code indicating a 32-bit float value follows. */
    int FLOAT32 = 0x90;

    /** Code indicating a 64-bit float value follows. */
    int FLOAT64 = 0x91;
    /** Float encoded with 2 decimal places using stop bits. */
    int FLOAT_STOP_2 = 0x92;

    /** Float encoded with 4 decimal places. */
    int FLOAT_STOP_4 = 0x94;

    /** Float encoded with 6 decimal places. */
    int FLOAT_STOP_6 = 0x96;

    /** Code for a float value scaled by 10^0. */
    int FLOAT_SET_LOW_0 = 0x9A;

    /** Float value scaled by 10^2. */
    int FLOAT_SET_LOW_2 = 0x9B;

    /** Float value scaled by 10^4. */
    int FLOAT_SET_LOW_4 = 0x9C;
    // 0x98 - 0x9F

    /** Code indicating a UUID value follows in the stream. */
    int UUID = 0xA0;

    /** Code indicating an unsigned 8-bit integer follows. */
    int UINT8 = 0xA1;

    /** Code indicating an unsigned 16-bit integer follows. */
    int UINT16 = 0xA2;

    /** Code indicating an unsigned 32-bit integer follows. */
    int UINT32 = 0xA3;

    /** Code indicating a signed 8-bit integer follows. */
    int INT8 = 0xA4;

    /** Code indicating a signed 16-bit integer follows. */
    int INT16 = 0xA5;

    /** Code indicating a signed 32-bit integer follows. */
    int INT32 = 0xA6;

    /** Code indicating a signed 64-bit integer follows. */
    int INT64 = 0xA7;

    /** Set low 8-bit integer value. */
    int SET_LOW_INT8 = 0xA8;

    /** Set low 16-bit integer value. */
    int SET_LOW_INT16 = 0xA9;

    /** Code indicating a stop-bit encoded integer follows. */
    int STOP_BIT = 0xAE;

    /** 64-bit integer formatted as hexadecimal. */
    int INT64_0x = 0xAF;

    /** Code indicating a boolean false value follows. */
    int FALSE = 0xB0;

    /** Code indicating a boolean true value follows. */
    int TRUE = 0xB1;

    /** Code indicating a millisecond time-of-day value follows. */
    int TIME = 0xB2;

    /** Code indicating a date value as days since epoch. */
    int DATE = 0xB3;

    /** Code indicating a local date-time value without zone. */
    int DATE_TIME = 0xB4;

    /** Code indicating a zoned date-time value follows. */
    int ZONED_DATE_TIME = 0xB5;

    /** Code marking a type prefix in the binary stream. */
    int TYPE_PREFIX = 0xB6;

    /** Code indicating a field name encoded as text follows. */
    int FIELD_NAME_ANY = 0xB7;

    /** Code indicating an arbitrary string value follows. */
    int STRING_ANY = 0xB8;

    /** Code indicating an event name string follows. */
    int EVENT_NAME = 0xB9;

    /** Field number encoded as stop bit. */
    int FIELD_NUMBER = 0xBA;

    /** Code marking a null literal value in the stream. */
    int NULL = 0xBB;

    /** Code indicating a type literal string follows. */
    int TYPE_LITERAL = 0xBC;

    /** Code indicating an event object encoded in binary follows. */
    int EVENT_OBJECT = 0xBD;

    /** Code indicating comment text follows in the stream. */
    int COMMENT = 0xBE;

    /** Code providing a hint for optimisation in parsing. */
    int HINT = 0xBF;

    /** Code indicating a field name with zero length. */
    int FIELD_NAME0 = 0xC0;
    // ...

    /** Field name exactly 31 bytes long. */
    int FIELD_NAME31 = 0xDF;

    /** Code indicating a string with zero length. */
    int STRING_0 = 0xE0;
    // ...
    /** Code indicating a string of exactly 31 bytes. */
    int STRING_31 = 0xFF;

    /** Lookup table mapping codes to their textual name, useful for debugging. */
    String[] STRING_FOR_CODE = stringForCode(BinaryWireCode.class);

    /**
     * Builds {@link #STRING_FOR_CODE} by reflecting over constant fields.
     *
     * @param clazz type containing the wire code constants
     * @return array mapping code values (0-255) to human readable names
     */
    static String[] stringForCode(Class<?> clazz) {
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
            throw new AssertionError("Failed to resolve binary wire codes", e);
        }
        return stringForCode;
    }
}
