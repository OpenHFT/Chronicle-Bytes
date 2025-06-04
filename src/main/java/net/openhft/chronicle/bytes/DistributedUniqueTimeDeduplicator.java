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
package net.openhft.chronicle.bytes;

/**
 * Detects and optionally retains the newest timestamp for each host id so duplicates can be filtered.
 */
public interface DistributedUniqueTimeDeduplicator {

    /**
     * Compares {@code timestampHostId} with the last timestamp held for its host id.
     *
     * @param timestampHostId value embedding time and host id
     * @return -1 if older, 0 if equal or no previous value, +1 if newer
     */
    int compareByHostId(long timestampHostId);

    /**
     * As {@link #compareByHostId(long)} but also retains {@code timestampHostId} if it is newer.
     */
    int compareAndRetainNewer(long timestampHostId);
}
