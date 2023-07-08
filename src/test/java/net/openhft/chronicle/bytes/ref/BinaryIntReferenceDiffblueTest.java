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
package net.openhft.chronicle.bytes.ref;

import java.nio.BufferUnderflowException;

import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinaryIntReferenceDiffblueTest extends BytesTestCommon {
  /**
  * Method under test: default or parameterless constructor of {@link BinaryIntReference}
  */
  @Test
  public void testConstructor() throws IllegalStateException, BufferUnderflowException {
    // Arrange and Act
    BinaryIntReference actualBinaryIntReference = new BinaryIntReference();

    // Assert
    assertEquals(0, actualBinaryIntReference.getValue());
    assertFalse(actualBinaryIntReference.isClosing());
    actualBinaryIntReference.close();
    assertTrue(actualBinaryIntReference.isClosed());
  }
}

