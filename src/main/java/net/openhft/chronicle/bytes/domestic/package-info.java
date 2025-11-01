/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
/**
 * Utilities used internally by Chronicle Bytes.
 * <p>
 * The primary class is {@link net.openhft.chronicle.bytes.domestic.ReentrantFileLock},
 * which allows the same thread to acquire a {@link java.nio.channels.FileLock} multiple times
 * without triggering {@link java.nio.channels.OverlappingFileLockException}. Other threads may
 * still take overlapping locks.
 * <p>
 * This package is not considered a stable public API and binary compatibility is not
 * guaranteed between releases.
 * <p>
 * See the AsciiDoc module overview {@code domestic-overview.adoc} for examples and further notes.
 */
package net.openhft.chronicle.bytes.domestic;
