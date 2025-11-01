/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PropertyReplacerTest extends BytesTestCommon {
    @Test
    public void testSystemPropertyMissing() {
        try {
            PropertyReplacer.replaceTokensWithProperties("plainText ${missingPropertyToReplace}");
        } catch (IllegalArgumentException e) {
            assertEquals("System property is missing: [property=missingPropertyToReplace, " +
                    "expression=plainText ${missingPropertyToReplace}]", e.getMessage());

            return;
        }

        fail("Exception is expected");
    }

    @Test
    public void testPropertyMissing() {
        try {
            final Properties properties = new Properties();
            properties.setProperty("wrongProperty", "wrongValue");

            PropertyReplacer.replaceTokensWithProperties("plainText ${missingPropertyToReplace}", properties);
        } catch (IllegalArgumentException e) {
            assertEquals("Property is missing: [property=missingPropertyToReplace, " +
                            "expression=plainText ${missingPropertyToReplace}, properties={wrongProperty=wrongValue}]",
                    e.getMessage());

            return;
        }

        fail("Exception is expected");
    }

    @Test
    public void testLeadingAndTrailingSpacesInsideBracketsIgnored() {
        final Properties props = new Properties();
        props.setProperty("myFancyProperty", "myFancyValue");

        String res = PropertyReplacer.replaceTokensWithProperties("plainKey: ${ myFancyProperty }", props);
        assertEquals("plainKey: myFancyValue", res);

        res = PropertyReplacer.replaceTokensWithProperties("plainKey: ${myFancyProperty}", props);
        assertEquals("plainKey: myFancyValue", res);

        res = PropertyReplacer.replaceTokensWithProperties("plainKey: ${  myFancyProperty  }", props);
        assertEquals("plainKey: myFancyValue", res);

        res = PropertyReplacer.replaceTokensWithProperties("plainKey: ${    myFancyProperty }", props);
        assertEquals("plainKey: myFancyValue", res);

        res = PropertyReplacer.replaceTokensWithProperties("plainKey: ${\tmyFancyProperty\t}", props);
        assertEquals("plainKey: myFancyValue", res);

        res = PropertyReplacer.replaceTokensWithProperties("plainKey: ${ \t\t\nmyFancyProperty \r\f}", props);
        assertEquals("plainKey: myFancyValue", res);
    }
}
