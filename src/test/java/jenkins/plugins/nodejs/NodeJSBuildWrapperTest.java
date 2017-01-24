package jenkins.plugins.nodejs;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    private static NodeJSInstallation installation;

    @Test
    public void test_creation_of_config() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("free");

        final Config config = createSetting("my-config-id", "email=foo@acme.com", null);

        installation = mock(NodeJSInstallation.class);
        when(installation.forNode(any(Node.class), any(TaskListener.class))).thenReturn(installation);
        when(installation.forEnvironment(any(EnvVars.class))).thenReturn(installation);
        when(installation.getName()).thenReturn("mockNode");
        when(installation.getHome()).thenReturn("/nodejs/home");

        NodeJSBuildWrapper bw = new MockNodeJSBuildWrapper(installation.getName(), config.id);

        job.getBuildWrappersList().add(bw);

        job.getBuildersList().add(new VerifyEnvVariableBuilder(NodeJSConstants.NPM_USERCONFIG));

        j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

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

    private static final class MockNodeJSBuildWrapper extends NodeJSBuildWrapper {

        public MockNodeJSBuildWrapper(String nodeJSInstallationName, String configId) {
            super(nodeJSInstallationName, configId);
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

    private static final class VerifyEnvVariableBuilder extends Builder {
        private String var;

        public VerifyEnvVariableBuilder(String var) {
            this.var = var;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {

            EnvVars env = build.getEnvironment(listener);
            assertTrue("variable " + var + " not set", env.containsKey(var));

            String value = env.get(var);
            assertNotNull("empty value for environment variable " + var, value);

            assertTrue("file of variable " + var + " does not exists or is not a file", new File(value).isFile());

            return true;
        }
    }
}
