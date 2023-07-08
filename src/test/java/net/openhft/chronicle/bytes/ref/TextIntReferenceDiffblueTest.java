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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.StackTrace;
import org.junit.Test;

public class TextIntReferenceDiffblueTest extends BytesTestCommon {
  /**
  * Method under test: default or parameterless constructor of {@link TextIntReference}
  */
  @Test
  public void testConstructor() {
    // Arrange and Act
    TextIntReference actualTextIntReference = new TextIntReference();

    // Assert
    assertEquals(0L, actualTextIntReference.offset);
    assertFalse(actualTextIntReference.isClosed());
    assertEquals(0L, actualTextIntReference.offset());
    assertNull(actualTextIntReference.bytes);
    assertFalse(actualTextIntReference.isClosing());
    StackTrace createdHereResult = actualTextIntReference.createdHere();
    assertEquals(0, createdHereResult.getSuppressed().length);
    assertNull(createdHereResult.getCause());
    actualTextIntReference.close();
  }

  /**
   * Method under test: default or parameterless constructor of {@link TextIntReference}
   */
  @Test
  public void testConstructor2() {
    // Arrange, Act and Assert
    TextIntReference reference = new TextIntReference();
    assertFalse(reference.isClosed());
    reference.close();
    assertFalse(reference.isClosed());
  }
}

