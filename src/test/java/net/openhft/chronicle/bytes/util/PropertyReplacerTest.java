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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class PropertyReplacerTest extends BytesTestCommon {

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

    // ========== Additional tests for full branch coverage ==========

    @Test
    @DisplayName("expression without tokens returns unchanged text output")
    void shouldReturnUnchangedWhenNoTokensPresent() {
        String expression = "plain text without any tokens";
        String result = PropertyReplacer.replaceTokensWithProperties(expression, new Properties());
        assertEquals(expression, result,
                "Expression without tokens should be returned unchanged");
    }

    @Test
    @DisplayName("system property replacement succeeds when property exists")
    void shouldReplaceSystemPropertySuccessfully() {
        // Use a known system property that always exists
        String javaVersion = System.getProperty("java.version");
        assertNotNull(javaVersion, "java.version system property should be available");

        String result = PropertyReplacer.replaceTokensWithProperties("Java: ${java.version}");
        assertEquals("Java: " + javaVersion, result,
                "System property should be replaced with its value");
    }

    @Test
    @DisplayName("multiple tokens are all replaced in a single pass")
    void shouldReplaceMultipleTokensSuccessfully() {
        Properties props = new Properties();
        props.setProperty("first", "ONE");
        props.setProperty("second", "TWO");
        props.setProperty("third", "THREE");

        String result = PropertyReplacer.replaceTokensWithProperties(
                "${first}-${second}-${third}", props);
        assertEquals("ONE-TWO-THREE", result,
                "All tokens should be replaced with their values");
    }

    @Test
    @DisplayName("token at start of expression is replaced")
    void shouldReplaceTokenAtStartOfExpression() {
        Properties props = new Properties();
        props.setProperty("greeting", "Hello");

        String result = PropertyReplacer.replaceTokensWithProperties("${greeting} World", props);
        assertEquals("Hello World", result,
                "Token at start should be replaced");
    }

    @Test
    @DisplayName("token at end of expression is replaced")
    void shouldReplaceTokenAtEndOfExpression() {
        Properties props = new Properties();
        props.setProperty("suffix", "World");

        String result = PropertyReplacer.replaceTokensWithProperties("Hello ${suffix}", props);
        assertEquals("Hello World", result,
                "Token at end should be replaced");
    }

    @Test
    @DisplayName("adjacent tokens are both replaced without separators")
    void shouldReplaceAdjacentTokens() {
        Properties props = new Properties();
        props.setProperty("a", "Hello");
        props.setProperty("b", "World");

        String result = PropertyReplacer.replaceTokensWithProperties("${a}${b}", props);
        assertEquals("HelloWorld", result,
                "Adjacent tokens should both be replaced");
    }

    @Test
    @DisplayName("empty token expression returns empty replacement string output")
    void shouldHandleEmptyExpression() {
        String result = PropertyReplacer.replaceTokensWithProperties("", new Properties());
        assertEquals("", result,
                "token replacement returns empty string for empty expression");
    }

    @Test
    @DisplayName("expression with only a token returns the property value text")
    void shouldHandleExpressionWithOnlyToken() {
        Properties props = new Properties();
        props.setProperty("onlyToken", "value");

        String result = PropertyReplacer.replaceTokensWithProperties("${onlyToken}", props);
        assertEquals("value", result,
                "Expression with only a token should return just the value");
    }

    @Test
    @DisplayName("system property replacement with multiple tokens")
    void shouldReplaceMultipleSystemPropertiesSuccessfully() {
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        assertNotNull(javaVersion, "java.version system property should be available for replacement");
        assertNotNull(javaVendor, "java.vendor should exist");

        String result = PropertyReplacer.replaceTokensWithProperties(
                "Java ${java.version} by ${java.vendor}");
        assertEquals("Java " + javaVersion + " by " + javaVendor, result,
                "Multiple system properties should be replaced");
    }

    @Test
    @DisplayName("expression without tokens using system properties returns unchanged")
    void shouldReturnUnchangedWhenNoTokensPresentSystemProperties() {
        String expression = "no tokens here";
        String result = PropertyReplacer.replaceTokensWithProperties(expression);
        assertEquals(expression, result,
                "Expression without tokens should be returned unchanged for system properties");
    }
}
