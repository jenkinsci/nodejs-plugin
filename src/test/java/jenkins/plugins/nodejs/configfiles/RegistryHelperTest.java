package jenkins.plugins.nodejs.configfiles;

import static jenkins.plugins.nodejs.NodeJSConstants.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
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

    @Test
    public void test_fill_credentials() {
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", user.getId(), null);
        NPMRegistry officalRegistry = new NPMRegistry("https://registry.npmjs.org/", null, "@user1 user2");

        Map<String, StandardUsernameCredentials> resolvedCredentials = new HashMap<>();
        resolvedCredentials.put(privateRegistry.getUrl(), user);

        RegistryHelper helper = new RegistryHelper(Arrays.asList(privateRegistry, officalRegistry));
        String content = helper.fillRegistry("", resolvedCredentials);
        assertNotNull(content);

        Npmrc npmrc = new Npmrc();
        npmrc.from(content);

        // test private registry
        assertTrue("Unexpected value for " + NPM_SETTINGS_ALWAYS_AUTH, npmrc.getAsBoolean(NPM_SETTINGS_ALWAYS_AUTH));
        assertEquals("Unexpected value for " + NPM_SETTINGS_REGISTRY, privateRegistry.getUrl(), npmrc.get(NPM_SETTINGS_REGISTRY));
        // test _auth
        assertTrue("Missing setting " + NPM_SETTINGS_AUTH, npmrc.contains(NPM_SETTINGS_AUTH));
        String auth = npmrc.get(NPM_SETTINGS_AUTH);
        assertNotNull("Unexpected value for " + NPM_SETTINGS_AUTH, npmrc);
        auth = new String(Base64.decodeBase64(auth));
        assertThat(auth, allOf(startsWith(user.getUsername()), endsWith("mypassword")));

        // test official registry
        String prefix = helper.calculatePrefix(officalRegistry.getUrl());
        for (String scope : officalRegistry.getScopesAsList()) {
            scope = '@' + scope;
            assertEquals(officalRegistry.getUrl(), npmrc.get(helper.compose(scope, NPM_SETTINGS_REGISTRY)));
        }
        assertFalse(npmrc.getAsBoolean(helper.compose(prefix, NPM_SETTINGS_ALWAYS_AUTH)));
        assertFalse(npmrc.contains(helper.compose(prefix, NPM_SETTINGS_AUTH)));
    }

}