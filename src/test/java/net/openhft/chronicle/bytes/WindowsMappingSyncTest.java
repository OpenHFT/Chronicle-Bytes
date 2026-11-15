/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.posix.PosixAPI;
import net.openhft.posix.internal.PosixAPIHolder;
import net.openhft.posix.internal.jnr.WinJNRPosixAPI;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class WindowsMappingSyncTest {
    @Test
    public void ownerFlushesAllChunksWithoutSelectingPretoucherStore() throws Exception {
        assumeTrue(OS.isWindows());
        PosixAPI previous = PosixAPI.posix();
        CountingProvider provider = new CountingProvider();
        PosixAPIHolder.POSIX_API = provider.api;
        File file = Files.createTempFile("owner-sync", ".dat").toFile();
        try (MappedBytes writer = MappedBytes.mappedBytes(file, 65536, 0)) {
            writer.writeLong(0, 123);
            MappedBytesStore first = (MappedBytesStore) writer.bytesStore();
            net.openhft.chronicle.core.io.ReferenceOwner owner =
                    net.openhft.chronicle.core.io.ReferenceOwner.temporary("test-live-chunk");
            first.reserve(owner);
            try {
                writer.writeLong(65536, 456);
                long length = file.length();
                writer.mappedFile().syncUpTo(65544, SyncMode.SYNC);
                assertTrue("Both live chunks must flush", provider.calls >= 2);
                assertEquals(length, file.length());
                assertEquals(123, writer.readLong(0));
                assertEquals(456, writer.readLong(65536));
                int calls = provider.calls;
                writer.mappedFile().syncUpTo(0, SyncMode.SYNC);
                assertEquals(calls, provider.calls);
                assertThrows(IllegalArgumentException.class,
                        () -> writer.mappedFile().syncUpTo(length + 1, SyncMode.SYNC));
                assertEquals(length, file.length());
            } finally { first.release(owner); }
        } finally {
            net.openhft.chronicle.core.io.BackgroundResourceReleaser.releasePendingResources();
            PosixAPIHolder.POSIX_API = previous;
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void failedFlushIsRetriedAtSamePosition() throws Exception {
        assumeTrue(OS.isWindows());
        PosixAPI previous = PosixAPI.posix();
        CountingProvider provider = new CountingProvider();
        PosixAPIHolder.POSIX_API = provider.api;
        File file = Files.createTempFile("retry-sync", ".dat").toFile();
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, 65536, 65536)) {
            bytes.writeLong(0, 789);
            MappedBytesStore store = (MappedBytesStore) bytes.bytesStore();
            provider.fail = true;
            assertThrows(IllegalStateException.class, () -> store.syncUpTo(8, SyncMode.SYNC));
            provider.fail = false;
            provider.failForce = true;
            assertThrows(java.io.UncheckedIOException.class, () -> store.syncUpTo(8, SyncMode.SYNC));
            provider.failForce = false;
            store.syncUpTo(8, SyncMode.SYNC);
            assertEquals(3, provider.calls);
            store.syncUpTo(8, SyncMode.SYNC);
            assertEquals(3, provider.calls);
        } finally {
            net.openhft.chronicle.core.io.BackgroundResourceReleaser.releasePendingResources();
            PosixAPIHolder.POSIX_API = previous;
            Files.deleteIfExists(file.toPath());
        }
    }

    private static class CountingProvider {
        int calls;
        boolean fail;
        boolean failForce;
        final PosixAPI delegate = new WinJNRPosixAPI();
        final PosixAPI api = (PosixAPI) java.lang.reflect.Proxy.newProxyInstance(
                PosixAPI.class.getClassLoader(), new Class<?>[]{PosixAPI.class}, (proxy, method, args) -> {
                    if (method.getName().equals("flushMappedView")) {
                        calls++;
                        if (fail)
                            return -1;
                    }
                    try {
                        Object result = method.invoke(delegate, args);
                        if (method.getName().equals("retainFileSync")) {
                            PosixAPI.FileSync retained = (PosixAPI.FileSync) result;
                            return new PosixAPI.FileSync() {
                                public void force() throws java.io.IOException {
                                    if (failForce) throw new java.io.IOException("injected force failure");
                                    retained.force();
                                }
                                public void close() throws java.io.IOException { retained.close(); }
                            };
                        }
                        return result;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}
