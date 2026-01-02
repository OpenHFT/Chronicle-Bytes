/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class PropertyReplacerTest extends BytesTestCommon {

    @BeforeEach
    void skipOnWindowsAndWsl() {
        // Skip on Windows/WSL due to JVM native crash (STATUS_HEAP_CORRUPTION) with Java 8
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL due to JVM crash");
    }
    @Test
    @DisplayName("missing system property fails with a detailed message")
    public void testSystemPropertyMissing() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> PropertyReplacer.replaceTokensWithProperties("plainText ${missingPropertyToReplace}"),
                "Missing system property should raise an exception");
        assertEquals("System property is missing: [property=missingPropertyToReplace, " +
                        "expression=plainText ${missingPropertyToReplace}]",
                exception.getMessage(),
                "Exception message should include the missing property and expression");
    }

    @Test
    @DisplayName("missing property fails with a detailed message")
    public void testPropertyMissing() {
        final Properties properties = new Properties();
        properties.setProperty("wrongProperty", "wrongValue");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> PropertyReplacer.replaceTokensWithProperties("plainText ${missingPropertyToReplace}", properties),
                "Missing property should raise an exception");
        assertEquals("Property is missing: [property=missingPropertyToReplace, " +
                        "expression=plainText ${missingPropertyToReplace}, properties={wrongProperty=wrongValue}]",
                exception.getMessage(),
                "Exception message should include the properties map and expression");
    }

    @Test
    @DisplayName("whitespace around property name is ignored in token")
    public void testLeadingAndTrailingSpacesInsideBracketsIgnored() {
        final Properties props = new Properties();
        props.setProperty("myFancyProperty", "myFancyValue");

        assertReplacement(props, "plainKey: ${ myFancyProperty }", "single space inside braces");
        assertReplacement(props, "plainKey: ${myFancyProperty}", "no whitespace inside braces");
        assertReplacement(props, "plainKey: ${  myFancyProperty  }", "double space inside braces");
        assertReplacement(props, "plainKey: ${    myFancyProperty }", "leading spaces inside braces");
        assertReplacement(props, "plainKey: ${\tmyFancyProperty\t}", "tab characters inside braces");
        assertReplacement(props, "plainKey: ${ \t\t\nmyFancyProperty \r\f}", "mixed whitespace inside braces");
    }

    private static void assertReplacement(Properties props, String input, String scenario) {
        String res = PropertyReplacer.replaceTokensWithProperties(input, props);
        assertEquals("plainKey: myFancyValue",
                res,
                "Property replacement should trim whitespace for " + scenario);
    }
}
