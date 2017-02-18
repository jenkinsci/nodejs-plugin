package jenkins.plugins.nodejs;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.TaskListener;
import jenkins.plugins.nodejs.CIBuilderHelper.Verifier;
import jenkins.plugins.nodejs.CIBuilderHelper;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMConfig.NPMConfigProvider;
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
                assertThat(env.keySet(), CoreMatchers.hasItems(NodeJSConstants.ENVVAR_NODEJS_PATH, NodeJSConstants.ENVVAR_NODEJS_HOME));
                assertEquals(getTestHome(), env.get(NodeJSConstants.ENVVAR_NODEJS_HOME));
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

    private Config createSetting(String id, String content, List<NPMRegistry> registries) {
        String providerId = new NPMConfigProvider().getProviderId();
        Config config = new NPMConfig(id, null, null, content, providerId, registries);

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