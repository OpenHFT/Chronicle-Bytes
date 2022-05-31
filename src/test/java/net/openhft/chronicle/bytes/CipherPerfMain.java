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

import java.security.Provider;
import java.security.Security;
import java.util.Map;

public class CipherPerfMain {
    public static void main(String[] args) {
        for (Provider providers : Security.getProviders()) {
            for (Map.Entry<Object, Object> entry : providers.entrySet()) {
                if (entry.getKey().toString().startsWith("Cipher."))
                    System.out.println(entry);
            }
        }
    }
}
