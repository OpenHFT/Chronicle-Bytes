/*
 * Copyright 2016-2020 Chronicle Software
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
package net.openhft.chronicle.bytes.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class PropertyReplacerTest {
    @Test
    public void testSystemPropertyMissing() {
        try {
            PropertyReplacer.replaceTokensWithProperties("plainText ${missingPropertyToReplace}");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Property is missing: [property=missingPropertyToReplace, expression=plainText ${missingPropertyToReplace}, properties="));
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

    @Test
    public void groupsIncomplete() {
        final List<PropertyReplacer.Group> groups = PropertyReplacer.groups("${a");
        assertEquals(emptyList(), groups);
    }

    @Test
    public void groups() {
        final List<PropertyReplacer.Group> groups = PropertyReplacer.groups("${a}");
        assertEquals(singletonList(new PropertyReplacer.Group(0, 3, 2, 2, "a")), groups);
    }

    @Test
    public void groupsWithSpacesAtTheEnd() {
        final List<PropertyReplacer.Group> groups = PropertyReplacer.groups("${a}   ");
        assertEquals(singletonList(new PropertyReplacer.Group(0, 3, 2, 2, "a")), groups);
    }

    @Test
    public void groups2() {
        final String s = "plainText ${missingPropertyToReplace}";
        final List<PropertyReplacer.Group> groups = PropertyReplacer.groups(s);
        assertEquals(singletonList(new PropertyReplacer.Group(
                        s.indexOf('$'),
                        s.lastIndexOf('}'),
                        s.indexOf('{') + 1,
                        s.lastIndexOf('}') - 1,
                        "missingPropertyToReplace"
                )
        ), groups);
    }

    @Test
    public void groups3() {
        final String s = "a ${ b }";
        final List<PropertyReplacer.Group> groups = PropertyReplacer.groups(s);
        assertEquals(singletonList(new PropertyReplacer.Group(
                s.indexOf('$'),
                s.lastIndexOf('}'),
                5,
                5,
                "b")
        ), groups);
    }

    @Test
    public void groupsSeveral() {
        final String s = "a ${b} ${c}";
        final List<PropertyReplacer.Group> groups = PropertyReplacer.groups(s);
        assertEquals(Arrays.asList(
                new PropertyReplacer.Group(s.indexOf('$'), s.indexOf('}'), s.indexOf('b'), s.indexOf('b'), "b"),
                new PropertyReplacer.Group(s.lastIndexOf('$'), s.lastIndexOf('}'), s.indexOf('c'), s.indexOf('c'), "c")
        ), groups);
    }

    @Test
    public void groupsNo() {
        final String s = "plainText ${     missingPropertyToReplace";
        final List<PropertyReplacer.Group> groups = PropertyReplacer.groups(s);
        assertEquals(emptyList(), groups);
    }

}
