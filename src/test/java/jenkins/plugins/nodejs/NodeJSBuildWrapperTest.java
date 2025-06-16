/*
 * The MIT License
 *
 * Copyright (c) 2019, Nikolas Falco
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.EnvVars;
import hudson.ExtensionList;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import jenkins.plugins.nodejs.VerifyEnvVariableBuilder.EnvVarVerifier;
import jenkins.plugins.nodejs.VerifyEnvVariableBuilder.FileVerifier;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.Platform;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NodeJSBuildWrapperTest {

    private static JenkinsRule j;
    @TempDir
    private File fileRule;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void test_calls_sequence_of_installer() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free");

        NodeJSInstallation installation = mockInstaller();
        NodeJSBuildWrapper bw = mockWrapper(installation, mock(NPMConfig.class));

        job.getBuildWrappersList().add(bw);

        j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(installation).forNode(any(Node.class), any(TaskListener.class));
        verify(installation).forEnvironment(any(EnvVars.class));
        verify(installation).buildEnvVars(any(EnvVars.class));
    }

    @Test
    void test_creation_of_config() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free2");

        final Config config = createSetting("my-config-id", "email=foo@acme.com", null);

        NodeJSInstallation installation = mockInstaller();
        NodeJSBuildWrapper bw = mockWrapper(installation, config);

        job.getBuildWrappersList().add(bw);

        job.getBuildersList().add(new FileVerifier());

        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Test
    void test_inject_path_variable() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free3");

        final Config config = createSetting("my-config-id", "", null);

        NodeJSInstallation installation = spy(new NodeJSInstallation("test", getTestHome(), null));
        doReturn(getTestExecutable()).when(installation).getExecutable(any(Launcher.class));
        doReturn(installation).when(installation).forNode(any(Node.class), any(TaskListener.class));
        doReturn(installation).when(installation).forEnvironment(any(EnvVars.class));

        NodeJSBuildWrapper spy = mockWrapper(installation, config);

        job.getBuildWrappersList().add(spy);

        job.getBuildersList().add(new PathVerifier(installation));

        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Issue("JENKINS-45840")
    @Test
    void test_check_no_executable_in_installation_folder() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free4");

        NodeJSInstallation installation = mockInstaller();
        when(installation.getExecutable(any(Launcher.class))).thenReturn(null);
        NodeJSBuildWrapper bw = mockWrapper(installation, mock(NPMConfig.class));

        job.getBuildWrappersList().add(bw);

        j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
    }

    @Test
    void test_set_of_cache_location() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("cache");

        final File cacheFolder = newFolder(fileRule, "junit");

        NodeJSBuildWrapper bw = mockWrapper(mockInstaller());
        bw.setCacheLocationStrategy(new TestCacheLocationLocator(cacheFolder));
        job.getBuildWrappersList().add(bw);
        job.getBuildersList().add(new EnvVarVerifier(NodeJSConstants.NPM_CACHE_LOCATION, cacheFolder.getAbsolutePath()));

        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    private Config createSetting(String id, String content, List<NPMRegistry> registries) {
        Config config = new NPMConfig(id, null, null, content, registries);

        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class)
                .get(GlobalConfigFiles.class);
        globalConfigFiles.save(config);
        return config;
    }

    private NodeJSBuildWrapper mockWrapper(NodeJSInstallation installation, Config config) {
        NodeJSBuildWrapper wrapper;
        if (config != null) {
            wrapper = new NodeJSBuildWrapper(installation.getName(), config.id);
        } else {
            wrapper = new NodeJSBuildWrapper(installation.getName());
        }
        ExtensionList.lookupSingleton(NodeJSInstallation.DescriptorImpl.class).setInstallations(installation);
        return wrapper;
    }

    private NodeJSBuildWrapper mockWrapper(NodeJSInstallation installation) {
        return mockWrapper(installation, null);
    }

    private NodeJSInstallation mockInstaller() throws Exception {
        NodeJSInstallation mock = mock(NodeJSInstallation.class);
        when(mock.forNode(any(Node.class), any(TaskListener.class))).then(RETURNS_SELF);
        when(mock.forEnvironment(any(EnvVars.class))).then(RETURNS_SELF);
        when(mock.getName()).thenReturn("mockNode");
        when(mock.getHome()).thenReturn(getTestHome());
        when(mock.getExecutable(any(Launcher.class))).thenReturn(getTestExecutable());
        return mock;
    }

    private String getTestExecutable() throws Exception {
        Platform currentPlatform = Platform.current();
        return new File(new File(getTestHome(), currentPlatform.binFolder), currentPlatform.nodeFileName).getAbsolutePath();
    }

    private String getTestHome() {
        return new File("/home", "nodejs").getAbsolutePath();
    }

    private static final class PathVerifier extends VerifyEnvVariableBuilder {
        private final NodeJSInstallation installation;

        private PathVerifier(NodeJSInstallation installation) {
            this.installation = installation;
        }

        @Override
        public void verify(EnvVars env) {
            String expectedValue = installation.getHome();
            assertEquals(expectedValue, env.get(NodeJSConstants.ENVVAR_NODEJS_HOME), "Unexpected value for " + NodeJSConstants.ENVVAR_NODEJS_HOME);
            assertThat(env.get("PATH")).contains(expectedValue);
            // check that PATH is not exact the NodeJS home otherwise means PATH was overridden
            assertThat(env.get("PATH")).isNotEqualTo(expectedValue); // JENKINS-41947
        }
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