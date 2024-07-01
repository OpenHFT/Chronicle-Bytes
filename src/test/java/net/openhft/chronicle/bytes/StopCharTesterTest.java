/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
package net.openhft.chronicle.bytes;

import org.junit.Test;
import static org.junit.Assert.*;

public class StopCharTesterTest {

    @Test
    public void testIsStopChar() {
        StopCharTester tester = ch -> ch == ';' || ch == ',';

        assertTrue("Semicolon should be a stop char", tester.isStopChar(';'));
        assertTrue("Comma should be a stop char", tester.isStopChar(','));
        assertFalse("Letter should not be a stop char", tester.isStopChar('A'));
    }

    @Test
    public void testEscaping() {
        StopCharTester baseTester = ch -> ch == ';';
        StopCharTester escapingTester = baseTester.escaping();

        assertTrue("Semicolon should be a stop char without escaping", baseTester.isStopChar(';'));
        assertFalse("Escaped semicolon should not be a stop char", escapingTester.isStopChar('\\'));
    }
}
