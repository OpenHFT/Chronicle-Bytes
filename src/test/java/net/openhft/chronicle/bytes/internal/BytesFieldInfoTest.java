package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.FieldGroup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesFieldInfoTest {

    @Test
    public void lookup() {
        final BytesFieldInfo lookup = BytesFieldInfo.lookup(Groups1.class);
        assertEquals("type: BytesFieldInfo, groups: { pad: 16 to 48, hi: 48 to 52, add: 52 to 68 }", lookup.dump());
    }

    static class Groups1 {
        @FieldGroup("header")
        int header;
        @FieldGroup("pad")
        long pad0, pad1, pad3, pad2;
        @FieldGroup("hi")
        int hi99;
        @FieldGroup("add")
        int add1, add2, add3, add4;
    }
}