/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
