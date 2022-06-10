/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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

public interface DistributedUniqueTimeDeduplicator {

    /**
     * Compare this new timestamp to the previously retained timstamp for the hostId
     *
     * @param timestampHostId to compare
     * @return -1 if the timestamp is older, 0 if the same, +1 if newer
     */
    int compareByHostId(long timestampHostId);

    /**
     * Compare this new timestamp to the previously retained timestamp for the hostId and retaining the timestamp
     *
     * @param timestampHostId to compare
     * @return -1 if the timestamp is older, 0 if the same, +1 if newer
     */
    int compareAndRetainNewer(long timestampHostId);
}
