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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullPrintStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;
import org.assertj.core.api.AssertDelegateTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.mockito.MockedStatic;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.DownloadFromUrlInstaller.Installable;
import hudson.util.StreamTaskListener;

class NodeJSInstallerTest {

    private static class TarFileAssert implements AssertDelegateTarget {

        private final File file;

        public TarFileAssert(File file) {
            this.file = file;
        }

        void hasEntry(String path) throws IOException {
            assertThat(file).exists();
            try (TarInputStream zf = new TarInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                TarEntry entry;
                while ((entry = zf.getNextEntry()) != null) {
                    if (path.equals(entry.getName())) {
                        break;
                    }
                }
                assertThat(entry).as("Entry " + path + " not found.").isNotNull();
            }
        }
    }

    @TempDir
    private File fileRule;

    @SuppressWarnings("deprecation")
    private final TaskListener taskListener = new StreamTaskListener(new NullPrintStream());

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
    void test_skip_install_global_packages_when_empty() throws Exception {
        File cache = new File(fileRule, "test.tar.gz");
        IOUtils.copy(getClass().getResource("test.tar.gz"), cache);
        String expectedPackages = " ";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);
        when(currentNode.getRootPath()).thenReturn(new FilePath(newFolder(fileRule, "junit")));

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours);
        NodeJSInstaller spy = spy(installer);

        // use Mockito to set up your expectation
        doReturn(cache).when(spy).getLocalCacheFile(any(), any());
        Installable installable = new Installable();
        installable.url = File.createTempFile("junit", null, fileRule).toURI().toString();
        doReturn(installable).when(spy).getInstallable();
        when(spy.getNpmPackages()).thenReturn(expectedPackages);

        try (MockedStatic<ToolsUtils> staticToolsUtils = mockStatic(ToolsUtils.class)) {
            staticToolsUtils.when(() -> ToolsUtils.getCPU(currentNode)).thenReturn(CPU.amd64);
            staticToolsUtils.when(() -> ToolsUtils.getPlatform(currentNode)).thenReturn(Platform.LINUX);

            ToolInstallation toolInstallation = mock(ToolInstallation.class);
            when(toolInstallation.getHome()).thenReturn("nodejs");

            // execute test
            spy.performInstallation(toolInstallation, currentNode, taskListener);
        }
    }

    @Test
    void verify_cache_is_build() throws Exception {
        File cache = new File(fileRule, "test.tar.gz");
        IOUtils.copy(getClass().getResource("test.tar.gz"), cache);
        String expectedPackages = " ";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);
        when(currentNode.getRootPath()).thenReturn(new FilePath(newFolder(fileRule, "junit")));

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours);
        NodeJSInstaller spy = spy(installer);

        // use Mockito to set up your expectation
        doReturn(cache).when(spy).getLocalCacheFile(any(), any());
        Installable installable = new Installable();
        File downloadURL = File.createTempFile("nodejs.tar.gz", null, fileRule);
        fillArchive(downloadURL, "nodejs/bin/npm.sh", "echo \"hello\"".getBytes());
        installable.url = downloadURL.toURI().toString();
        doReturn(installable).when(spy).getInstallable();
        when(spy.getNpmPackages()).thenReturn(expectedPackages);

        try (MockedStatic<ToolsUtils> staticToolsUtils = mockStatic(ToolsUtils.class)) {
            staticToolsUtils.when(() -> ToolsUtils.getCPU(currentNode)).thenReturn(CPU.amd64);
            staticToolsUtils.when(() -> ToolsUtils.getPlatform(currentNode)).thenReturn(Platform.LINUX);

            ToolInstallation toolInstallation = mock(ToolInstallation.class);
            when(toolInstallation.getHome()).thenReturn("nodejs");

            // execute test
            spy.performInstallation(toolInstallation, currentNode, taskListener);
            assertThat(new TarFileAssert(cache)).hasEntry("bin/npm.sh");
        }
    }

    @Test
    void test_cache_archive_is_used() throws Exception {
        String expectedPackages = " ";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);
        when(currentNode.getRootPath()).thenReturn(new FilePath(newFolder(fileRule, "junit")));

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours);
        NodeJSInstaller spy = spy(installer);

        // use Mockito to set up your expectation
        File cache = File.createTempFile("junit", null, fileRule);
        fillArchive(cache, "nodejs.txt", "test".getBytes());
        doReturn(cache).when(spy).getLocalCacheFile(any(), any());
        Installable installable = new Installable();
        installable.url = File.createTempFile("junit", null, fileRule).toURI().toString();
        doReturn(installable).when(spy).getInstallable();
        when(spy.getNpmPackages()).thenReturn(expectedPackages);

        try (MockedStatic<ToolsUtils> staticToolsUtils = mockStatic(ToolsUtils.class)) {
            staticToolsUtils.when(() -> ToolsUtils.getCPU(currentNode)).thenReturn(CPU.amd64);
            staticToolsUtils.when(() -> ToolsUtils.getPlatform(currentNode)).thenReturn(Platform.LINUX);

            ToolInstallation toolInstallation = mock(ToolInstallation.class);
            when(toolInstallation.getHome()).thenReturn("nodejs");

            // execute test
            FilePath expected = spy.performInstallation(toolInstallation, currentNode, taskListener);
            assertThat(expected.list("nodejs.txt")).isNotEmpty();
        }
    }

    private void fillArchive(File file, String fileEntry, byte[] content) throws IOException {
        try (TarOutputStream zf = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
            TarEntry ze = new TarEntry(fileEntry);
            ze.setSize(content.length);
            zf.putNextEntry(ze);
            IOUtils.write(content, zf);
            zf.closeEntry();
        }
    }

    @Issue("JENKINS-56895")
    @Test
    void verify_global_packages_are_refreshed_also_if_nodejs_installation_is_uptodate() throws Exception {
        String expectedPackages = "npm@6.7.0";
        int expectedRefreshHours = NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS;
        Node currentNode = mock(Node.class);
        when(currentNode.getRootPath()).thenReturn(new FilePath(newFolder(fileRule, "junit")));

        // create partial mock
        NodeJSInstaller installer = new NodeJSInstaller("test-id", expectedPackages, expectedRefreshHours) {
            @Override
            public Installable getInstallable() throws IOException {
                Installable installable = new Installable();
                installable.url = File.createTempFile("junit", null, fileRule).toURI().toString();
                return installable;
            }

            @Override
            protected boolean isUpToDate(FilePath expectedLocation, Installable i) {
                return true;
            }
        };
        NodeJSInstaller spy = spy(installer);

        // use Mockito to set up your expectation
        doNothing().when(spy).refreshGlobalPackages(any(Node.class), any(TaskListener.class), any(FilePath.class));

        ToolInstallation toolInstallation = mock(ToolInstallation.class);
        when(toolInstallation.getHome()).thenReturn("nodejs");

        // execute test
        spy.performInstallation(toolInstallation, currentNode, taskListener);

        verify(spy).refreshGlobalPackages(any(Node.class), any(TaskListener.class), any(FilePath.class));
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }

}