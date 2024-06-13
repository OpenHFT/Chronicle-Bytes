package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.util.AbstractInterner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractInternerTest {

    private static final class TestInterner extends AbstractInterner<String> {
        protected TestInterner(int capacity) {
            super(capacity);
        }

        @Override
        protected String getValue(BytesStore bs, int length) {
            return bs.toString();
        }
    }

    private TestInterner interner;

    @BeforeEach
    void setUp() {
        interner = new TestInterner(256);
    }

    @Test
    void internBytesObjectWithExistingInstance() {
        Bytes<?> bytes = Bytes.from("testString");
        String firstInterned = interner.intern(bytes);
        String secondInterned = interner.intern(bytes);
        Assertions.assertSame(firstInterned, secondInterned);
    }

    @Test
    void internBytesObjectWithNewInstance() {
        Bytes<?> firstBytes = Bytes.from("firstString");
        Bytes<?> secondBytes = Bytes.from("secondString");
        String firstInterned = interner.intern(firstBytes);
        String secondInterned = interner.intern(secondBytes);
        Assertions.assertNotSame(firstInterned, secondInterned);
    }

    @Test
    void internDifferentLengthBytes() {
        Bytes<?> shortBytes = Bytes.from("short");
        Bytes<?> longBytes = Bytes.from("aVeryLongStringIndeed");
        String shortInterned = interner.intern(shortBytes);
        String longInterned = interner.intern(longBytes);
        Assertions.assertNotSame(shortInterned, longInterned);
    }

    @Test
    void valueCountWithMultipleInterns() {
        Bytes<?> firstBytes = Bytes.from("firstString");
        Bytes<?> secondBytes = Bytes.from("secondString");
        interner.intern(firstBytes);
        interner.intern(secondBytes);
        int count = interner.valueCount();
        Assertions.assertTrue(count >= 2);
    }

    @Test
    void toggleCheck() {
        Bytes<?> firstBytes = Bytes.from("toggleFirst");
        Bytes<?> secondBytes = Bytes.from("toggleSecond");
        interner.intern(firstBytes);
        interner.toggle();
        interner.intern(secondBytes);
        int count = interner.valueCount();
        Assertions.assertTrue(count >= 2);
    }
}
