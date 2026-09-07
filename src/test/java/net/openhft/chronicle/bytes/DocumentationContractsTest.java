/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.Before;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.io.ReferenceOwner.INIT;
import static org.junit.Assert.*;

public class DocumentationContractsTest extends BytesTestCommon {
    @Before
    public void requireCheckedConfiguration() {
        assertFalse("These contracts require checked bounds", Jvm.getBoolean("bytes.bounds.unchecked"));
        String expected = System.getProperty("docs.expectedAssertions");
        if (expected != null)
            assertEquals("Actual assertion state in the library", Boolean.parseBoolean(expected),
                    AbstractBytes.class.desiredAssertionStatus());
    }

    @Test
    public void checkedHeapBoundaries() {
        checkSequentialBoundaries(Bytes.allocateElasticOnHeap(8));
    }

    @Test
    public void checkedNativeBoundaries() {
        checkSequentialBoundaries(nativeBytes(8));
    }

    private void checkSequentialBoundaries(Bytes<?> bytes) {
        try {
            bytes.writeLimit(4);
            assertThrows(BufferOverflowException.class, () -> bytes.writeLong(1));
            assertEquals(0, bytes.writePosition());
            bytes.writeInt(42);
            assertThrows(BufferUnderflowException.class, bytes::readLong);
            assertEquals(0, bytes.readPosition());
            assertEquals(42, bytes.readInt());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void nativeAllocationGrowsBeyondInitialRealCapacity() {
        Bytes<?> bytes = nativeBytes(8);
        try {
            long initial = bytes.realCapacity();
            bytes.writeLong(11).writeLong(22);
            assertTrue(bytes.realCapacity() > initial);
            assertEquals(11, bytes.readLong());
            assertEquals(22, bytes.readLong());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void explicitMaximumRejectsGrowth() {
        Bytes<?> bytes = Bytes.elasticByteBuffer(8, 32);
        try {
            bytes.ensureCapacity(32);
            assertEquals(32, bytes.capacity());
            assertEquals(32, bytes.realCapacity());
            assertThrows(BufferOverflowException.class, () -> bytes.ensureCapacity(33));
            bytes.write(new byte[32]);
            assertThrows(BufferOverflowException.class, () -> bytes.writeByte((byte) 1));
            assertEquals(32, bytes.realCapacity());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void writeLimitIsNotAnAllocationBudget() {
        Bytes<?> bytes = nativeBytes(8);
        try {
            bytes.writeLimit(4);
            bytes.ensureCapacity(16);
            assertTrue(bytes.realCapacity() >= 16);
            assertEquals(4, bytes.writeLimit());
            assertThrows(BufferOverflowException.class, () -> bytes.writeLong(1));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void viewSurvivesParentRelease() {
        checkReleaseOrder(true);
    }

    @Test
    public void parentSurvivesViewRelease() {
        checkReleaseOrder(false);
    }

    private void checkReleaseOrder(boolean parentFirst) {
        Bytes<?> parent = nativeBytes(8);
        Bytes<?> view = null;
        BytesStore<?, ?> store = parent.bytesStore(); // borrowed, not an extra reservation
        try {
            parent.writeInt(42);
            view = parent.bytesForRead();
            assertSame(store, view.bytesStore());
            assertEquals(2, store.refCount());
            if (parentFirst) {
                parent.releaseLast();
                parent = null;
                assertEquals(42, view.readInt());
            } else {
                view.releaseLast();
                view = null;
                assertEquals(42, parent.readInt());
            }
            assertEquals(1, store.refCount());
        } finally {
            if (view != null)
                view.releaseLast();
            if (parent != null)
                parent.releaseLast();
        }
        assertEquals(0, store.refCount());
    }

    @Test
    public void viewSurvivesStoreOwnerRelease() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(8);
        Bytes<?> view;
        try {
            view = store.bytesForWrite();
        } finally {
            store.release(INIT); // balances the factory's reservation, not the view's
        }
        try {
            assertEquals(1, store.refCount());
            view.writeInt(42);
            assertEquals(42, view.readInt());
        } finally {
            view.releaseLast();
        }
        assertEquals(0, store.refCount());
    }

    @Test
    public void viewKeepsOldStoreAfterNativeGrowth() {
        Bytes<?> parent = nativeBytes(8);
        Bytes<?> view = null;
        BytesStore<?, ?> oldStore = parent.bytesStore();
        try {
            parent.writeInt(11);
            view = parent.bytesForRead();
            parent.writeInt(0, 22);
            assertEquals(22, view.readInt(0));

            parent.ensureCapacity(oldStore.realCapacity() + 1);
            assertNotSame(oldStore, parent.bytesStore());
            assertSame(oldStore, view.bytesStore());
            assertEquals(1, oldStore.refCount());
            assertEquals(22, parent.readInt(0));

            parent.writeInt(0, 33);
            assertEquals(22, view.readInt(0));
            view.writeInt(0, 44);
            assertEquals(33, parent.readInt(0));
        } finally {
            if (view != null)
                view.releaseLast();
            parent.releaseLast();
        }
        assertEquals(0, oldStore.refCount());
    }

    @Test
    public void guardedPrimitiveChangesRepresentation() {
        Bytes<?> ordinary = nativeBytes(8);
        Bytes<?> guarded = new GuardedNativeBytes<>(BytesStore.empty(), 32);
        try {
            ordinary.writeInt(42);
            guarded.writeInt(42);
            assertEquals(4, ordinary.writePosition());
            assertEquals(5, guarded.writePosition());
            assertEquals(GuardedNativeBytes.INT_T, guarded.bytesStore().readByte(0));
            assertEquals(42, guarded.bytesStore().readInt(1));
            assertEquals(42, ordinary.readInt());
            assertEquals(42, guarded.readInt());
        } finally {
            guarded.releaseLast();
            ordinary.releaseLast();
        }
    }

    private Bytes<?> nativeBytes(int initialCapacity) {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(initialCapacity);
        try {
            return new NativeBytes<>(store, Bytes.MAX_CAPACITY);
        } finally {
            store.release(INIT);
        }
    }
}
