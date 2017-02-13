package jenkins.plugins.nodejs;

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
import jenkins.plugins.nodejs.tools.CPU;
import jenkins.plugins.nodejs.tools.NodeJSInstaller;
import jenkins.plugins.nodejs.tools.Platform;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NodeJSInstaller.class)
public class NodeJSInstallerTest {

    @Issue("JENKINS-41876")
    @Test
    public void testMethodThatCallsStaticMethod() throws Exception {
        String expectedPackages = " ";
        int expectedRefreshHours = 72;
        Node currentNode = mock(Node.class);

        // mock all the static methods in a class called "Static"
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