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
package net.openhft.chronicle.bytes.domestic;

import static org.junit.Assert.assertFalse;
import java.io.File;
import java.nio.file.Paths;

import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

public class ReentrantFileLockDiffblueTest extends BytesTestCommon {
  /**
  * Method under test: {@link ReentrantFileLock#isHeldByCurrentThread(File)}
  */
  @Test
  public void testIsHeldByCurrentThread() {
    // Arrange, Act and Assert
    assertFalse(
        ReentrantFileLock.isHeldByCurrentThread(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toFile()));
  }
}

