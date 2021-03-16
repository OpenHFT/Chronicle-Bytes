package net.openhft.chronicle.bytes.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesFieldInfoTest {

    @Test
    public void lookup() {
        final BytesFieldInfo lookup = BytesFieldInfo.lookup(Groups1.class);
        assertEquals("type: BytesFieldInfo, groups: { hi: 12 to 16, pad: 16 to 48, add: 48 to 64 }", lookup.dump());
    }

    static class Groups1 {
        long pad0, pad1, pad3, pad2;
        int hi99;
        int add1, add2, add3, add4;
    }
}