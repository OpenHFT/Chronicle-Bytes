package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public interface BinaryWireCode {

    // sequence of length 0 - 255 bytes
    public static final int BYTES_LENGTH8 = 0x80;
    //     sequence of length 0 - 2^16-1 bytes
    public static final int BYTES_LENGTH16 = 0x81;
    // sequence of length 0 - 2^32-1
    public static final int BYTES_LENGTH32 = 0x82;
    // sequence of length 0 - 255
//        public static final int BYTES_LENGTH64 = 0x83;

    public static final int FIELD_ANCHOR = 0x87;
    public static final int ANCHOR = 0x88;
    public static final int UPDATED_ALIAS = 0x89;

    // an array of unsigned bytes
    public static final int U8_ARRAY = 0x8A;
    //        public static final int U16_ARRAY = 0x8B;
//        public static final int I32_ARRAY = 0x8C;
    public static final int I64_ARRAY = 0x8D;
    public static final int PADDING32 = 0x8E;
    public static final int PADDING = 0x8F;

    public static final int FLOAT32 = 0x90;
    public static final int FLOAT64 = 0x91;
    public static final int FLOAT_STOP_2 = 0x92;
    public static final int FLOAT_STOP_4 = 0x94;
    public static final int FLOAT_STOP_6 = 0x96;
    public static final int FLOAT_SET_LOW_0 = 0x9A;
    public static final int FLOAT_SET_LOW_2 = 0x9B;
    public static final int FLOAT_SET_LOW_4 = 0x9C;
    // 0x98 - 0x9F

    public static final int UUID = 0xA0;
    public static final int UINT8 = 0xA1;
    public static final int UINT16 = 0xA2;
    public static final int UINT32 = 0xA3;
    public static final int INT8 = 0xA4;
    public static final int INT16 = 0xA5;
    public static final int INT32 = 0xA6;
    public static final int INT64 = 0xA7;
    public static final int SET_LOW_INT8 = 0xA8;
    public static final int SET_LOW_INT16 = 0xA9;
    //    public static final int FIXED_5 = 0xAA;
//    public static final int FIXED_4 = 0xAB;
//    public static final int FIXED_3 = 0xAC;
//    public static final int FIXED_2 = 0xAD;
    public static final int STOP_BIT = 0xAE;
    public static final int INT64_0x = 0xAF;

    public static final int FALSE = 0xB0;
    public static final int TRUE = 0xB1;
    public static final int TIME = 0xB2;
    public static final int DATE = 0xB3;
    public static final int DATE_TIME = 0xB4;
    public static final int ZONED_DATE_TIME = 0xB5;
    public static final int TYPE_PREFIX = 0xB6;
    public static final int FIELD_NAME_ANY = 0xB7;
    public static final int STRING_ANY = 0xB8;
    public static final int EVENT_NAME = 0xB9;
    public static final int FIELD_NUMBER = 0xBA;
    public static final int NULL = 0xBB;
    public static final int TYPE_LITERAL = 0xBC;
    public static final int EVENT_OBJECT = 0xBD;
    public static final int COMMENT = 0xBE;
    public static final int HINT = 0xBF;

    public static final int FIELD_NAME0 = 0xC0;
    // ...
    public static final int FIELD_NAME31 = 0xDF;

    public static final int STRING_0 = 0xE0;
    // ...
    public static final int STRING_31 = 0xFF;

    public static final String[] STRING_FOR_CODE = _stringForCode(BinaryWireCode.class);

    static String[] _stringForCode(Class clazz) {
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
                    if (i <= ' ' || i >= 127)
                        stringForCode[i] = "Unknown_0x" + Integer.toHexString(i).toUpperCase();
                    else
                        stringForCode[i] = "Unknown_" + (char) i;
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        return stringForCode;
    }
}
