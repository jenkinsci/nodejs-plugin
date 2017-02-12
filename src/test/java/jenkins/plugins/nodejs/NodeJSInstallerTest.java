package jenkins.plugins.nodejs;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import jenkins.plugins.nodejs.tools.NodeJSInstaller;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NodeJSInstaller.class)
public class NodeJSInstallerTest {

    @Test
    public void test_skip_install_global_packages_when_empty() throws Exception {
        MockNodeJSInstaller mock = new MockNodeJSInstaller("test-id", " ", NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS);
        mock.performInstallation(null, null, null);
    }

    private class MockNodeJSInstaller extends NodeJSInstaller {

        public MockNodeJSInstaller(String id, String npmPackages, long npmPackagesRefreshHours) {
            super(id, npmPackages, npmPackagesRefreshHours);
        }

        @Override
        public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
            FilePath expected = new FilePath(new File("/home/tools"));
            refreshGlobalPackages(node, log, expected);
            return expected;
        }
    }

}