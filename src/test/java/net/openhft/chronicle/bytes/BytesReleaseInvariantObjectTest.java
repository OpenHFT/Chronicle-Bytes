package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.*;
import static org.junit.jupiter.api.Assertions.*;

final class BytesReleaseInvariantObjectTest extends BytesTestCommon {

    /**
     * Checks a released Bytes handles "equals()" safely for versions where content matters
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void equalsContentAffects(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        equals(bytes, readWrite, createCommand, true);
    }

    /**
     * Checks a released Bytes handles "equals()" safely for versions where content does not matter
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void equals(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        equals(bytes, readWrite, createCommand, false);
    }

    void equals(final Bytes<?> bytes,
                final boolean readWrite,
                final String createCommand,
                boolean contentAffects) {
        if (readWrite) {
            bytes.writeChar('A');
        }
        final Bytes<?> other = Bytes.from("A");
        try {
            // You might think this is a redundant assertion but it is not because we are using
            // delegates in combination with object identity checks.
            assertTrue(bytes.equals(bytes));

            releaseAndAssertReleased(bytes);
            final Executable task = () -> bytes.equals(other);
            contentDependentHashcodeAndEquals(bytes, contentAffects);
            if (contentAffects) {
                assertThrows(ClosedIllegalStateException.class, () -> bytes.equals(other), createCommand);
            } else {
                assertDoesNotThrow(task, createCommand);
                assertNotEquals(bytes, other);
            }
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
        hashCode(bytes, readWrite, createCommand, true);
    }

    /**
     * Checks a released Bytes handles "hashCode()" safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void hashCode(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        hashCode(bytes, readWrite, createCommand, false);
    }

    void hashCode(final Bytes<?> bytes,
                  final boolean readWrite,
                  final String createCommand,
                  final boolean contentAffects) {
        releaseAndAssertReleased(bytes);
        final Executable task = bytes::hashCode;
        contentDependentHashcodeAndEquals(bytes, contentAffects);
        if (contentAffects) {
            assertThrows(ClosedIllegalStateException.class, task, createCommand);
        } else {
            assertDoesNotThrow(task, createCommand);
            assertEquals(System.identityHashCode(bytes), bytes.hashCode());
        }
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
        assertThrows(ClosedIllegalStateException.class, bytes::toString, createCommand);
    }

    /**
     * Checks a released Bytes handles some other operations correctly
     */
    @TestFactory
    Stream<DynamicTest> toHexString() {
        return cartesianProductTest(BytesFactoryUtil::provideBytesObjects,
                BytesReleaseInvariantObjectTest::provideToOperations,
                (args, bytes, nc) -> {
                    if (bytes.refCount() > 0) {
                        // If not released, write some content and release
                        if (isReadWrite(args)) {
                            bytes.append("John");
                        }
                        bytes.releaseLast();
                    }
                    assertThrows(ClosedIllegalStateException.class, () -> nc.accept(bytes), createCommand(args));
                }
        );
    }

    private static Stream<NamedConsumer<Bytes<Object>>> provideToOperations() {
        return Stream.of(
                NamedConsumer.of(Bytes::toHexString, "toHexString()"),
                NamedConsumer.of(bytes -> bytes.toHexString(10), "bytes.toHexString(int)"),
                NamedConsumer.of(bytes -> bytes.toHexString(1, 10), "bytes.toHexString(int, int)"),
                NamedConsumer.of(RandomDataInput::toByteArray, "bytes.toByteArray()"),
                NamedConsumer.of(BytesStore::toDebugString, "toDebugString()"),
                NamedConsumer.of(bytes -> bytes.toDebugString(10), "toDebugString(10)"),
                NamedConsumer.of(BytesStore::to8bitString, "to8bitString")
        );
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

        //Bytes<?> bytes = wipe(Bytes.allocateDirect(SIZE).unchecked(true));
        HexDumpBytes bytes = wipe(new HexDumpBytes());
        bytes.contentDependentHashcodeAndEquals(false);
        bytes.append("Arne");
        releaseAndAssertReleased(bytes);
        final int hash = bytes.hashCode();
        final int expected = System.identityHashCode(bytes);
    }

    private void contentDependentHashcodeAndEquals(final Bytes<?> bytes,
                                                   final boolean contentAffects) {
        if (bytes instanceof AbstractBytes) {
            ((AbstractBytes<?>) bytes).contentDependentHashcodeAndEquals(contentAffects);
        } else if (bytes instanceof UncheckedNativeBytes) {
            ((UncheckedNativeBytes<?>) bytes).contentDependentHashcodeAndEquals(contentAffects);
        } else if (bytes instanceof HexDumpBytes) {
            ((HexDumpBytes) bytes).contentDependentHashcodeAndEquals(contentAffects);
        } else {
            throw new UnsupportedOperationException("Unable to set in " + bytes.getClass().getName());
        }
    }


}