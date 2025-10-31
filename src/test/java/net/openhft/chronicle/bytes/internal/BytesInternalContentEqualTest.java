/*
 * Copyright 2016-2025 chronicle.software
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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BytesInternalContentEqualTest extends BytesTestCommon {

    @Test
    public void heapVsDirectEqualContent() {
        Bytes<?> heap = Bytes.from("abcdef");
        Bytes<?> direct = Bytes.allocateDirect(6);
        try {
            direct.append("abcdef");
            // ensure comparisons start from position 0
            heap.readPosition(0);
            direct.readPosition(0);

            assertTrue("Expected equal content across heap and direct stores",
                    BytesInternal.contentEqual(heap.bytesStore(), direct.bytesStore()));
        } finally {
            heap.releaseLast();
            direct.releaseLast();
        }
    }

    @Test
    public void differentLengthsAreNotEqual() {
        Bytes<?> left = Bytes.from("abc");
        Bytes<?> right = Bytes.from("abcd");
        try {
            assertFalse(BytesInternal.contentEqual(left.bytesStore(), right.bytesStore()));
        } finally {
            left.releaseLast();
            right.releaseLast();
        }
    }

    @Test
    public void singleByteMismatchDetected() {
        Bytes<?> left = Bytes.from("abcde");
        Bytes<?> right = Bytes.from("abXde");
        try {
            assertFalse(BytesInternal.contentEqual(left.bytesStore(), right.bytesStore()));
        } finally {
            left.releaseLast();
            right.releaseLast();
        }
    }
}
