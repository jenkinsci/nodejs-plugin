package jenkins.plugins.nodejs;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Environment;
import hudson.model.EnvironmentList;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMConfig.NPMConfigProvider;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.configfiles.Npmrc;

public class NpmrcFileSupplyTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void test_supply_npmrc_with_registry() throws Exception {
        StandardUsernameCredentials user = createUser("test-user-id", "myuser", "mypassword");
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", user.getId(), null);
        NPMRegistry officalRegistry = new NPMRegistry("https://registry.npmjs.org/", null, "@user1 user2");

        Config config = createSetting("mytest", "email=guest@example.com",
                Arrays.asList(privateRegistry, officalRegistry));

        List<Environment> enviroments = new ArrayList<Environment>();

        FreeStyleBuild build = new MockBuild(j.createFreeStyleProject(), new FilePath(folder.newFolder()), enviroments);

        FilePath npmrcFile = NodeJSUtils.supplyConfig(config.id, build, build.getWorkspace(), j.createTaskListener(), new EnvVars());
        assertTrue(npmrcFile.exists());
        assertTrue(npmrcFile.length() > 0);

        Npmrc npmrc = Npmrc.load(new File(npmrcFile.getRemote()));
        assertTrue("Missing setting email", npmrc.contains("email"));
        assertEquals("Unexpected value from settings email", "guest@example.com", npmrc.get("email"));
    }

    private StandardUsernameCredentials createUser(String id, String username, String password) {
        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, null, username, password);
    }

    private Config createSetting(String id, String content, List<NPMRegistry> registries) {
        String providerId = new NPMConfigProvider().getProviderId();
        Config config = new NPMConfig(id, null, null, content, providerId, registries);

        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class)
                .get(GlobalConfigFiles.class);
        globalConfigFiles.save(config);
        return config;
    }

    private class MockBuild extends FreeStyleBuild {
        List<Environment> environments = new ArrayList<Environment>();

        public MockBuild(FreeStyleProject project, FilePath workspace, List<Environment> enviroments)
                throws IOException {
            super(project);
            setWorkspace(workspace);
            this.environments = enviroments;
        }

        @Override
        public EnvVars getEnvironment(TaskListener log) throws IOException, InterruptedException {
            return new EnvVars();
        }

        @Override
        public EnvironmentList getEnvironments() {
            return new EnvironmentList(environments);
        }
    }
}