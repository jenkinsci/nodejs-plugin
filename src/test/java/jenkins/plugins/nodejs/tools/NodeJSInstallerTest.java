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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.DownloadFromUrlInstaller.Installable;

@RunWith(PowerMockRunner.class)
public class NodeJSInstallerTest {

    @Rule
    public TemporaryFolder fileRule = new TemporaryFolder();

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
    @PrepareForTest({ NodeJSInstaller.class, ToolsUtils.class, FilePath.class })
    public void test_skip_install_global_packages_when_empty() throws Exception {
        String expectedPackages = " ";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);
        when(currentNode.getRootPath()).thenReturn(new FilePath(fileRule.newFile()));

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours);
        NodeJSInstaller spy = PowerMockito.spy(installer);

        // use Mockito to set up your expectation
        PowerMockito.stub(PowerMockito.method(NodeJSInstaller.class, "areNpmPackagesUpToDate", FilePath.class, String.class, long.class)) //
                .toThrow(new AssertionError("global package should skip install if is an empty string"));
        PowerMockito.suppress(PowerMockito.method(FilePath.class, "installIfNecessaryFrom", URL.class, TaskListener.class, String.class));
        Installable installable = new Installable();
        installable.url = fileRule.newFile().toURI().toString();
        PowerMockito.doReturn(installable).when(spy).getInstallable();
        when(spy.getNpmPackages()).thenReturn(expectedPackages);

        PowerMockito.mockStatic(ToolsUtils.class);
        when(ToolsUtils.getCPU(currentNode)).thenReturn(CPU.amd64);
        when(ToolsUtils.getPlatform(currentNode)).thenReturn(Platform.LINUX);

        // execute test
        ToolInstallation toolInstallation = mock(ToolInstallation.class);
        when(toolInstallation.getHome()).thenReturn("nodejs");

        spy.performInstallation(toolInstallation, currentNode, mock(TaskListener.class));
    }

    @Issue("JENKINS-56895")
    @Test
    @PrepareForTest({ NodeJSInstaller.class, ToolsUtils.class, FilePath.class })
    public void verify_global_packages_are_refreshed_also_if_nodejs_installation_is_uptodate() throws Exception {
        String expectedPackages = "npm@6.7.0";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);
        when(currentNode.getRootPath()).thenReturn(new FilePath(fileRule.newFile()));

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours) {
            @Override
            public Installable getInstallable() throws IOException {
                Installable installable = new Installable();
                installable.url = fileRule.newFile().toURI().toString();
                return installable;
            }

            @Override
            protected boolean isUpToDate(FilePath expectedLocation, Installable i) throws IOException, InterruptedException {
                return true;
            }
        };
        NodeJSInstaller spy = PowerMockito.spy(installer);

        // use Mockito to set up your expectation
        Mockito.doNothing().when(spy).refreshGlobalPackages(Mockito.any(Node.class), Mockito.any(TaskListener.class), Mockito.any(FilePath.class));

        ToolInstallation toolInstallation = mock(ToolInstallation.class);
        when(toolInstallation.getHome()).thenReturn("nodejs");

        // execute test
        spy.performInstallation(toolInstallation, currentNode, mock(TaskListener.class));

        Mockito.verify(spy).refreshGlobalPackages(Mockito.any(Node.class), Mockito.any(TaskListener.class), Mockito.any(FilePath.class));
    }

}