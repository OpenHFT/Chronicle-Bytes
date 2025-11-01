/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.perf;

public class ContentEqual8192JLBH {
    public static void main(String[] args) {
        ContentEqualJLBH.runWith(() -> ContentEqualJLBH.bytesFor(8192));
    }
}
