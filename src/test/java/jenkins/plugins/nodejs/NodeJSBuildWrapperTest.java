package jenkins.plugins.nodejs;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.configfiles.NPMConfig.NPMConfigProvider;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;

public class NodeJSBuildWrapperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test_creation_of_config() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free");

        final Config config = createSetting("my-config-id", "email=foo@acme.com", null);

        NodeJSInstallation installation = mock(NodeJSInstallation.class);
        when(installation.forNode(any(Node.class), any(TaskListener.class))).then(RETURNS_SELF);
        when(installation.forEnvironment(any(EnvVars.class))).then(RETURNS_SELF);
        when(installation.getName()).thenReturn("mockNode");
        when(installation.getHome()).thenReturn("/nodejs/home");

        NodeJSBuildWrapper bw = new MockNodeJSBuildWrapper(installation, config.id);

        job.getBuildWrappersList().add(bw);

        job.getBuildersList().add(new FileVerifier());

        j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        verify(installation).forNode(any(Node.class), any(TaskListener.class));
        verify(installation).forEnvironment(any(EnvVars.class));
        verify(installation, atLeast(1)).buildEnvVars(any(EnvVars.class));
    }

    @Test
    public void test_inject_path_variable() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free");

        final Config config = createSetting("my-config-id", null, null);

        String nodejsHome = new File("/home", "nodejs").getAbsolutePath(); // platform independent
        final NodeJSInstallation installation = new NodeJSInstallation("inject_var", nodejsHome, null);

        NodeJSBuildWrapper bw = new MockNodeJSBuildWrapper(installation, config.id);

        job.getBuildWrappersList().add(bw);

        job.getBuildersList().add(new PathVerifier(installation));

        j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));
    }

    private Config createSetting(String id, String content, List<NPMRegistry> registries) {
        String providerId = new NPMConfigProvider().getProviderId();
        Config config = new NPMConfig(id, null, null, content, providerId, registries);

        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class)
                .get(GlobalConfigFiles.class);
        globalConfigFiles.save(config);
        return config;
    }

    private static final class MockNodeJSBuildWrapper extends NodeJSBuildWrapper {
        private NodeJSInstallation installation;

        public MockNodeJSBuildWrapper(NodeJSInstallation installation, String configId) {
            super(installation.getName(), configId);
            this.installation = installation;
        }

        @Override
        public NodeJSInstallation getNodeJS() {
            return installation;
        };

        @Override
        public Descriptor<BuildWrapper> getDescriptor() {
            return new NodeJSBuildWrapper.DescriptorImpl();
        }
    }

    private static abstract class VerifyEnvVariableBuilder extends Builder {
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {

            EnvVars env = build.getEnvironment(listener);
            verify(env);
            return true;
        }

        public abstract void verify(EnvVars env);
    }

    private static final class FileVerifier extends VerifyEnvVariableBuilder {
        @Override
        public void verify(EnvVars env) {
            String var = NodeJSConstants.NPM_USERCONFIG;
            String value = env.get(var);

            assertTrue("variable " + var + " not set", env.containsKey(var));
            assertNotNull("empty value for environment variable " + var, value);
            assertTrue("file of variable " + var + " does not exists or is not a file", new File(value).isFile());
        }
    }

    private static final class PathVerifier extends VerifyEnvVariableBuilder {
        private final NodeJSInstallation installation;

        private PathVerifier(NodeJSInstallation installation) {
            this.installation = installation;
        }

        @Override
        public void verify(EnvVars env) {
            String expectedValue = installation.getHome();
            assertEquals(env.get("NODEJS_HOME"), expectedValue);
            assertThat(env.get("PATH"), CoreMatchers.containsString(expectedValue));
        }
    }

}