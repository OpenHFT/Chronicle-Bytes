/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.releaseAndAssertReleased;
import static net.openhft.chronicle.bytes.BytesFactoryUtil.wipe;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BytesReleaseInvariantObjectTest extends BytesTestCommon {

    /**
     * Checks a released Bytes handles "equals()" safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void equalsContentAffects(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        if (readWrite) {
            bytes.writeChar('A');
        }
        final Bytes<?> other = Bytes.from("A");
        try {
            releaseAndAssertReleased(bytes);
            final Executable task = () -> bytes.equals(other);
            assertThrows(ClosedIllegalStateException.class, task, createCommand);
        } finally {
            other.releaseLast();
        }
    }

    /**
     * Checks a released Bytes handles "hashCode()" safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void hashcodeContentAffect(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        releaseAndAssertReleased(bytes);
        final Executable task = bytes::hashCode;
        assertThrows(ClosedIllegalStateException.class, task, createCommand);

    }

    /**
     * Checks a released Bytes handles "toString()" safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void toString(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        final String expected;

        if (readWrite) {
            expected = "The quick brown fox jumped over the usual suspect.";
            bytes.append(expected);
        } else {
            expected = "";
        }
        final String toString = bytes.toString();
        assertEquals(expected, toString);
        releaseAndAssertReleased(bytes);
        assertThrows(ClosedIllegalStateException.class, bytes::toHexString, createCommand);
    }

    //@Test
    void manualTest() {
/*        provideBytesObjects()
                .map(BytesFactoryUtil::bytes)
                .filter(bytes -> bytes.getClass().getSimpleName().contains("Unchecked"))
                .forEach(bytes -> {
                    bytes.append("Arne");
                    releaseAndAssertReleased(bytes);
                    bytes.toString();
                });*/

        //Bytes bytes = wipe(Bytes.allocateDirect(SIZE).unchecked(true));
        HexDumpBytes bytes = wipe(new HexDumpBytes());
        // bytes.contentDependentHashcodeAndEquals(false);
        bytes.append("Arne");
        releaseAndAssertReleased(bytes);
        final int hash = bytes.hashCode();
        final int expected = System.identityHashCode(bytes);
    }

}