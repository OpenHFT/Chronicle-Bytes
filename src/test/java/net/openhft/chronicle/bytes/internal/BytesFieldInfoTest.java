/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("BytesFieldInfo layout and description checks for group offsets")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class BytesFieldInfoTest extends BytesTestCommon {

    @Test
    @DisplayName("lookup returns expected group layouts for fields")
    public void lookup() {
        assumeFalse(Jvm.isArm() || Jvm.isAzulZing(),
                "Field layout is not stable on this JVM");
        final BytesFieldInfo lookup = BytesFieldInfo.lookup(Groups1.class);
        assertEquals("type: BytesFieldInfo, groups: { header: 12 to 16, pad: 16 to 48, other: 64 to 96, hi: 96 to 100, add: 104 to 120 }",
                lookup.dump(),
                "Groups1 layout matches expected dump");
        assertEquals("a070000", Integer.toHexString(lookup.description()),
                "Groups1 layout description matches expected hex");
        final BytesFieldInfo lookup2 = BytesFieldInfo.lookup(Groups2.class);
        assertEquals("type: BytesFieldInfo, groups: { hi: 12 to 16, pad: 16 to 48, add: 48 to 64 }",
                lookup2.dump(),
                "Groups2 layout matches expected dump");
        assertEquals("4050000", Integer.toHexString(lookup2.description()),
                "Groups2 layout description matches expected hex");
        final BytesFieldInfo lookup3 = BytesFieldInfo.lookup(Groups3.class);
        // field layout changed with Java 15 - https://bugs.openjdk.java.net/browse/JDK-8237767
        final String groups3 = Jvm.isJava15Plus() ?
                "type: BytesFieldInfo, groups: { hi: 12 to 16, pad: 16 to 48, add: 48 to 64 }" :
                "type: BytesFieldInfo, groups: { pad: 16 to 48, hi: 48 to 52, add: 52 to 68 }";
        assertEquals(groups3, lookup3.dump(),
                "Groups3 layout matches JVM specific dump");
        assertEquals("4050000", Integer.toHexString(lookup3.description()),
                "Groups3 layout description matches expected hex");
    }

    private static class Groups1 {
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

    private static class Groups2 {
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

    private static class Groups3 extends GroupsBase {
        @FieldGroup("hi")
        int hi99;
        @FieldGroup("add")
        int add1, add2, add3, add4;
    }
}
