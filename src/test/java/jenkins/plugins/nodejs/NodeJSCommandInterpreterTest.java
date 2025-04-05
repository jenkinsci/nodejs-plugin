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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
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
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import jenkins.plugins.nodejs.VerifyEnvVariableBuilder.EnvVarVerifier;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.tools.DetectionFailedException;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.Platform;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NodeJSCommandInterpreterTest {

    private static JenkinsRule j;
    @TempDir
    private File folder;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Issue("JENKINS-41947")
    @Test
    void test_inject_path_variable() throws Exception {
        NodeJSInstallation installation = mockInstaller();
        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_executable_value", installation, null, (build, launcher, listener) -> {
            assertFalse(build.getEnvironments().isEmpty(), "No Environments");

            EnvVars env = build.getEnvironment(listener);
            assertThat(env).containsKeys(NodeJSConstants.ENVVAR_NODEJS_PATH, NodeJSConstants.ENVVAR_NODEJS_HOME);
            assertEquals(getTestHome(), env.get(NodeJSConstants.ENVVAR_NODEJS_HOME));
            assertEquals(getTestHome(), env.get(NodeJSConstants.ENVVAR_NODE_HOME));
            assertEquals(getTestBin(), env.get(NodeJSConstants.ENVVAR_NODEJS_PATH));
        });

        FreeStyleProject job = j.createFreeStyleProject();
        job.getBuildersList().add(builder);
        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Test
    void test_executable_value() throws Exception {
        NodeJSInstallation installation = mockInstaller();
        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_executable_value", installation, null);

        FreeStyleProject job = j.createFreeStyleProject();
        job.getBuildersList().add(builder);
        j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        String[] buildCommandLine = builder.buildCommandLine(new FilePath(File.createTempFile("junit", null, folder)));
        assertEquals(buildCommandLine[0], getTestExecutable());
    }

    @Test
    void test_creation_of_config() throws Exception {
        Config config = createSetting("my-config-id", "email=foo@acme.com", null);

        NodeJSInstallation installation = mockInstaller();
        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_creation_of_config", installation, config.id, (build, launcher, listener) -> {
            EnvVars env = build.getEnvironment(listener);

            String var = NodeJSConstants.NPM_USERCONFIG;
            String value = env.get(var);

            assertTrue(env.containsKey(var), "variable " + var + " not set");
            assertNotNull(value, "empty value for environment variable " + var);
            assertTrue(new File(value).isFile(), "file of variable " + var + " does not exists or is not a file");
        });

        FreeStyleProject job = j.createFreeStyleProject();
        job.getBuildersList().add(builder);
        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Test
    void test_calls_sequence_of_installer() throws Exception {
        NodeJSInstallation installation = mockInstaller();
        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_creation_of_config", installation, null);

        FreeStyleProject job = j.createFreeStyleProject("free");
        job.getBuildersList().add(builder);
        j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(installation).forNode(any(Node.class), any(TaskListener.class));
        verify(installation).forEnvironment(any(EnvVars.class));
        verify(installation).buildEnvVars(any(EnvVars.class));
    }

    @Issue("JENKINS-45840")
    @Test
    void test_check_no_executable_in_installation_folder() throws Exception {
        NodeJSInstallation installation = mockInstaller();
        when(installation.getExecutable(any(Launcher.class))).thenReturn(null);

        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_creation_of_config", installation, null);

        FreeStyleProject job = j.createFreeStyleProject("JENKINS-45840");
        job.getBuildersList().add(builder);
        j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
    }

    @Test
    void test_set_of_cache_location() throws Exception {
        final File cacheFolder = newFolder(folder, "junit");

        NodeJSInstallation installation = mockInstaller();
        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_creation_of_config", installation);
        builder.setCacheLocationStrategy(new TestCacheLocationLocator(cacheFolder));

        FreeStyleProject job = j.createFreeStyleProject("cache");
        job.getBuildersList().add(builder);
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

    private NodeJSInstallation mockInstaller() throws IOException, InterruptedException {
        NodeJSInstallation mock = mock(NodeJSInstallation.class);
        when(mock.forNode(any(Node.class), any(TaskListener.class))).then(RETURNS_SELF);
        when(mock.forEnvironment(any(EnvVars.class))).then(RETURNS_SELF);
        when(mock.getName()).thenReturn("mockNode");
        when(mock.getHome()).thenReturn(getTestHome());
        when(mock.getExecutable(any(Launcher.class))).thenReturn(getTestExecutable());
        doCallRealMethod().when(mock).buildEnvVars(any(EnvVars.class));
        return mock;
    }

    private String getTestExecutable() throws DetectionFailedException {
        return new File(getTestBin(), Platform.current().nodeFileName).getAbsolutePath();
    }

    private String getTestBin() throws DetectionFailedException {
        return new File(getTestHome(), Platform.current().binFolder).getAbsolutePath();
    }

    private String getTestHome() {
        return new File("/home", "nodejs").getAbsolutePath();
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