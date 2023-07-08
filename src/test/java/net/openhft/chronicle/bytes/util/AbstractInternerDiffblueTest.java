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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

public class AbstractInternerDiffblueTest extends BytesTestCommon {
  /**
  * Method under test: {@link AbstractInterner#toggle()}
  */
  @Test
  public void testToggle() throws IllegalArgumentException {
    // Arrange
    Bit8StringInterner bit8StringInterner = new Bit8StringInterner(3);

    // Act and Assert
    assertTrue(bit8StringInterner.toggle());
    assertTrue(bit8StringInterner.toggle);
  }

  /**
   * Method under test: {@link AbstractInterner#valueCount()}
   */
  @Test
  public void testValueCount() throws IllegalArgumentException {
    // Arrange, Act and Assert
    assertEquals(0, (new Bit8StringInterner(3)).valueCount());
  }
}

