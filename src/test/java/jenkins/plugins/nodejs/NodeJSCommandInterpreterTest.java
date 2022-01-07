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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import jenkins.plugins.nodejs.CIBuilderHelper.Verifier;
import jenkins.plugins.nodejs.VerifyEnvVariableBuilder.EnvVarVerifier;
import jenkins.plugins.nodejs.cache.DefaultCacheLocationLocator;
import jenkins.plugins.nodejs.cache.PerJobCacheLocationLocator;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.tools.DetectionFailedException;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.Platform;

public class NodeJSCommandInterpreterTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Issue("JENKINS-41947")
    @Test
    public void test_inject_path_variable() throws Exception {
        NodeJSInstallation installation = mockInstaller();
        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_executable_value", installation, null, new CIBuilderHelper.Verifier() {
            @Override
            public void verify(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws Exception {
                assertFalse("No Environments", build.getEnvironments().isEmpty());

                EnvVars env = build.getEnvironment(listener);
                Assertions.assertThat(env.keySet()).contains(NodeJSConstants.ENVVAR_NODEJS_PATH, NodeJSConstants.ENVVAR_NODEJS_HOME);
                assertEquals(getTestHome(), env.get(NodeJSConstants.ENVVAR_NODEJS_HOME));
                assertEquals(getTestHome(), env.get(NodeJSConstants.ENVVAR_NODE_HOME));
                assertEquals(getTestBin(), env.get(NodeJSConstants.ENVVAR_NODEJS_PATH));
            }
        });

        FreeStyleProject job = j.createFreeStyleProject();
        job.getBuildersList().add(builder);
        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Test
    public void test_executable_value() throws Exception {
        NodeJSInstallation installation = mockInstaller();
        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_executable_value", installation, null);

        FreeStyleProject job = j.createFreeStyleProject();
        job.getBuildersList().add(builder);
        j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        String[] buildCommandLine = builder.buildCommandLine(new FilePath(folder.newFile()));
        assertEquals(buildCommandLine[0], getTestExecutable());
    }

    @Test
    public void test_creation_of_config() throws Exception {
        Config config = createSetting("my-config-id", "email=foo@acme.com", null);

        NodeJSInstallation installation = mockInstaller();
        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_creation_of_config", installation, config.id, new Verifier() {
            @Override
            public void verify(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws Exception {
                EnvVars env = build.getEnvironment(listener);

                String var = NodeJSConstants.NPM_USERCONFIG;
                String value = env.get(var);

                assertTrue("variable " + var + " not set", env.containsKey(var));
                assertNotNull("empty value for environment variable " + var, value);
                assertTrue("file of variable " + var + " does not exists or is not a file", new File(value).isFile());
            }
        });

        FreeStyleProject job = j.createFreeStyleProject();
        job.getBuildersList().add(builder);
        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Test
    public void test_calls_sequence_of_installer() throws Exception {
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
    public void test_check_no_executable_in_installation_folder() throws Exception {
        NodeJSInstallation installation = mockInstaller();
        when(installation.getExecutable(any(Launcher.class))).thenReturn(null);

        NodeJSCommandInterpreter builder = CIBuilderHelper.createMock("test_creation_of_config", installation, null);

        FreeStyleProject job = j.createFreeStyleProject("free");
        job.getBuildersList().add(builder);
        j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
    }

    /**
     * Verify that the serialisation is backward compatible.
     */
    @LocalData
    @Test
    @Issue("JENKINS-57844")
    public void test_serialisation_is_compatible_with_version_1_2_x() throws Exception {
        FreeStyleProject prj = j.jenkins.getAllItems(hudson.model.FreeStyleProject.class) //
                .stream() //
                .filter(p -> "test".equals(p.getName())) //
                .findFirst().get();

        NodeJSCommandInterpreter step = prj.getBuildersList().get(NodeJSCommandInterpreter.class);
        Assertions.assertThat(step.getCacheLocationStrategy()).isInstanceOf(DefaultCacheLocationLocator.class);
    }

    /**
     * Verify reloading jenkins job configuration use the saved cache strategy instead reset to default.
     */
    @LocalData
    @Test
    @Issue("JENKINS-58029")
    public void test_reloading_job_configuration_contains_saved_cache_strategy() throws Exception {
        FreeStyleProject prj = j.jenkins.getAllItems(hudson.model.FreeStyleProject.class) //
                .stream() //
                .filter(p -> "test".equals(p.getName())) //
                .findFirst().get();

        NodeJSCommandInterpreter step = prj.getBuildersList().get(NodeJSCommandInterpreter.class);
        Assertions.assertThat(step.getCacheLocationStrategy()).isInstanceOf(PerJobCacheLocationLocator.class);
    }

    @Test
    public void test_set_of_cache_location() throws Exception {
        final File cacheFolder = folder.newFolder();

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
}