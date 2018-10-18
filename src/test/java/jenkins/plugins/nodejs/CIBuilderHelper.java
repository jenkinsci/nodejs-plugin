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
package jenkins.plugins.nodejs;

import static org.mockito.Mockito.*;

import org.powermock.api.mockito.PowerMockito;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;

/* package */ final class CIBuilderHelper {

    public static interface Verifier {
        void verify(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws Exception;
    }

    public static NodeJSCommandInterpreter createMock(String command, NodeJSInstallation installation, String configId) {
        return createMock(command, installation, configId, null);
    }

    public static NodeJSCommandInterpreter createMock(String command, NodeJSInstallation installation, String configId,
            Verifier verifier) {
        MockCommandInterpreterBuilder spy = PowerMockito.spy(new MockCommandInterpreterBuilder(command, installation.getName(), configId));
        doReturn(installation).when(spy).getNodeJS();
        doReturn(new NodeJSCommandInterpreter.NodeJsDescriptor()).when(spy).getDescriptor();
        spy.setVerifier(verifier);
        return spy;
    }

    static class MockCommandInterpreterBuilder extends NodeJSCommandInterpreter {

        // transient to ensure serialisation
        private transient CIBuilderHelper.Verifier verifier;

        private MockCommandInterpreterBuilder(String command, String nodeJSInstallationName, String configId) {
            super(command, nodeJSInstallationName, configId);
        }

        @Override
        protected boolean internalPerform(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws InterruptedException {
            if (verifier != null) {
                try {
                    verifier.verify(build, launcher, listener);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        }

        private void setVerifier(Verifier verifier) {
            this.verifier = verifier;
        }
    }
}