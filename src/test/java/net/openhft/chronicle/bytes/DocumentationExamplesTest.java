/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class DocumentationExamplesTest extends BytesTestCommon {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void decimalExampleCompilesAndRendersBothSigns() throws Exception {
        String document = new String(Files.readAllBytes(Paths.get("src/main/docs/decimal-rendering.adoc")),
                StandardCharsets.UTF_8);
        Matcher block = Pattern.compile("(?ms)^\\[source,java]\\R----\\R(.*?)^----$").matcher(document);
        assertTrue("Missing executable decimal example", block.find());
        Path source = temporaryFolder.getRoot().toPath().resolve("DecimalRenderingExample.java");
        Files.write(source, block.group(1).getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("The documentation smoke test requires a JDK", compiler);
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        assertEquals("The Java block in decimal-rendering.adoc must compile", 0,
                compiler.run(null, null, null, "-proc:none", "-classpath", classpath, source.toString()));

        URL location = temporaryFolder.getRoot().toURI().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{location}, getClass().getClassLoader())) {
            Method render = loader.loadClass("DecimalRenderingExample").getMethod("render", double.class);
            assertEquals("12345e-4", render.invoke(null, 1.2345));
            assertEquals("-12345e-4", render.invoke(null, -1.2345));
        }
    }
}
