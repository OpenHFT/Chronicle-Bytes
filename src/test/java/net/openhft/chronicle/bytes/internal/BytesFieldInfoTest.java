package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.FieldGroup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesFieldInfoTest {

    @Test
    public void lookup() {
        final BytesFieldInfo lookup = BytesFieldInfo.lookup(Groups1.class);
        assertEquals("type: BytesFieldInfo, groups: { pad: 16 to 48, other: 64 to 96, hi: 96 to 100, add: 104 to 120 }", lookup.dump());
        assertEquals("a068000", Integer.toHexString(lookup.description()));
        final BytesFieldInfo lookup2 = BytesFieldInfo.lookup(Groups2.class);
        assertEquals("type: BytesFieldInfo, groups: { hi: 12 to 16, pad: 16 to 48, add: 48 to 64 }", lookup2.dump());
        assertEquals("4050000", Integer.toHexString(lookup2.description()));
        final BytesFieldInfo lookup3 = BytesFieldInfo.lookup(Groups3.class);
        assertEquals("type: BytesFieldInfo, groups: { pad: 16 to 48, hi: 48 to 52, add: 52 to 68 }", lookup3.dump());
        assertEquals("4050000", Integer.toHexString(lookup3.description()));
    }

    static class Groups1 {
        @FieldGroup("header")
        int header;
        @FieldGroup("pad")
        long pad0, pad1, pad3, pad2;
        double d1, d2;
        @FieldGroup("other")
        long pad10, pad11, pad13, pad12;
        @FieldGroup("hi")
        int hi99;
        float f;
        @FieldGroup("add")
        int add1, add2, add3, add4;
    }

    static class Groups2 {
        @FieldGroup("pad")
        long pad0, pad1, pad3, pad2;
        @FieldGroup("hi")
        int hi99;
        @FieldGroup("add")
        int add1, add2, add3, add4;
    }

    static class GroupsBase {
        @FieldGroup("pad")
        long pad0, pad1, pad3, pad2;
    }

    static class Groups3 extends GroupsBase {
        @FieldGroup("hi")
        int hi99;
        @FieldGroup("add")
        int add1, add2, add3, add4;
    }
}