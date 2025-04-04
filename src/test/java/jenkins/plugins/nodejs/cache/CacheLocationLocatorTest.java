/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.nodejs.cache;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

class CacheLocationLocatorTest {

    @TempDir
    private File fileRule;
    private FilePath workspace;

    @BeforeEach
    void setup() throws Exception {
        workspace = new FilePath(newFolder(fileRule, "junit"));
    }

    @Test
    void test_default() {
        assertNull(new DefaultCacheLocationLocator().locate(workspace), "expect null location path");
    }

    @Test
    void test_per_job() {
        assertEquals(workspace.child(".npm"), new PerJobCacheLocationLocator().locate(workspace), "expect the same location path passes as input");
    }

    @Test
    void test_per_executor() throws Exception {
        FilePath wc = mock(FilePath.class);
        Executor executor = mock(Executor.class);
        int executorNumber = 7;
        when(executor.getNumber()).thenReturn(executorNumber);

        try (MockedStatic<Executor> staticExecutor = mockStatic(Executor.class)) {
            staticExecutor.when(Executor::currentExecutor).thenReturn(executor);

            Computer computer = mock(Computer.class);
            Node node = mock(Node.class);
            when(computer.getNode()).thenReturn(node);
            FilePath rootPath = new FilePath(newFolder(fileRule, "junit"));
            when(node.getRootPath()).thenReturn(rootPath);
            when(wc.toComputer()).thenReturn(computer);

            FilePath expectedLocation = rootPath.child("npm-cache").child(String.valueOf(executorNumber));
            assertEquals(expectedLocation, new PerExecutorCacheLocationLocator().locate(wc), "expect null location path");
        }
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.exists() && !result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
