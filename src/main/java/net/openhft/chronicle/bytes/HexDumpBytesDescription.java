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

/**
 * This interface is for additional description to be added to HexDumpBytes
 */
public interface HexDumpBytesDescription<B extends HexDumpBytesDescription<B>> {
    /**
     * Do these Bytes support saving comments as descriptions for fields.
     *
     * @return true if comments are for field descriptions
     */
    default boolean retainedHexDumpDescription() {
        return false;
    }

    /**
     * Add comment as appropriate for the toHexString format
     *
     * @param comment to add (or ignore)
     * @return this
     */
    @SuppressWarnings("unchecked")
    default B writeHexDumpDescription(CharSequence comment)
            throws IllegalStateException {
        return (B) this;
    }

    /**
     * Adjust the indent for nested data
     *
     * @param n +1 indent in, -1 reduce indenting
     * @return this.
     */
    @SuppressWarnings("unchecked")
    default B adjustHexDumpIndentation(int n)
            throws IllegalStateException {
        return (B) this;
    }
}
