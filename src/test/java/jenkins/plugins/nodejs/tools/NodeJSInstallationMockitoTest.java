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
package jenkins.plugins.nodejs.tools;

import static jenkins.plugins.nodejs.NodeJSConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import hudson.EnvVars;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

public class NodeJSInstallationMockitoTest {

    /**
     * Ensure the use of {@link EnvVars#put(String, String)} instead
     * {@code EnvVars#override(String, String)} to respect the
     * {@link ToolInstallation#buildEnvVars(EnvVars)) API documentation.
     * <p>
     * A lot stuff rely on that logic.
     */
    @Issue("JENKINS-41947")
    @Test
    public void test_installer_environment() throws Exception {
        String nodeJSHome = "/home/nodejs";
        String bin = nodeJSHome + "/bin";

        NodeJSInstallation installer = spy(new NodeJSInstallation("test", nodeJSHome, null));

        EnvVars env = spy(new EnvVars());
        installer.buildEnvVars(env);
        verify(env, never()).override(anyString(), anyString());
        verify(env, never()).overrideAll(anyMap());

        assertEquals("Unexpected value for " + ENVVAR_NODEJS_HOME, nodeJSHome, env.get(ENVVAR_NODEJS_HOME));
        assertEquals("Unexpected value for " + ENVVAR_NODE_HOME, nodeJSHome, env.get(ENVVAR_NODE_HOME));
        assertEquals("Unexpected value for " + ENVVAR_NODEJS_PATH, bin, env.get(ENVVAR_NODEJS_PATH));
        assertNull("PATH variable should not appear in this environment", env.get("PATH"));
    }

}