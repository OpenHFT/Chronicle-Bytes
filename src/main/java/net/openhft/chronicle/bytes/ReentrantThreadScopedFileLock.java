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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.CanonicalPathUtil;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A way of acquiring exclusive locks on files in a re-entrant fashion.
 * <p>
 * It will avoid {@link OverlappingFileLockException} for threads that share a class-loader, but
 * they can still occur if there are threads in multiple class loaders taking locks.
 * <p>
 * All the usual caveats around file locks apply, shared locks and locks for specific ranges are
 * not supported.
 */
public final class ReentrantThreadScopedFileLock implements AutoCloseable {

    private static final ConcurrentHashMap<String, LockHolder> lockHolders = new ConcurrentHashMap<>();

    private final LockHolder lockHolder;

    private ReentrantThreadScopedFileLock(LockHolder lockHolder) {
        this.lockHolder = lockHolder;
    }

    @Override
    public void close() throws IOException {
        unlock(lockHolder);
    }

    /**
     * Try and take an exclusive lock on the entire file, non-blocking
     *
     * @param file        The file to lock
     * @param fileChannel An open {@link FileChannel to the file}
     * @return the lock if it was acquired, or null if it could not be acquired
     * @throws IOException
     */
    @Nullable
    public static ReentrantThreadScopedFileLock tryLock(File file, FileChannel fileChannel) throws IOException {
        final LockHolder lockHolderForFile = lockHolders.computeIfAbsent(CanonicalPathUtil.of(file), LockHolder::new);
        final boolean acquired = lockHolderForFile.lock.tryLock();
        if (acquired) {
            // If this is the first acquisition for the thread, try and acquire the file lock
            if (lockHolderForFile.lock.getHoldCount() == 1) {
                try {
                    final FileLock fileLock = fileChannel.tryLock();
                    if (fileLock != null) {
                        lockHolderForFile.fileLock = fileLock;
                    } else {
                        // another process has the file lock, unlock our local lock and return null
                        lockHolderForFile.lock.unlock();
                        return null;
                    }
                } catch (OverlappingFileLockException e) {
                    Jvm.warn().on(ReentrantThreadScopedFileLock.class, "Another thread holds a lock on this file directly");
                    lockHolderForFile.lock.unlock();
                    return null;
                } catch (Exception e) {
                    lockHolderForFile.lock.unlock();
                    throw e;
                }
                return new ReentrantThreadScopedFileLock(lockHolderForFile);
            }
        }
        return null;
    }

    /**
     * Take an exclusive lock on the entire file, blocks until lock is acquired
     *
     * @param file        The file to lock
     * @param fileChannel An open {@link FileChannel to the file}
     * @return the lock if it was acquired, or null if it could not be acquired
     * @throws IOException
     */
    public static ReentrantThreadScopedFileLock lock(File file, FileChannel fileChannel) throws IOException {
        final LockHolder lockHolderForFile = lockHolders.computeIfAbsent(CanonicalPathUtil.of(file), LockHolder::new);
        lockHolderForFile.lock.lock();
        // If this is the first entry for the thread, try and acquire the file lock
        if (lockHolderForFile.lock.getHoldCount() == 1) {
            try {
                lockHolderForFile.fileLock = fileChannel.lock();
            } catch (Exception e) {
                lockHolderForFile.lock.unlock();
                throw e;
            }
        }
        return new ReentrantThreadScopedFileLock(lockHolderForFile);
    }

    public static boolean isLocked(File file) {
        return lockHolders.computeIfAbsent(CanonicalPathUtil.of(file), LockHolder::new).lock.isLocked();
    }

    public static boolean isHeldByCurrentThread(File file) {
        return lockHolders.computeIfAbsent(CanonicalPathUtil.of(file), LockHolder::new).lock.isHeldByCurrentThread();
    }

    private static void unlock(LockHolder lockHolder) throws IOException {
        if (!lockHolder.lock.isHeldByCurrentThread()) {
            Jvm.error().on(ReentrantThreadScopedFileLock.class, "Attempted to unlock a lock not held by us, this is a bug");
            return;
        }
        final int holdCount = lockHolder.lock.getHoldCount();
        if (holdCount == 1) {
            try {
                lockHolder.fileLock.release();
            } catch (ClosedChannelException e) {
                Jvm.error().on(ReentrantThreadScopedFileLock.class, "Channel closed while unlocking " + lockHolder.canonicalPath, e);
            } catch (IOException e) {
                Jvm.error().on(ReentrantThreadScopedFileLock.class, "Error releasing lock from " + lockHolder.canonicalPath, e);
                lockHolder.lock.unlock();   // still unlock the process-scoped lock?
                throw e;
            }
        }
        lockHolder.lock.unlock();
    }

    private static class LockHolder {
        private final String canonicalPath;
        @NotNull
        private final ReentrantLock lock = new ReentrantLock();
        @Nullable
        private FileLock fileLock;

        public LockHolder(String canonicalPath) {
            this.canonicalPath = canonicalPath;
        }
    }
}
