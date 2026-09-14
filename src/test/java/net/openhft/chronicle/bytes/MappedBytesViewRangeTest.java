/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.bytes.algo.OptimisedBytesStoreHash;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class MappedBytesViewRangeTest extends BytesTestCommon {
    @Parameterized.Parameters(name = "writeView={0}")
    public static Collection<Object[]> views() {
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    @Parameterized.Parameter
    public boolean writeView;

    private final long chunk = 16L * OS.pageSize();
    private final long position = chunk - 16;
    private final byte[] data = new byte[64];
    private File file;
    private MappedBytes reader;
    private Bytes<?> view;

    @Before
    public void openView() throws IOException {
        file = new File(OS.getTarget(), "mapped-view-range-" + System.nanoTime() + ".dat");
        for (int i = 0; i < data.length; i++)
            data[i] = (byte) (i * 37 + 11);
        try (MappedBytes writer = MappedBytes.mappedBytes(file, chunk, chunk)) {
            writer.writePosition(position).write(data);
            writer.writePosition(2 * chunk + 64).writeByte((byte) 1);
        }
        reader = MappedBytes.mappedBytes(file, chunk, 0L, true);
        reader.readPositionRemaining(position, data.length);
        view = writeView ? reader.bytesForWrite() : reader.bytesForRead();
        view.readPositionRemaining(position, data.length);
        assertSame(reader, view.bytesStore());
    }

    @After
    public void closeView() {
        if (view != null)
            view.releaseLast();
        if (reader != null)
            reader.releaseLast();
        BackgroundResourceReleaser.releasePendingResources();
        if (file != null)
            deleteIfPossible(file);
    }

    @Test
    public void physicalRangeCheckFollowsViewWithoutRemapping() {
        reader.readByte(2 * chunk);
        final BytesStore<?, ?> mapping = reader.bytesStore();
        assertEquals(2 * chunk, mapping.start());
        assertFalse(BytesInternal.insideCurrentStore(reader, position, data.length));
        assertFalse("the view must reject the same earlier range as its reader",
                BytesInternal.insideCurrentStore(view, position, data.length));
        assertSame("a predicate must not acquire another mapping", mapping, reader.bytesStore());
        assertEquals(position, view.readPosition());
        assertEquals(position, reader.readPosition());
    }

    @Test
    public void unsafeReadsThroughViewsCrossMappings() {
        final Bytes<?> dest = Bytes.allocateDirect(data.length);
        try {
            dest.writePosition(data.length);
            for (boolean ahead : new boolean[]{false, true}) {
                resetView(ahead);
                view.unsafeRead(dest.addressForRead(0), data.length);
                assertArrayEquals(data, readContent(dest));
                assertEquals(position + data.length, view.readPosition());

                resetView(ahead);
                byte[] object = new byte[data.length];
                view.unsafeReadObject(object, Jvm.arrayByteBaseOffset(), data.length);
                assertArrayEquals(data, object);
                assertEquals(position + data.length, view.readPosition());
            }
        } finally {
            dest.releaseLast();
        }
    }

    @Test
    public void copiesFromViewsCrossMappings() throws IOException {
        final Bytes<?> direct = Bytes.allocateDirect(data.length);
        final File destination = new File(OS.getTarget(), "mapped-view-copy-" + System.nanoTime() + ".dat");
        try (MappedBytes mapped = MappedBytes.mappedBytes(destination, chunk, chunk)) {
            for (boolean ahead : new boolean[]{false, true}) {
                for (Bytes<?> dest : new Bytes<?>[]{direct, mapped}) {
                    resetView(ahead);
                    dest.clear().write((BytesStore<?, ?>) view, position, data.length);
                    assertArrayEquals(data, readContent(dest));
                    assertEquals(position, view.readPosition());
                    resetView(ahead);
                    dest.clear().write(0, view, position, data.length);
                    dest.writePosition(data.length);
                    assertArrayEquals(data, readContent(dest));
                    assertEquals(position, view.readPosition());
                }
            }
        } finally {
            direct.releaseLast();
            BackgroundResourceReleaser.releasePendingResources();
            deleteIfPossible(destination);
        }
    }

    @Test
    public void copyFromViewSharingTheDestinationCanRemap() throws IOException {
        try (MappedBytes mapped = MappedBytes.mappedBytes(file, chunk, 0L)) {
            mapped.readPositionRemaining(chunk, 32);
            final Bytes<?> shared = writeView ? mapped.bytesForWrite() : mapped.bytesForRead();
            try {
                shared.readPositionRemaining(chunk, 32);
                mapped.writePosition(2 * chunk + 8);
                mapped.write((BytesStore<?, ?>) shared, chunk, 32);
                byte[] actual = new byte[32];
                mapped.read(2 * chunk + 8, actual, 0, actual.length);
                assertArrayEquals(Arrays.copyOfRange(data, 16, 48), actual);
                assertEquals(2 * chunk + 40, mapped.writePosition());
                assertEquals(chunk, shared.readPosition());
            } finally {
                shared.releaseLast();
            }
        }
    }

    @Test
    public void comparisonsThroughViewsCrossMappings() {
        for (boolean direct : new boolean[]{false, true}) {
            final Bytes<?> other = direct ? Bytes.allocateDirect(data.length) : Bytes.allocateElasticOnHeap(data.length);
            try {
                other.write(data);
                for (boolean equal : new boolean[]{true, false}) {
                    other.writeByte(32, (byte) (equal ? data[32] : data[32] ^ 1));
                    for (boolean reverse : new boolean[]{false, true}) {
                        final Bytes<?> left = reverse ? other : view;
                        final Bytes<?> right = reverse ? view : other;
                        resetView(true);
                        assertEquals(equal, left.equalBytes(right, data.length));
                        resetView(true);
                        assertEquals(equal, left.equals(right));
                        resetView(true);
                        assertEquals(equal, left.startsWith(right));
                        assertEquals(position, view.readPosition());
                        assertEquals(0, other.readPosition());
                    }
                }
            } finally {
                other.releaseLast();
            }
        }
    }

    @Test
    public void nativeHashEntryPointsRejectViewsWithoutRemapping() {
        for (long start : new long[]{chunk - 4, position}) {
            view.readPositionRemaining(start, 33);
            reader.readByte(2 * chunk);
            final BytesStore<?, ?> mapping = reader.bytesStore();
            // The predicate is checked first, so a regression stops before any native hash could read.
            assertFalse(BytesInternal.insideCurrentStore(view, start, data.length));
            assertThrows(UnsupportedOperationException.class, () -> BytesStoreHash.hash(view, 8));
            assertThrows(UnsupportedOperationException.class,
                    () -> OptimisedBytesStoreHash.applyAsLong32bytesMultiple(view, 32));
            assertThrows(UnsupportedOperationException.class,
                    () -> OptimisedBytesStoreHash.applyAsLongAny(view, 33));
            assertSame(mapping, reader.bytesStore());
            assertEquals(start, view.readPosition());
            assertEquals(start + 33, view.readLimit());
        }
    }

    @Test
    public void nativeHashEntryPointsKeepContiguousViewResults() {
        final Bytes<?> direct = Bytes.allocateDirect(data.length).write(data);
        try {
            // A range entirely within the first mapping remains valid through either view type.
            reader.readPosition(position);
            for (int length : new int[]{1, 4, 8, 9, 16}) {
                assertEquals(BytesStoreHash.hash(direct, length), BytesStoreHash.hash(view, length));
                assertEquals(OptimisedBytesStoreHash.applyAsLongAny(direct, length),
                        OptimisedBytesStoreHash.applyAsLongAny(view, length));
            }
            reader.readPosition(chunk);
            reader.readByte(chunk);
            view.readPositionRemaining(chunk, 32);
            direct.readPosition(16);
            assertEquals(OptimisedBytesStoreHash.applyAsLong32bytesMultiple(direct, 32),
                    OptimisedBytesStoreHash.applyAsLong32bytesMultiple(view, 32));
        } finally {
            direct.releaseLast();
        }
    }

    private void resetView(boolean ahead) {
        reader.readPosition(position);
        view.readPositionRemaining(position, data.length);
        if (ahead)
            reader.readByte(2 * chunk);
        assertFalse(BytesInternal.insideCurrentStore(view, position, data.length));
    }

    private byte[] readContent(Bytes<?> bytes) {
        byte[] actual = new byte[data.length];
        bytes.read(0, actual, 0, actual.length);
        return actual;
    }
}
