/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.ReferenceOwner;

import java.io.File;
import java.io.FileNotFoundException;

public class MixedAccessMain {
    public static void main(String[] args) throws FileNotFoundException {
        System.setProperty("jvm.resource.tracing", "true");
        File file = new File(OS.TMP + "/map-test");
        try (MappedBytes mf = MappedBytes.mappedBytes(file,
                1024 * 1024, OS.pageSize())) {
            mf.writeLong(0);
            BytesStore start = mf.bytesStore();
            start.reserve(ReferenceOwner.TMP);

            for (int i = 0; i < 1_000_000; i++) {
                mf.writeLong(i);
                start.writeLong(0, i);
            }
            start.release(ReferenceOwner.TMP);
        }
    }
}
