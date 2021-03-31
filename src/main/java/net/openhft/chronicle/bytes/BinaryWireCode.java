/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

public interface BinaryWireCode {

    // sequence of length 0 - 255 bytes
    int BYTES_LENGTH8 = 0x80;
    //     sequence of length 0 - 2^16-1 bytes
    int BYTES_LENGTH16 = 0x81;
    // sequence of length 0 - 2^32-1
    int BYTES_LENGTH32 = 0x82;
    // sequence of length 0 - 255
//        public static final int BYTES_LENGTH64 = 0x83;

    int FIELD_ANCHOR = 0x87;
    int ANCHOR = 0x88;
    int UPDATED_ALIAS = 0x89;

    // an array of unsigned bytes
    int U8_ARRAY = 0x8A;
    //        public static final int U16_ARRAY = 0x8B;
//        public static final int I32_ARRAY = 0x8C;
    int I64_ARRAY = 0x8D;
    int PADDING32 = 0x8E;
    int PADDING = 0x8F;

    int FLOAT32 = 0x90;
    int FLOAT64 = 0x91;
    int FLOAT_STOP_2 = 0x92;
    int FLOAT_STOP_4 = 0x94;
    int FLOAT_STOP_6 = 0x96;
    int FLOAT_SET_LOW_0 = 0x9A;
    int FLOAT_SET_LOW_2 = 0x9B;
    int FLOAT_SET_LOW_4 = 0x9C;
    // 0x98 - 0x9F

    int UUID = 0xA0;
    int UINT8 = 0xA1;
    int UINT16 = 0xA2;
    int UINT32 = 0xA3;
    int INT8 = 0xA4;
    int INT16 = 0xA5;
    int INT32 = 0xA6;
    int INT64 = 0xA7;
    int SET_LOW_INT8 = 0xA8;
    int SET_LOW_INT16 = 0xA9;
    //    public static final int FIXED_5 = 0xAA;
//    public static final int FIXED_4 = 0xAB;
//    public static final int FIXED_3 = 0xAC;
//    public static final int FIXED_2 = 0xAD;
    int STOP_BIT = 0xAE;
    int INT64_0x = 0xAF;

    int FALSE = 0xB0;
    int TRUE = 0xB1;
    int TIME = 0xB2;
    int DATE = 0xB3;
    int DATE_TIME = 0xB4;
    int ZONED_DATE_TIME = 0xB5;
    int TYPE_PREFIX = 0xB6;
    int FIELD_NAME_ANY = 0xB7;
    int STRING_ANY = 0xB8;
    int EVENT_NAME = 0xB9;
    int FIELD_NUMBER = 0xBA;
    int NULL = 0xBB;
    int TYPE_LITERAL = 0xBC;
    int EVENT_OBJECT = 0xBD;
    int COMMENT = 0xBE;
    int HINT = 0xBF;

    int FIELD_NAME0 = 0xC0;
    // ...
    int FIELD_NAME31 = 0xDF;

    int STRING_0 = 0xE0;
    // ...
    int STRING_31 = 0xFF;

    String[] STRING_FOR_CODE = _stringForCode(BinaryWireCode.class);

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
                if (stringForCode[i] == null)
                    if (i <= ' ' || i >= 127) {
                        stringForCode[i] = "Unknown_0x" + Integer.toHexString(i).toUpperCase();
                    } else {
                        stringForCode[i] = "Unknown_" + (char) i;
                    }
            }
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
        return stringForCode;
    }
}
