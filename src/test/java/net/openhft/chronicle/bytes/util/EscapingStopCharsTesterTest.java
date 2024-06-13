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
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.StopCharsTester;
import org.junit.Test;
import org.junit.Before;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class EscapingStopCharsTesterTest {

    private StopCharsTester baseTester;
    private EscapingStopCharsTester tester;
    @Before
    public void setUp() {
        // Setup the base tester with specific behavior for demonstration
        baseTester = (ch, peekNextCh) -> ch == 'x'; // Let's say 'x' is a stop character
        tester = new EscapingStopCharsTester(baseTester);
    }

    @Test
    public void testIsStopCharWithEscape() {
        // First call with escape character
        assertFalse("Escaped character should not be stop char", tester.isStopChar('\\', 'x'));
        // Next call with the character that would normally be a stop character
        assertFalse("Character following an escape should not be treated as stop char", tester.isStopChar('x', ' '));
        // Subsequent call with a stop character not preceded by an escape
        assertTrue("Non-escaped stop char should be recognized as stop char", tester.isStopChar('x', ' '));
    }

    @Test
    public void testIsStopCharWithoutEscape() {
        assertFalse("Non-stop char should not be recognized as stop char", tester.isStopChar('y', ' '));
    }
}
