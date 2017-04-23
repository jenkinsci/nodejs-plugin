package jenkins.plugins.nodejs.configfiles;

import static jenkins.plugins.nodejs.NodeJSConstants.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.util.Secret;

@RunWith(Parameterized.class)
public class RegistryHelperCredentialsTest {

    @Parameters(name = "test registries: {0}")
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> dataParameters = new ArrayList<Object[]>();

        user = Mockito.mock(StandardUsernamePasswordCredentials.class);
        Mockito.when(user.getId()).thenReturn("privateId");
        Mockito.when(user.getUsername()).thenReturn("myuser");

        Constructor<Secret> c = Secret.class.getDeclaredConstructor(String.class);
        c.setAccessible(true);
        Secret userSecret = c.newInstance("mypassword");
        Mockito.when(((StandardUsernamePasswordCredentials) user).getPassword()).thenReturn(userSecret);

        NPMRegistry globalRegistry = new NPMRegistry("https://registry.npmjs.org", null, null);
        NPMRegistry proxyRegistry = new NPMRegistry("https://registry.proxy.com", user.getId(), null);
        NPMRegistry scopedGlobalRegsitry = new NPMRegistry("https://registry.npmjs.org", null, "@user1 user2");
        NPMRegistry organisationRegistry = new NPMRegistry("https://registry.acme.com", user.getId(), "scope1 scope2");

        dataParameters.add(new Object[] { "global no auth", new NPMRegistry[] { globalRegistry } });
        dataParameters.add(new Object[] { "proxy with auth", new NPMRegistry[] { proxyRegistry } });
        dataParameters.add(new Object[] { "global scoped no auth", new NPMRegistry[] { scopedGlobalRegsitry } });
        dataParameters.add(new Object[] { "organisation scoped with auth", new NPMRegistry[] { organisationRegistry } });
        dataParameters.add(new Object[] { "mix of proxy + global scoped + scped organisation registries",
                new NPMRegistry[] { proxyRegistry, scopedGlobalRegsitry, organisationRegistry } });

        return dataParameters;
    }

    private static StandardUsernameCredentials user;
    private NPMRegistry[] registries;
    private Map<String, StandardUsernameCredentials> resolvedCredentials;

    public RegistryHelperCredentialsTest(String testName, NPMRegistry[] registries) {
        this.registries = registries;

        resolvedCredentials = new HashMap<>();
        for (NPMRegistry r : registries) {
            if (r.getCredentialsId() != null) {
                resolvedCredentials.put(r.getUrl(), user);
            }
        }
    }

    @Test
    public void test_registry_credentials() throws Exception {
        RegistryHelper helper = new RegistryHelper(Arrays.asList(registries));
        String content = helper.fillRegistry("", resolvedCredentials);
        assertNotNull(content);

        Npmrc npmrc = new Npmrc();
        npmrc.from(content);

        for (NPMRegistry registry : registries) {
            if (!registry.isHasScopes()) {
                verifyGlobalRegistry(registry, npmrc);
            } else {
                verifyScopedRegistry(helper, npmrc, registry);
            }

        }
    }

    private void verifyScopedRegistry(RegistryHelper helper, Npmrc npmrc, NPMRegistry registry) {
        String prefix = helper.calculatePrefix(registry.getUrl());
        for (String scope : registry.getScopesAsList()) {
            assertFalse("Unexpected value for " + NPM_SETTINGS_AUTH, npmrc.contains(helper.compose(prefix, NPM_SETTINGS_AUTH)));

            if (registry.getCredentialsId() != null) {
                // test require authentication, by default is false
                assertTrue("Unexpected value for " + NPM_SETTINGS_ALWAYS_AUTH, npmrc.getAsBoolean(helper.compose(prefix, NPM_SETTINGS_ALWAYS_AUTH)));

                // test credentials fields
                assertEquals("Unexpected value for " + NPM_SETTINGS_USER, user.getUsername(), npmrc.get(helper.compose(prefix, NPM_SETTINGS_USER)));
                String password = npmrc.get(helper.compose(prefix, NPM_SETTINGS_PASSWORD));
                assertNotNull("Unexpected value for " + NPM_SETTINGS_PASSWORD, password);
                password = new String(Base64.decodeBase64(password));
                assertEquals("Invalid password for scoped registry", password, "mypassword");
            }

            scope = '@' + scope;
            String scopeKey = helper.compose(scope, NPM_SETTINGS_REGISTRY);
            // test registry URL entry
            assertTrue("Miss registry entry for scope " + scope, npmrc.contains(scopeKey));
            assertEquals("Wrong registry URL for scope " + scope, registry.getUrl() + "/", npmrc.get(scopeKey));
        }
    }

    private void verifyGlobalRegistry(NPMRegistry registry, Npmrc npmrc) {
        // test require authentication, by default is false
        assertEquals("Unexpected value for " + NPM_SETTINGS_ALWAYS_AUTH, registry.getCredentialsId() != null, npmrc.getAsBoolean(NPM_SETTINGS_ALWAYS_AUTH));

        if (registry.getCredentialsId() != null) {
            // test _auth
            String auth = npmrc.get(NPM_SETTINGS_AUTH);
            assertNotNull("Unexpected value for " + NPM_SETTINGS_AUTH, npmrc);
            auth = new String(Base64.decodeBase64(auth));
            assertThat(auth, allOf(startsWith(user.getUsername()), endsWith("mypassword")));
        }

        // test registry URL entry
        assertEquals("Unexpected value for " + NPM_SETTINGS_REGISTRY, registry.getUrl(), npmrc.get(NPM_SETTINGS_REGISTRY));
    }

}