package jenkins.plugins.nodejs;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.powermock.api.mockito.PowerMockito;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import jenkins.plugins.nodejs.VerifyEnvVariableBuilder.FileVerifier;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMConfig.NPMConfigProvider;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.Platform;

public class NodeJSBuildWrapperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test_calls_sequence_of_installer() throws Exception {
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
    public void test_creation_of_config() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free");

        final Config config = createSetting("my-config-id", "email=foo@acme.com", null);

        NodeJSInstallation installation = mockInstaller();
        NodeJSBuildWrapper bw = mockWrapper(installation, config);

        job.getBuildWrappersList().add(bw);

        job.getBuildersList().add(new FileVerifier());

        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Test
    public void test_inject_path_variable() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free");

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
    public void test_check_no_executable_in_installation_folder() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free");

        NodeJSInstallation installation = mockInstaller();
        when(installation.getExecutable(any(Launcher.class))).thenReturn(null);
        NodeJSBuildWrapper bw = mockWrapper(installation, mock(NPMConfig.class));

        job.getBuildWrappersList().add(bw);

        j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
    }

    private Config createSetting(String id, String content, List<NPMRegistry> registries) {
        String providerId = new NPMConfigProvider().getProviderId();
        Config config = new NPMConfig(id, null, null, content, providerId, registries);

        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class)
                .get(GlobalConfigFiles.class);
        globalConfigFiles.save(config);
        return config;
    }

    private NodeJSBuildWrapper mockWrapper(NodeJSInstallation installation, Config config) {
        NodeJSBuildWrapper wrapper = PowerMockito.spy(new NodeJSBuildWrapper("mock", config.id));
        doReturn(installation).when(wrapper).getNodeJS();
        doReturn(new NodeJSBuildWrapper.DescriptorImpl()).when(wrapper).getDescriptor();
        return wrapper;
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
            assertEquals("Unexpected value for " + NodeJSConstants.ENVVAR_NODEJS_HOME, expectedValue, env.get(NodeJSConstants.ENVVAR_NODEJS_HOME));
            assertThat(env.get("PATH"), CoreMatchers.containsString(expectedValue));
            // check that PATH is not exact the NodeJS home otherwise means PATH was overridden
            assertThat(env.get("PATH"), CoreMatchers.is(CoreMatchers.not(expectedValue))); // JENKINS-41947
        }
    }

}