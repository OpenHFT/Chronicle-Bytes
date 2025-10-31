/*
 * Copyright 2016-2025 chronicle.software
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
