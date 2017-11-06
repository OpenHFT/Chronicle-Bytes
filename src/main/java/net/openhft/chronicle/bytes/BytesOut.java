/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.bytes;

/*
 * Created by Peter Lawrey on 20/04/2016.
 */
public interface BytesOut<Underlying> extends
        StreamingDataOutput<Bytes<Underlying>>,
        ByteStringAppender<Bytes<Underlying>>,
        BytesPrepender<Bytes<Underlying>> {


    /**
     * Do these Bytes support saving comments
     *
     * @return true if comments are kept
     */
    default boolean retainsComments() {
        return false;
    }

    /**
     * Add comment as approriate for the toHexString format
     *
     * @param comment to add (or ignore)
     * @return this
     */
    default BytesOut<Underlying> comment(CharSequence comment) {
        return this;
    }

    /**
     * Adjust the indent for nested data
     *
     * @param n +1 indent in, -1 reduce indenting
     * @return this.
     */
    default BytesOut<Underlying> indent(int n) {
        return this;
    }
}
