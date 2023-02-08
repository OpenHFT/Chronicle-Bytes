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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.testframework.process.JavaProcessBuilder;
import org.junit.Test;

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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class ReentrantThreadScopedFileLockTest extends BytesTestCommon {

    private static final int NUM_THREADS = 4;
    private static final int NUM_ITERATIONS = 300;

    @Test
    public void willAcquireLockOnFileWhenAvailable() throws IOException {
        final File fileToLock = IOTools.createTempFile("fileToLock");
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final ReentrantThreadScopedFileLock lock = ReentrantThreadScopedFileLock.lock(fileToLock, channel);
            final ReentrantThreadScopedFileLock secondLock = ReentrantThreadScopedFileLock.lock(fileToLock, channel);
            assertTrue(ReentrantThreadScopedFileLock.isLocked(fileToLock));
            assertTrue(ReentrantThreadScopedFileLock.isHeldByCurrentThread(fileToLock));
            Closeable.closeQuietly(lock);
            assertTrue(ReentrantThreadScopedFileLock.isLocked(fileToLock));
            assertTrue(ReentrantThreadScopedFileLock.isHeldByCurrentThread(fileToLock));
            Closeable.closeQuietly(secondLock);
            assertFalse(ReentrantThreadScopedFileLock.isLocked(fileToLock));
            assertFalse(ReentrantThreadScopedFileLock.isHeldByCurrentThread(fileToLock));
        }
    }

    @Test
    public void lockWillThrowOverlappingFileLockExceptionWhenAnOverlappingLockIsHeldDirectly() throws IOException {
        final File fileToLock = IOTools.createTempFile("fileToLock");
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final FileLock lock = channel.lock();
            assertThrows(OverlappingFileLockException.class, () -> ReentrantThreadScopedFileLock.lock(fileToLock, channel));
            assertFalse(ReentrantThreadScopedFileLock.isLocked(fileToLock));
        }
    }

    @Test
    public void tryLockWillReturnNullWhenAnOverlappingLockIsHeldDirectly() throws IOException {
        expectException("Another thread holds a lock on this file directly");
        final File fileToLock = IOTools.createTempFile("fileToLock");
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final FileLock lock = channel.lock();
            assertNull(ReentrantThreadScopedFileLock.tryLock(fileToLock, channel));
            assertFalse(ReentrantThreadScopedFileLock.isLocked(fileToLock));
        }
    }

    @Test
    public void lockWillPropagateOtherExceptionsOnAcquire() throws IOException {
        final File fileToLock = IOTools.createTempFile("fileToLock");
        FileChannel closedChannel;
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            closedChannel = channel;
        }
        assertThrows(ClosedChannelException.class, () -> ReentrantThreadScopedFileLock.lock(fileToLock, closedChannel));
        assertFalse(ReentrantThreadScopedFileLock.isLocked(fileToLock));
    }

    @Test
    public void tryLockWillPropagateOtherExceptionsOnAcquire() throws IOException {
        final File fileToLock = IOTools.createTempFile("fileToLock");
        FileChannel closedChannel;
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            closedChannel = channel;
        }
        assertThrows(ClosedChannelException.class, () -> ReentrantThreadScopedFileLock.tryLock(fileToLock, closedChannel));
        assertFalse(ReentrantThreadScopedFileLock.isLocked(fileToLock));
    }

    @Test
    public void unlockWillLogAndUnlockProcessScopedLockOnChannelClosedException() throws IOException {
        expectException("Channel closed while unlocking");
        final File fileToLock = IOTools.createTempFile("fileToLock");
        final ReentrantThreadScopedFileLock rtsfl;
        try (FileChannel channel = FileChannel.open(fileToLock.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            rtsfl = ReentrantThreadScopedFileLock.lock(fileToLock, channel);
        }
        rtsfl.close();
        assertFalse(ReentrantThreadScopedFileLock.isLocked(fileToLock));
    }

    @Test
    public void providesMutualExclusionBetweenThreadsInAProcess_Lock() throws IOException, InterruptedException {
        providesMutualExclusionBetweenThreadsInAProcess(false);
    }

    @Test
    public void providesMutualExclusionBetweenThreadsInAProcess_TryLock() throws IOException, InterruptedException {
        providesMutualExclusionBetweenThreadsInAProcess(true);
    }

    @Test
    public void providesMutualExclusionBetweenProcesses_Lock() throws IOException {
        providesMutualExclusionBetweenProcesses(false);
    }

    @Test
    public void providesMutualExclusionBetweenProcesses_TryLock() throws IOException {
        providesMutualExclusionBetweenProcesses(true);
    }

    private void providesMutualExclusionBetweenThreadsInAProcess(boolean useTryLock) throws IOException, InterruptedException {
        final File fileToLock = IOTools.createTempFile("fileToLock");
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < NUM_THREADS; i++) {
                futures.add(executorService.submit(new LockerThread(fileToLock.getCanonicalPath(), i, useTryLock)));
            }
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (Exception e) {
                    fail(e);
                }
            });
        } finally {
            executorService.shutdown();
            assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private void providesMutualExclusionBetweenProcesses(boolean useTryLock) throws IOException {
        final File fileToLock = IOTools.createTempFile("fileToLock");
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
                    try (final ReentrantThreadScopedFileLock lock = acquireLock(fileToLock, channel)) {
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

        private ReentrantThreadScopedFileLock acquireLock(File file, FileChannel fileChannel) throws IOException {
            if (useTryLock) {
                return ReentrantThreadScopedFileLock.tryLock(file, fileChannel);
            } else {
                return ReentrantThreadScopedFileLock.lock(file, fileChannel);
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