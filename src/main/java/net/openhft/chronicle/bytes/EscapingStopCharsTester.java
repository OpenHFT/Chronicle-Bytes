/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

/**
 * Created by peter.lawrey on 16/01/15.
 */
public class EscapingStopCharsTester implements StopCharsTester {
    private final StopCharsTester sct;
    private boolean escaped = false;

    EscapingStopCharsTester(StopCharsTester sct) {
        this.sct = sct;
    }

    @Override
    public boolean isStopChar(int ch, int ch2) throws IllegalStateException {
        if (escaped) {
            escaped = false;
            return false;
        }
        if (ch == '\\') {
            escaped = true;
            return false;
        }
        return sct.isStopChar(ch, ch2);
    }

}
