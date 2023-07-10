/**
 * The package contains classes related to handling bytes in a reentrant manner.
 * <p>
 * The primary class in this package is {@link net.openhft.chronicle.bytes.domestic.ReentrantFileLock}, 
 * a way of acquiring exclusive locks on files in a re-entrant fashion. This class ensures that 
 * a single thread that uses this interface to acquire locks won't cause an 
 * {@link java.nio.channels.OverlappingFileLockException}. Separate threads will not be 
 * prevented from taking overlapping file locks.
 * <p>
 * This package is a part of the open-source library Chronicle Bytes.
 * Please visit <a href="https://chronicle.software">chronicle.software</a> for more information.
 *
 * @see net.openhft.chronicle.bytes.domestic.ReentrantFileLock
 */
package net.openhft.chronicle.bytes.domestic;
