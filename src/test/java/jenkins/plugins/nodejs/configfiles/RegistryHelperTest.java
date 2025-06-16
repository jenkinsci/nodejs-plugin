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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.FreeStyleBuild;
import hudson.util.Secret;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RegistryHelperTest {

    private static JenkinsRule j;

    private StandardUsernameCredentials user;
    private StringCredentials token;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        j = rule;
    }

    @BeforeEach
    void setUp() throws Exception {
        user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "privateId", "dummy desc", "myuser", "mypassword");
        token = new StringCredentialsImpl(CredentialsScope.GLOBAL, "privateToken", "dummy desc", Secret.fromString("mysecret"));
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), user);
        store.addCredentials(Domain.global(), token);
    }

    @Test
    void test_registry_credentials_resolution() throws Exception {
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", user.getId(), null);
        NPMRegistry officialRegistry = new NPMRegistry("https://registry.npmjs.org/", null, "@user1 user2");

        FreeStyleBuild build = j.createFreeStyleProject().createExecutable();

        RegistryHelper helper = new RegistryHelper(Arrays.asList(privateRegistry, officialRegistry));
        Map<String, StandardCredentials> resolvedCredentials = helper.resolveCredentials(build);

        assertThat(resolvedCredentials)
                .isNotEmpty()
                .hasSize(1)
                .containsKey(privateRegistry.getUrl())
                .containsEntry(privateRegistry.getUrl(), user);
    }

    @Test
    void test_registry_auth_token_credentials_resolution() throws Exception {
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", token.getId(), null);
        NPMRegistry officialRegistry = new NPMRegistry("https://registry.npmjs.org/", token.getId(), "@user1 user2");

        FreeStyleBuild build = j.createFreeStyleProject().createExecutable();

        RegistryHelper helper = new RegistryHelper(Arrays.asList(privateRegistry, officialRegistry));
        Map<String, StandardCredentials> resolvedCredentials = helper.resolveCredentials(build);

        assertThat(resolvedCredentials)
                .isNotEmpty()
                .hasSize(2)
                .containsKey(privateRegistry.getUrl())
                .containsEntry(privateRegistry.getUrl(), token)
                .containsKey(officialRegistry.getUrl())
                .containsEntry(officialRegistry.getUrl(), token);
    }
}
