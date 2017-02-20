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