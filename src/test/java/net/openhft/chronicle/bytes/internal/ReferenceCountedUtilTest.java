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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceCounted;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceCountedUtilTest extends BytesTestCommon {

    @Test
    void throwExceptionIfReleased() {
        test(o -> ReferenceCountedUtil.throwExceptionIfReleased((ReferenceCounted) o));
    }

    @Test
    void testThrowExceptionIfReleased() {
        test(ReferenceCountedUtil::throwExceptionIfReleased);
        assertDoesNotThrow(() -> ReferenceCountedUtil.throwExceptionIfReleased("Foo"));
        assertThrows(NullPointerException.class, () -> ReferenceCountedUtil.throwExceptionIfReleased(null));
    }

    private void test(Consumer<Object> method) {
        final Bytes<?> bytes = Bytes.from("A");
        assertDoesNotThrow(() -> method.accept(bytes));
        bytes.releaseLast();
        assertThrows(ClosedIllegalStateException.class, () -> method.accept(bytes));
    }
}
