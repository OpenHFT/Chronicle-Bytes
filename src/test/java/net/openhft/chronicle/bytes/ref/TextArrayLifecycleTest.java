/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TextArrayLifecycleTest {
    @Parameterized.Parameters(name = "longWidth={0}")
    public static Collection<Object[]> widths() {
        return Arrays.asList(new Object[][]{{false}, {true}});
    }
    private final boolean longWidth;
    public TextArrayLifecycleTest(boolean longWidth) { this.longWidth = longWidth; }

    private AbstractReference reference() {
        return longWidth ? new TextLongArrayReference() : new TextIntArrayReference();
    }
    private void write(Bytes<?> bytes, int capacity) {
        if (longWidth) TextLongArrayReference.write(bytes, capacity);
        else TextIntArrayReference.write(bytes, capacity);
    }
    private void reset(AbstractReference ref) {
        if (longWidth) ((TextLongArrayReference) ref).reset();
        else ((TextIntArrayReference) ref).reset();
    }
    @Test
    public void resetReleasesReservation() {
        Bytes<?> bytes = Bytes.allocateDirect(512);
        BytesStore<?, ?> store = bytes.bytesStore();
        int initial = store.refCount();
        AbstractReference ref = reference();
        try {
            write(bytes, 3);
            ref.bytesStore(bytes, 0, bytes.writePosition());
            assertEquals(initial + 1, store.refCount());
            reset(ref);
            assertEquals(initial, store.refCount());
            reset(ref);
            assertEquals(initial, store.refCount());
        } finally {
            ref.close();
            if (store.refCount() > initial) store.release(ref);
            bytes.releaseLast();
        }
    }
    @Test
    public void emptyArrayCanBindItsWrittenRepresentation() {
        Bytes<?> bytes = Bytes.allocateDirect(512);
        try (AbstractReference ref = reference()) {
            write(bytes, 0);
            ref.bytesStore(bytes, 0, bytes.writePosition());
            assertEquals(bytes.writePosition(), ref.maxSize());
        } finally { bytes.releaseLast(); }
    }
    @Test
    public void declaredSizeMatchesWrittenRepresentation() {
        Bytes<?> bytes = Bytes.allocateDirect(512);
        try (AbstractReference ref = reference()) {
            for (int capacity : new int[]{0, 1, 3}) {
                bytes.clear();
                write(bytes, capacity);
                long declared = longWidth ? ((TextLongArrayReference) ref).sizeInBytes(capacity)
                        : ((TextIntArrayReference) ref).sizeInBytes(capacity);
                assertEquals("capacity=" + capacity, bytes.writePosition(), declared);
            }
        } finally { bytes.releaseLast(); }
    }
}
