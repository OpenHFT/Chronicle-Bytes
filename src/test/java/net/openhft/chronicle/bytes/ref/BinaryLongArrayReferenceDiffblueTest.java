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

import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

public class BinaryLongArrayReferenceDiffblueTest extends BytesTestCommon {
  /**
  * Method under test: {@link BinaryLongArrayReference#BinaryLongArrayReference()}
  */
  @Test
  public void testConstructor() throws IllegalStateException {
    // Arrange and Act
    BinaryLongArrayReference actualBinaryLongArrayReference = new BinaryLongArrayReference();

    // Assert
    assertEquals(0L, actualBinaryLongArrayReference.getCapacity());
    assertFalse(actualBinaryLongArrayReference.isClosing());
    actualBinaryLongArrayReference.close();
  }

  /**
   * Method under test: {@link BinaryLongArrayReference#BinaryLongArrayReference(long)}
   */
  @Test
  public void testConstructor2() throws IllegalStateException {
    // Arrange and Act
    BinaryLongArrayReference actualBinaryLongArrayReference = new BinaryLongArrayReference(1L);

    // Assert
    assertEquals(1L, actualBinaryLongArrayReference.getCapacity());
    assertFalse(actualBinaryLongArrayReference.isClosing());
  }
}

