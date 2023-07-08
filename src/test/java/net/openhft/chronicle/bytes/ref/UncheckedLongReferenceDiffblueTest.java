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

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class UncheckedLongReferenceDiffblueTest extends BytesTestCommon {
  /**
   * Method under test: {@link UncheckedLongReference#bytesStore()}
   */
  @Test
  public void testBytesStore() {
    // Arrange, Act and Assert
    NativeBytesStore bytes = NativeBytesStore.from("01234567");
    try (UncheckedLongReference reference = new UncheckedLongReference()) {
      reference.bytesStore(bytes, 0, 8);
      assertEquals("01234567", reference.bytesStore().toString());
    }
    bytes.releaseLast();
  }

  /**
  * Method under test: default or parameterless constructor of {@link UncheckedLongReference}
  */
  @Test
  public void testConstructor() {
    // Arrange, Act and Assert
    UncheckedLongReference reference = new UncheckedLongReference();
    assertFalse(reference.isClosing());
    reference.close();
    assertTrue(reference.isClosing());
  }
}

