/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.posix.PosixAPI;
import net.openhft.posix.internal.PosixAPIHolder;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class MappingSyncLifecycleTest {
    @Test
    public void failedHandleRetentionReleasesUnpublishedMapping() throws Exception {
        assumeTrue(OS.isWindows());
        PosixAPI previous = PosixAPI.posix();
        IOException failure = new IOException("injected handle retention failure");
        PosixAPIHolder.POSIX_API = (PosixAPI) Proxy.newProxyInstance(
                PosixAPI.class.getClassLoader(), new Class<?>[]{PosixAPI.class}, (proxy, method, args) -> {
                    if (method.getName().equals("retainFileSync"))
                        throw failure;
                    try {
                        return method.invoke(previous, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        File file = Files.createTempFile("mapping-retain-failure", ".dat").toFile();
        ReferenceOwner owner = ReferenceOwner.temporary("unpublished-mapping");
        AtomicReference<MappedBytesStore> created = new AtomicReference<>();
        try (MappedFile mapped = MappedFile.mappedFile(file, 65536, 0)) {
            mapped.syncMode(SyncMode.SYNC);
            UncheckedIOException actual = assertThrows(UncheckedIOException.class,
                    () -> mapped.acquireByteStore(owner, 0, null,
                            (o, m, s, a, c, safe, page) -> {
                                MappedBytesStore store = new MappedBytesStore(o, m, s, a, c, safe, page);
                                created.set(store);
                                return store;
                            }));
            assertSame(failure, actual.getCause());
            assertEquals("Failed construction must release its mapping", 0, created.get().refCount());
        } finally {
            // Also keep the deliberately unfixed negative control leak-free.
            if (created.get() != null && created.get().refCount() > 0)
                created.get().release(owner);
            BackgroundResourceReleaser.releasePendingResources();
            PosixAPIHolder.POSIX_API = previous;
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void ownerSyncRetainsStoreUntilConcurrentRetirementCompletes() throws Exception {
        File file = Files.createTempFile("mapping-sync-retire", ".dat").toFile();
        ReferenceOwner owner = ReferenceOwner.temporary("retiring-mapping");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = null;
        MappedBytesStore store = null;
        boolean released = false;
        try (MappedFile mapped = MappedFile.mappedFile(file, 65536, 0)) {
            mapped.syncMode(SyncMode.NONE);
            store = mapped.acquireByteStore(owner, 0, null,
                    (o, m, s, a, c, safe, page) -> new MappedBytesStore(o, m, s, a, c, safe, page) {
                        @Override
                        public void syncUpTo(long position, SyncMode mode) {
                            entered.countDown();
                            try {
                                assertTrue("Sync control timed out", proceed.await(5, TimeUnit.SECONDS));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new AssertionError(e);
                            }
                            super.syncUpTo(position, mode);
                        }
                    });
            store.writeLong(0, 123);
            long size = file.length();
            worker = new Thread(() -> {
                try {
                    mapped.syncUpTo(8, SyncMode.ASYNC);
                } catch (Throwable t) {
                    failure.set(t);
                }
            }, "mapping-owner-sync");
            worker.start();
            try {
                assertTrue("Sync did not acquire the store", entered.await(5, TimeUnit.SECONDS));
                store.release(owner);
                released = true;
                assertTrue("Sync must retain the live address", store.refCount() > 0);
                proceed.countDown();
                worker.join(5000);
                assertFalse("Sync worker survived cleanup", worker.isAlive());
                assertNull(failure.get());
                assertEquals(0, store.refCount());
                assertEquals("Sync must not grow the file", size, file.length());
            } finally {
                proceed.countDown();
                worker.join(5000);
                assertFalse("Sync worker must stop before owner closure", worker.isAlive());
                if (!released)
                    store.release(owner);
            }
        } finally {
            BackgroundResourceReleaser.releasePendingResources();
            Files.deleteIfExists(file.toPath());
        }
    }
}
