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
package jenkins.plugins.nodejs.configfiles;

import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_ALWAYS_AUTH;
import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_AUTH;
import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_PASSWORD;
import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_REGISTRY;
import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.util.Secret;

@RunWith(Parameterized.class)
public class RegistryHelperCredentialsTest {

    @Parameters(name = "test registries: {0}")
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> dataParameters = new ArrayList<Object[]>();

        user = mock(StandardUsernamePasswordCredentials.class);
        when(user.getId()).thenReturn("privateId");
        when(user.getUsername()).thenReturn("myuser");

        Constructor<Secret> c = Secret.class.getDeclaredConstructor(String.class);
        c.setAccessible(true);
        Secret userSecret = c.newInstance("mypassword");
        when(((StandardUsernamePasswordCredentials) user).getPassword()).thenReturn(userSecret);

        NPMRegistry globalRegistry = new NPMRegistry("https://registry.npmjs.org", null, null);
        NPMRegistry proxyRegistry = new NPMRegistry("https://registry.proxy.com", user.getId(), null);
        NPMRegistry scopedGlobalRegsitry = new NPMRegistry("https://registry.npmjs.org", null, "@user1 user2");
        NPMRegistry organisationRegistry = new NPMRegistry("https://registry.acme.com", user.getId(), "scope1 scope2");

        dataParameters.add(new Object[] { "global no auth", new NPMRegistry[] { globalRegistry }, false });
        dataParameters.add(new Object[] { "proxy with auth", new NPMRegistry[] { proxyRegistry }, false });
        dataParameters.add(new Object[] { "global scoped no auth", new NPMRegistry[] { scopedGlobalRegsitry }, false });
        dataParameters.add(new Object[] { "organisation scoped with auth", new NPMRegistry[] { organisationRegistry }, false });
        dataParameters.add(new Object[] { "organisation scoped with auth", new NPMRegistry[] { organisationRegistry }, true });
        dataParameters.add(new Object[] { "mix of proxy + global scoped + scped organisation registries",
                                          new NPMRegistry[] { proxyRegistry, scopedGlobalRegsitry, organisationRegistry }, true });

        return dataParameters;
    }

    private static StandardUsernameCredentials user;
    private NPMRegistry[] registries;
    private Map<String, StandardCredentials> resolvedCredentials;
    private boolean npm9Format;

    public RegistryHelperCredentialsTest(String testName, NPMRegistry[] registries, boolean npm9Format) {
        this.registries = registries;

        resolvedCredentials = new HashMap<>();
        for (NPMRegistry r : registries) {
            if (r.getCredentialsId() != null) {
                resolvedCredentials.put(r.getUrl(), user);
            }
        }
        this.npm9Format = npm9Format;
    }

    @Test
    public void test_registry_credentials() throws Exception {
        RegistryHelper helper = new RegistryHelper(Arrays.asList(registries));
        String content = helper.fillRegistry("", resolvedCredentials, npm9Format);
        assertNotNull(content);

        Npmrc npmrc = new Npmrc();
        npmrc.from(content);

        for (NPMRegistry registry : registries) {
            if (!registry.isHasScopes()) {
                verifyGlobalRegistry(helper, registry, npmrc);
            } else {
                verifyScopedRegistry(helper, npmrc, registry);
            }

        }
    }

    private void verifyScopedRegistry(RegistryHelper helper, Npmrc npmrc, NPMRegistry registry) {
        String registryPrefix = helper.calculatePrefix(registry.getUrl());

        // scoped registry not depends on npm format, has always the registry prefix
        String alwaysAuthKey = helper.compose(registryPrefix, NPM_SETTINGS_ALWAYS_AUTH);
        String usernameKey = helper.compose(registryPrefix, NPM_SETTINGS_USER);
        String passwordKey = helper.compose(registryPrefix, NPM_SETTINGS_PASSWORD);

        for (String scope : registry.getScopesAsList()) {
            assertFalse("Unexpected value for " + NPM_SETTINGS_AUTH, npmrc.contains(helper.compose(registryPrefix, NPM_SETTINGS_AUTH)));

            if (registry.getCredentialsId() != null) {
                // test require authentication, by default is false
                Assertions.assertThat(npmrc.contains(alwaysAuthKey)).isTrue() //
                    .describedAs("key %s not found", NPM_SETTINGS_ALWAYS_AUTH);
                Assertions.assertThat(npmrc.getAsBoolean(alwaysAuthKey)).isTrue();

                // test credentials fields
                Assertions.assertThat(npmrc.get(usernameKey)).isEqualTo(user.getUsername());
                String password = npmrc.get(passwordKey);
                Assertions.assertThat(password).isNotNull();
                password = new String(Base64.decodeBase64(password));
                Assertions.assertThat(password).isEqualTo("mypassword").describedAs("Invalid scoped password");
            }

            scope = '@' + scope;
            String scopeKey = helper.compose(scope, NPM_SETTINGS_REGISTRY);
            // test registry URL entry
            assertTrue("Miss registry entry for scope " + scope, npmrc.contains(scopeKey));
            assertEquals("Wrong registry URL for scope " + scope, registry.getUrl() + "/", npmrc.get(scopeKey));
        }
    }

    private void verifyGlobalRegistry(RegistryHelper helper, NPMRegistry registry, Npmrc npmrc) {
        String registryPrefix = helper.calculatePrefix(registry.getUrl());
        String alwaysAuthKey = npm9Format ? helper.compose(registryPrefix, NPM_SETTINGS_ALWAYS_AUTH) : NPM_SETTINGS_ALWAYS_AUTH;
        String authKey = npm9Format ? helper.compose(registryPrefix, NPM_SETTINGS_AUTH) : NPM_SETTINGS_AUTH;

        Assertions.assertThat(npmrc.contains(alwaysAuthKey)).isEqualTo(registry.getCredentialsId() != null) //
            .describedAs("Unexpected value for %s", alwaysAuthKey);

        if (registry.getCredentialsId() != null) {
            // test _auth
            String auth = npmrc.get(authKey);
            assertNotNull("Unexpected value for " + NPM_SETTINGS_AUTH, npmrc);
            auth = new String(Base64.decodeBase64(auth));
            Assertions.assertThat(auth).startsWith(user.getUsername()).endsWith("mypassword");
        }

        // test registry URL entry
        Assertions.assertThat(npmrc.get(NPM_SETTINGS_REGISTRY)).isEqualTo(registry.getUrl());
    }

}
