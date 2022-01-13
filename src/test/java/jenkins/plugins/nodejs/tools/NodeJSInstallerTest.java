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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AssertDelegateTarget;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.DownloadFromUrlInstaller.Installable;
import hudson.util.StreamTaskListener;
import io.jenkins.cli.shaded.org.apache.commons.io.output.NullPrintStream;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.*" })
public class NodeJSInstallerTest {

    private static class TarFileAssert implements AssertDelegateTarget {

        private File file;

        public TarFileAssert(File file) {
            this.file = file;
        }

        void hasEntry(String path) throws IOException {
            Assertions.assertThat(file).exists();
            try (TarArchiveInputStream zf = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                TarArchiveEntry entry;
                while ((entry = zf.getNextTarEntry()) != null) {
                    if (path.equals(entry.getName())) {
                        break;
                    }
                }
                Assertions.assertThat(entry).as("Entry " + path + " not found.").isNotNull();
            }
        }
    }

    @Rule
    public TemporaryFolder fileRule = new TemporaryFolder();

    @SuppressWarnings("deprecation")
    private TaskListener taskListener = new StreamTaskListener(new NullPrintStream());

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
        when(currentNode.getRootPath()).thenReturn(new FilePath(fileRule.newFolder()));

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours);
        NodeJSInstaller spy = PowerMockito.spy(installer);

        // use Mockito to set up your expectation
        PowerMockito.stub(PowerMockito.method(NodeJSInstaller.class, "getLocalCacheFile", Installable.class, Node.class)) //
            .toReturn(new File(fileRule.getRoot(), "test.tar.gz"));
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

        ToolInstallation toolInstallation = mock(ToolInstallation.class);
        when(toolInstallation.getHome()).thenReturn("nodejs");

        // execute test
        spy.performInstallation(toolInstallation, currentNode, taskListener);
    }

    @Test
    @PrepareForTest({ NodeJSInstaller.class, ToolsUtils.class, FilePath.class })
    public void verify_cache_is_build() throws Exception {
        String expectedPackages = " ";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);
        when(currentNode.getRootPath()).thenReturn(new FilePath(fileRule.newFolder()));

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours);
        NodeJSInstaller spy = PowerMockito.spy(installer);

        // use Mockito to set up your expectation
        File cache = new File(fileRule.getRoot(), "test.tar.gz");
        PowerMockito.stub(PowerMockito.method(NodeJSInstaller.class, "getLocalCacheFile", Installable.class, Node.class)) //
                .toReturn(cache);
        PowerMockito.stub(PowerMockito.method(NodeJSInstaller.class, "areNpmPackagesUpToDate", FilePath.class, String.class, long.class)) //
                .toThrow(new AssertionError("global package should skip install if is an empty string"));
        Installable installable = new Installable();
        File downloadURL = fileRule.newFile("nodejs.tar.gz");
        fillArchive(downloadURL, "nodejs/bin/npm.sh", "echo \"hello\"".getBytes());
        installable.url = downloadURL.toURI().toString();
        PowerMockito.doReturn(installable).when(spy).getInstallable();
        when(spy.getNpmPackages()).thenReturn(expectedPackages);

        PowerMockito.mockStatic(ToolsUtils.class);
        when(ToolsUtils.getCPU(currentNode)).thenReturn(CPU.amd64);
        when(ToolsUtils.getPlatform(currentNode)).thenReturn(Platform.LINUX);

        ToolInstallation toolInstallation = mock(ToolInstallation.class);
        when(toolInstallation.getHome()).thenReturn("nodejs");

        // execute test
        spy.performInstallation(toolInstallation, currentNode, taskListener);
        Assertions.assertThat(new TarFileAssert(cache)).hasEntry("bin/npm.sh");
    }

    @Test
    @PrepareForTest({ NodeJSInstaller.class, ToolsUtils.class, FilePath.class })
    public void test_cache_archive_is_used() throws Exception {
        String expectedPackages = " ";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);
        when(currentNode.getRootPath()).thenReturn(new FilePath(fileRule.newFolder()));

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours);
        NodeJSInstaller spy = PowerMockito.spy(installer);

        // use Mockito to set up your expectation
        File cache = fileRule.newFile();
        fillArchive(cache, "nodejs.txt", "test".getBytes());
        PowerMockito.stub(PowerMockito.method(NodeJSInstaller.class, "getLocalCacheFile", Installable.class, Node.class)) //
                .toReturn(cache);

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

        ToolInstallation toolInstallation = mock(ToolInstallation.class);
        when(toolInstallation.getHome()).thenReturn("nodejs");

        // execute test
        FilePath expected = spy.performInstallation(toolInstallation, currentNode, taskListener);
        Assertions.assertThat(expected.list("nodejs.txt")).isNotEmpty();
    }

    private void fillArchive(File file, String fileEntry, byte[] content) throws IOException {
        try (TarArchiveOutputStream zf = new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(file)))) {
            TarArchiveEntry ze = new TarArchiveEntry(fileEntry);
            ze.setSize(content.length);
            zf.putArchiveEntry(ze);
            IOUtils.write(content, zf);
            zf.closeArchiveEntry();
        }
    }

    @Issue("JENKINS-56895")
    @Test
    @PrepareForTest({ NodeJSInstaller.class, ToolsUtils.class, FilePath.class })
    public void verify_global_packages_are_refreshed_also_if_nodejs_installation_is_uptodate() throws Exception {
        String expectedPackages = "npm@6.7.0";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);
        when(currentNode.getRootPath()).thenReturn(new FilePath(fileRule.newFolder()));

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
        spy.performInstallation(toolInstallation, currentNode, taskListener);

        Mockito.verify(spy).refreshGlobalPackages(Mockito.any(Node.class), Mockito.any(TaskListener.class), Mockito.any(FilePath.class));
    }

}