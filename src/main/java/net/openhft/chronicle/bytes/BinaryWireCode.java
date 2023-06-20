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
package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
/**
 * Interface defining constants used in BinaryWire. These constants are used to represent
 * data types or values in a serialized format.
 */
public interface BinaryWireCode {

    /**
     * Represents a sequence of length 0 - 255 bytes.
     */
    int BYTES_LENGTH8 = 0x80;

    /**
     * Represents a sequence of length 0 - 2^16-1 bytes.
     */
    int BYTES_LENGTH16 = 0x81;

    /**
     * Represents a sequence of length 0 - 2^32-1.
     */
    int BYTES_LENGTH32 = 0x82;

    /**
     * Denotes a field anchor.
     */
    int FIELD_ANCHOR = 0x87;

    /**
     * Represents an anchor.
     */
    int ANCHOR = 0x88;

    /**
     * Represents an updated alias.
     */
    int UPDATED_ALIAS = 0x89;

    /**
     * Represents an array of unsigned bytes.
     */
    int U8_ARRAY = 0x8A;
    /**
     * Represents an array of signed long.
     */
    int I64_ARRAY = 0x8D;

    /**
     * Represents 32-bit padding.
     */
    int PADDING32 = 0x8E;

    /**
     * Represents padding.
     */
    int PADDING = 0x8F;

    /**
     * Represents a 32-bit floating-point number.
     */
    int FLOAT32 = 0x90;

    /**
     * Represents a 64-bit floating-point number.
     */
    int FLOAT64 = 0x91;
    /**
     * Represents floating-point stop bit encoded * 10^2.
     */
    int FLOAT_STOP_2 = 0x92;

    /**
     * Represents floating-point stop 4.
     */
    int FLOAT_STOP_4 = 0x94;

    /**
     * Represents floating-point stop 6.
     */
    int FLOAT_STOP_6 = 0x96;

    /**
     * Represents floating-point set low 0.
     */
    int FLOAT_SET_LOW_0 = 0x9A;

    /**
     * Represents floating-point set low 2.
     */
    int FLOAT_SET_LOW_2 = 0x9B;

    /**
     * Represents floating-point set low 4.
     */
    int FLOAT_SET_LOW_4 = 0x9C;
    // 0x98 - 0x9F

    /**
     * Represents a UUID.
     */
    int UUID = 0xA0;

    /**
     * Represents an 8-bit unsigned integer.
     */
    int UINT8 = 0xA1;

    /**
     * Represents a 16-bit unsigned integer.
     */
    int UINT16 = 0xA2;

    /**
     * Represents a 32-bit unsigned integer.
     */
    int UINT32 = 0xA3;

    /**
     * Represents an 8-bit integer.
     */
    int INT8 = 0xA4;

    /**
     * Represents a 16-bit integer.
     */
    int INT16 = 0xA5;

    /**
     * Represents a 32-bit integer.
     */
    int INT32 = 0xA6;

    /**
     * Represents a 64-bit integer.
     */
    int INT64 = 0xA7;

    /**
     * Represents a set low 8-bit integer.
     */
    int SET_LOW_INT8 = 0xA8;

    /**
     * Represents a set low 16-bit integer.
     */
    int SET_LOW_INT16 = 0xA9;

    /**
     * Represents a stop bit.
     */
    int STOP_BIT = 0xAE;

    /**
     * Represents a 64-bit integer with a hexadecimal prefix.
     */
    int INT64_0x = 0xAF;

    /**
     * Represents a boolean false value.
     */
    int FALSE = 0xB0;

    /**
     * Represents a boolean true value.
     */
    int TRUE = 0xB1;

    /**
     * Represents a time.
     */
    int TIME = 0xB2;

    /**
     * Represents a date.
     */
    int DATE = 0xB3;

    /**
     * Represents a date and time.
     */
    int DATE_TIME = 0xB4;

    /**
     * Represents a zoned date and time.
     */
    int ZONED_DATE_TIME = 0xB5;

    /**
     * Represents a type prefix.
     */
    int TYPE_PREFIX = 0xB6;

    /**
     * Represents a field name that can be any string.
     */
    int FIELD_NAME_ANY = 0xB7;

    /**
     * Represents a string that can be any string.
     */
    int STRING_ANY = 0xB8;

    /**
     * Represents an event name.
     */
    int EVENT_NAME = 0xB9;

    /**
     * Represents a field number.
     */
    int FIELD_NUMBER = 0xBA;

    /**
     * Represents a null value.
     */
    int NULL = 0xBB;

    /**
     * Represents a type literal.
     */
    int TYPE_LITERAL = 0xBC;

    /**
     * Represents an event object.
     */
    int EVENT_OBJECT = 0xBD;

    /**
     * Represents a comment.
     */
    int COMMENT = 0xBE;

    /**
     * Represents a hint.
     */
    int HINT = 0xBF;

    /**
     * Represents the field name of length 0.
     */
    int FIELD_NAME0 = 0xC0;
    // ...

    /**
     * Represents the field name of length 31 bytes.
     */
    int FIELD_NAME31 = 0xDF;

    /**
     * Represents the string of length 0.
     */
    int STRING_0 = 0xE0;
    // ...
    /**
     * Represents the string of length 31 bytes.
     */
    int STRING_31 = 0xFF;

    /**
     * Maps each code in the interface to its corresponding string representation.
     */
    String[] STRING_FOR_CODE = _stringForCode(BinaryWireCode.class);

    /**
     * Helper method for generating a text representation all the codes
     * @param clazz to search for constants to extract
     * @return an array of Strings for each code
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
