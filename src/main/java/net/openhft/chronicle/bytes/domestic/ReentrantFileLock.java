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
package net.openhft.chronicle.bytes.domestic;

import net.openhft.chronicle.bytes.internal.CanonicalPathUtil;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.threads.CleaningThreadLocal;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * A way of acquiring exclusive locks on files in a re-entrant fashion.
 * <p>
 * It will prevent a single thread that uses this interface to acquire locks from causing
 * an {@link OverlappingFileLockException}. Separate threads will not be prevented from taking
 * overlapping file locks.
 * <p>
 * All the usual caveats around file locks apply, shared locks and locks for specific ranges are
 * not supported.
 */
public final class ReentrantFileLock extends FileLock {

    private static final ThreadLocal<HashMap<String, ReentrantFileLock>> heldLocks =
            CleaningThreadLocal.withCleanup(HashMap::new, h -> closeQuietly(h.values()));

    private final String canonicalPath;
    private final FileLock delegate;
    private final long owningThreadId;
    private int counter;

    public ReentrantFileLock(String canonicalPath, FileLock fileLock) {
        super(fileLock.channel(), fileLock.position(), fileLock.size(), fileLock.isShared());
        this.canonicalPath = canonicalPath;
        this.delegate = fileLock;
        this.owningThreadId = Thread.currentThread().getId();
        this.counter = 1;
    }

    @Override
    public Channel acquiredBy() {
        checkThreadAccess();
        return delegate.acquiredBy();
    }

    @Override
    public boolean isValid() {
        checkThreadAccess();
        return delegate.isValid();
    }

    @Override
    public void release() throws IOException {
        checkThreadAccess();
        if (--counter == 0) {
            try {
                delegate.release();
            } finally {
                heldLocks.get().remove(canonicalPath);
            }
        }
    }

    private ReentrantFileLock incrementCounter() {
        checkThreadAccess();
        counter++;
        return this;
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
    public static ReentrantFileLock tryLock(File file, FileChannel fileChannel) throws IOException {
        final String canonicalPath = CanonicalPathUtil.of(file);
        final ReentrantFileLock reentrantFileLock = heldLocks.get().get(canonicalPath);
        if (reentrantFileLock != null) {
            return reentrantFileLock.incrementCounter();
        }

        final FileLock lock = fileChannel.tryLock();
        if (lock != null) {
            ReentrantFileLock refl = new ReentrantFileLock(canonicalPath, lock);
            heldLocks.get().put(canonicalPath, refl);
            return refl;
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
    public static ReentrantFileLock lock(File file, FileChannel fileChannel) throws IOException {
        final String canonicalPath = CanonicalPathUtil.of(file);
        final ReentrantFileLock reentrantFileLock = heldLocks.get().get(canonicalPath);
        if (reentrantFileLock != null) {
            return reentrantFileLock.incrementCounter();
        }

        final FileLock lock = fileChannel.lock();
        ReentrantFileLock refl = new ReentrantFileLock(canonicalPath, lock);
        heldLocks.get().put(canonicalPath, refl);
        return refl;
    }

    /**
     * Is there a cached FileLock held by the current thread for the specified file
     *
     * @param file The file to check
     * @return true if there is a cached file lock, false otherwise
     */
    public static boolean isHeldByCurrentThread(File file) {
        return heldLocks.get().containsKey(CanonicalPathUtil.of(file));
    }

    /**
     * Log an error if someone is passing around ReentrantFileLocks between threads
     */
    private void checkThreadAccess() {
        final long currentThreadId = Thread.currentThread().getId();
        if (currentThreadId != owningThreadId) {
            Jvm.error().on(ReentrantFileLock.class, "You're accessing a ReentrantFileLock created by thread " + owningThreadId + " on thread " + currentThreadId + " this can have unexpected results, don't do it.");
        }
    }
}
