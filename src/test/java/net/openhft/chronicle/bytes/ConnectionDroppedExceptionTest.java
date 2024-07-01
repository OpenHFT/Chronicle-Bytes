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

import org.junit.Assert;
import org.junit.Test;

public class ConnectionDroppedExceptionTest {

    @Test
    public void testMessageConstructor() {
        String expectedMessage = "Connection dropped unexpectedly.";
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage);

        Assert.assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void testCauseConstructor() {
        Throwable expectedCause = new RuntimeException("Underlying cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedCause);

        Assert.assertEquals(expectedCause, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String expectedMessage = "Connection dropped with details.";
        Throwable expectedCause = new RuntimeException("Specific cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage, expectedCause);

        Assert.assertEquals(expectedMessage, exception.getMessage());
        Assert.assertEquals(expectedCause, exception.getCause());
    }
}
