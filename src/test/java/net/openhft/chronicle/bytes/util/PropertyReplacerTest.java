/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
