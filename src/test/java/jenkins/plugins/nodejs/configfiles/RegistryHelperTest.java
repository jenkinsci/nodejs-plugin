package jenkins.plugins.nodejs.configfiles;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.FreeStyleBuild;

public class RegistryHelperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    private StandardUsernameCredentials user;

    @Before
    public void setUp() throws Exception {
        user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "privateId", "dummy desc", "myuser", "mypassword");
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), user);
    }

    @Test
    public void test_registry_credentials_resolution() throws Exception {
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", user.getId(), null);
        NPMRegistry officalRegistry = new NPMRegistry("https://registry.npmjs.org/", null, "@user1 user2");

        FreeStyleBuild build = j.createFreeStyleProject().createExecutable();

        RegistryHelper helper = new RegistryHelper(Arrays.asList(privateRegistry, officalRegistry));
        Map<String, StandardUsernameCredentials> resolvedCredentials = helper.resolveCredentials(build);
        assertFalse(resolvedCredentials.isEmpty());
        assertEquals(1, resolvedCredentials.size());

        assertThat(resolvedCredentials.keySet(), hasItem(privateRegistry.getUrl()));
        assertThat(resolvedCredentials.get(privateRegistry.getUrl()), equalTo(user));
    }

}