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
import jenkins.plugins.nodejs.cache.DefaultCacheLocationLocator;
import jenkins.plugins.nodejs.cache.PerExecutorCacheLocationLocator;
import jenkins.plugins.nodejs.cache.PerJobCacheLocationLocator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FilePath.class, Executor.class })
public class CacheLocationLocatorTest {

    @Rule
    public TemporaryFolder fileRule = new TemporaryFolder();
    private FilePath workspace;

    @Before
    public void setup() throws Exception {
        workspace = new FilePath(fileRule.newFolder());
    }

    @Test
    public void test_default() {
        Assert.assertNull("expect null location path", new DefaultCacheLocationLocator().locate(workspace));
    }

    @Test
    public void test_per_job() throws Exception {
        Assert.assertEquals("expect the same location path passes as input", workspace.child(".npm"), new PerJobCacheLocationLocator().locate(workspace));
    }

    @Test
    public void test_per_executor() throws Exception {
        FilePath wc = PowerMockito.mock(FilePath.class);
        PowerMockito.mockStatic(Executor.class);
        Executor executor = mock(Executor.class);
        int executorNumber = 7;
        when(executor.getNumber()).thenReturn(executorNumber);
        PowerMockito.when(Executor.currentExecutor()).thenReturn(executor);

        Computer computer = mock(Computer.class);
        Node node = PowerMockito.mock(Node.class);
        when(computer.getNode()).thenReturn(node);
        FilePath rootPath = new FilePath(fileRule.newFolder());
        when(node.getRootPath()).thenReturn(rootPath);
        PowerMockito.when(wc.toComputer()).thenReturn(computer);

        FilePath expectedLocation = rootPath.child("npm-cache").child(String.valueOf(executorNumber));
        Assert.assertEquals("expect null location path", expectedLocation, new PerExecutorCacheLocationLocator().locate(wc));
    }
}
