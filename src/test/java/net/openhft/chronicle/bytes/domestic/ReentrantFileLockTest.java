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

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.threads.ThreadDump;
import net.openhft.chronicle.testframework.process.JavaProcessBuilder;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ReentrantFileLockTest extends BytesTestCommon {

    private static final int NUM_THREADS = 4;
    private static final int NUM_ITERATIONS = 300;
    private File fileToLock;

    @BeforeEach
    public void setUp() {
        fileToLock = IOTools.createTempFile("fileToLock");
    }

    @Before
    @BeforeEach
    public void threadDump() {
        super.threadDump();
    }

    @AfterEach
    public void tearDown() {
        fileToLock.delete();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void willAcquireLockOnFileWhenAvailableAndReleaseOnLastRelease(boolean useTryLock) throws IOException {
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final ReentrantFileLock lock = acquireLock(useTryLock, fileToLock, channel);
            final ReentrantFileLock secondLock = acquireLock(useTryLock, fileToLock, channel);
            assertTrue(ReentrantFileLock.isHeldByCurrentThread(fileToLock));
            Closeable.closeQuietly(lock);
            assertTrue(ReentrantFileLock.isHeldByCurrentThread(fileToLock));
            Closeable.closeQuietly(secondLock);
            assertFalse(ReentrantFileLock.isHeldByCurrentThread(fileToLock));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void willThrowOverlappingFileLockExceptionWhenAnOverlappingLockIsHeldDirectly(boolean useTryLock) throws IOException {
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final FileLock lock = channel.lock();
            assertThrows(OverlappingFileLockException.class, () -> acquireLock(useTryLock, fileToLock, channel));
            assertFalse(ReentrantFileLock.isHeldByCurrentThread(fileToLock));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void willPropagateOtherExceptionsOnAcquire(boolean useTryLock) throws IOException {
        FileChannel closedChannel;
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            closedChannel = channel;
        }
        assertThrows(ClosedChannelException.class, () -> acquireLock(useTryLock, fileToLock, closedChannel));
        assertFalse(ReentrantFileLock.isHeldByCurrentThread(fileToLock));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void unlockWillPropagateChannelClosedExceptionOnUnlock(boolean useTryLock) throws IOException {
        final ReentrantFileLock rtsfl;
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            rtsfl = acquireLock(useTryLock, fileToLock, channel);
        }
        assertThrows(ClosedChannelException.class, rtsfl::close);
        assertFalse(ReentrantFileLock.isHeldByCurrentThread(fileToLock));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void errorIsLoggedWhenLocksArePassedBetweenThreads(boolean useTryLock) throws IOException, ExecutionException, InterruptedException {
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final ReentrantFileLock lock = acquireLock(useTryLock, fileToLock, channel);
            final AtomicLong spawnedThreadId = new AtomicLong();
            Executors.newSingleThreadExecutor().submit(() -> {
                spawnedThreadId.set(Thread.currentThread().getId());
                assertTrue(lock.isValid());
            }).get();
            expectException("You're accessing a ReentrantFileLock created by thread " + Thread.currentThread().getId() + " on thread " + spawnedThreadId.get() + " this can have unexpected results, don't do it.");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void providesMutualExclusionBetweenProcesses(boolean useTryLock) throws IOException {
        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            processes.add(JavaProcessBuilder.create(LockerThread.class)
                    .withProgramArguments(fileToLock.getCanonicalPath(), String.valueOf(i), String.valueOf(useTryLock))
                    .start());
        }
        processes.forEach(future -> {
            try {
                int exitValue = future.waitFor();
                if (exitValue != 0) {
                    JavaProcessBuilder.printProcessOutput("locker", future);
                    fail();
                }
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    public void noLockIsCachedOnFailedTryLock() throws IOException, InterruptedException {
        final Process processHoldingLock = JavaProcessBuilder.create(LockUntilInterruptedThread.class)
                .withProgramArguments(fileToLock.getCanonicalPath())
                .start();
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            while (true) {
                try (final ReentrantFileLock reentrantFileLock = ReentrantFileLock.tryLock(fileToLock, channel)) {
                    if (reentrantFileLock == null) {
                        break;
                    }
                }
            }
            assertFalse(ReentrantFileLock.isHeldByCurrentThread(fileToLock));
        } finally {
            processHoldingLock.destroy();
            assertTrue(processHoldingLock.waitFor(5, TimeUnit.SECONDS));
        }
    }

    private static ReentrantFileLock acquireLock(boolean useTryLock, File file, FileChannel fileChannel) throws IOException {
        if (useTryLock) {
            return ReentrantFileLock.tryLock(file, fileChannel);
        } else {
            return ReentrantFileLock.lock(file, fileChannel);
        }
    }

    private static final class LockUntilInterruptedThread implements Runnable {

        private final String filePath;

        public static void main(String[] args) {
            new LockUntilInterruptedThread(args[0]).run();
        }

        public LockUntilInterruptedThread(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void run() {
            final File fileToLock = new File(filePath);
            try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                int acquiredCount = 0;
                while (acquiredCount < NUM_ITERATIONS) {
                    try (final ReentrantFileLock lock = ReentrantFileLock.lock(fileToLock, channel)) {
                        while (!Thread.currentThread().isInterrupted()) {
                            Jvm.pause(1);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class LockerThread implements Runnable {

        private final String filePath;
        private final int identifier;
        private final boolean useTryLock;
        private final ByteBuffer buffer = ByteBuffer.allocate(NUM_THREADS);

        public static void main(String[] args) {
            new LockerThread(args[0], Integer.parseInt(args[1]), Boolean.parseBoolean(args[2])).run();
        }

        public LockerThread(String filePath, int identifier, boolean useTryLock) {
            this.filePath = filePath;
            this.identifier = identifier;
            this.useTryLock = useTryLock;
        }

        @Override
        public void run() {
            final File fileToLock = new File(filePath);
            try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                int acquiredCount = 0;
                while (acquiredCount < NUM_ITERATIONS) {
                    try (final ReentrantFileLock lock = acquireLock(useTryLock, fileToLock, channel)) {
                        if (lock != null) {
                            writeIdentifier(channel);
                            Jvm.pause(ThreadLocalRandom.current().nextInt(5));
                            final int identifierInFile = readIdentifier(channel);
                            if (identifierInFile != identifier) {
                                throw new RuntimeException("Expected " + identifier + " got " + identifierInFile);
                            }
                            acquiredCount++;
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private int readIdentifier(FileChannel channel) {
            try {
                buffer.clear();
                channel.read(buffer, 0);
                buffer.flip();
                return buffer.getInt();
            } catch (IOException e) {
                throw new RuntimeException("Couldn't read ID", e);
            }
        }

        private void writeIdentifier(FileChannel channel) {
            try {
                buffer.clear();
                buffer.putInt(identifier);
                buffer.flip();
                channel.write(buffer, 0);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't write ID", e);
            }
        }
    }
}