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

import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextLongArrayReferenceDiffblueTest extends BytesTestCommon {
  /**
  * Method under test: default or parameterless constructor of {@link TextLongArrayReference}
  */
  @Test
  public void testConstructor() {
    // Arrange and Act
    TextLongArrayReference actualTextLongArrayReference = new TextLongArrayReference();

    // Assert
    assertEquals(0L, actualTextLongArrayReference.getCapacity());
    assertFalse(actualTextLongArrayReference.isClosing());
    actualTextLongArrayReference.close();
    assertTrue(actualTextLongArrayReference.isClosing());
  }
}

