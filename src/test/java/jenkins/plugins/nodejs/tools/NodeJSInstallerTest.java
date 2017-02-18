package jenkins.plugins.nodejs.tools;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NodeJSInstaller.class)
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
    public void test_skip_install_global_packages_when_empty() throws Exception {
        String expectedPackages = " ";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);

        // mock all the static methods in the class
        PowerMockito.mockStatic(NodeJSInstaller.class);

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours);
        NodeJSInstaller spy = PowerMockito.spy(installer);

        // use Mockito to set up your expectation
        when(NodeJSInstaller.areNpmPackagesUpToDate(null, expectedPackages, expectedRefreshHours)).thenThrow(new AssertionError());
        PowerMockito.suppress(PowerMockito.methodsDeclaredIn(DownloadFromUrlInstaller.class));
        PowerMockito.doReturn(null).when(spy).getInstallable();
        PowerMockito.doReturn(Platform.LINUX).when(spy, "getPlatform", currentNode);
        PowerMockito.doReturn(CPU.amd64).when(spy, "getCPU", currentNode);
        when(spy.getNpmPackages()).thenReturn(expectedPackages);

        // execute test
        spy.performInstallation(mock(ToolInstallation.class), currentNode, mock(TaskListener.class));
    }

}