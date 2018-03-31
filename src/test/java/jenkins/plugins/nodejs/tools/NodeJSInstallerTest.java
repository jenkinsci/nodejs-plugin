package jenkins.plugins.nodejs.tools;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;

@RunWith(PowerMockRunner.class)
public class NodeJSInstallerTest {

    /**
     * Verify that the installer skip install of global package also if
     * npmPackage is an empty/spaces string.
     * <p>
     * This could happen because after migration 0.2 -> 1.0 the persistence
     * could have npmPackages value empty or with spaces. XStream
     * de-serialisation does use constructs object using constructor so value
     * can be safely set to null.
     */
    @Issue("JENKINS-41876")
    @Test
    @PrepareForTest({ NodeJSInstaller.class, ToolsUtils.class })
    public void test_skip_install_global_packages_when_empty() throws Exception {
        String expectedPackages = " ";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours);
        NodeJSInstaller spy = PowerMockito.spy(installer);

        // use Mockito to set up your expectation
        PowerMockito.stub(PowerMockito.method(NodeJSInstaller.class, "areNpmPackagesUpToDate", FilePath.class, String.class, long.class)) //
            .toThrow(new AssertionError("global package should skip install if is an empty string"));
        PowerMockito.suppress(PowerMockito.methodsDeclaredIn(DownloadFromUrlInstaller.class));
        PowerMockito.doReturn(null).when(spy).getInstallable();
        when(spy.getNpmPackages()).thenReturn(expectedPackages);

        PowerMockito.mockStatic(ToolsUtils.class);
        when(ToolsUtils.getCPU(currentNode)).thenReturn(CPU.amd64);
        when(ToolsUtils.getPlatform(currentNode)).thenReturn(Platform.LINUX);

        // execute test
        spy.performInstallation(mock(ToolInstallation.class), currentNode, mock(TaskListener.class));
    }

}