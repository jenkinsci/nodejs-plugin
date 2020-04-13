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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import jenkins.plugins.nodejs.Messages;
import jenkins.plugins.nodejs.configfiles.NPMRegistry.DescriptorImpl;

/**
 * Test input form validation.
 *
 * @author Nikolas Falco
 */
public class NPMRegistryValidatorTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void test_empty_scopes() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "");
        Assertions.assertThat(result.kind).isEqualTo(Kind.ERROR);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_emptyScopes());
    }

    @Test
    public void test_scopes_with_at_in_name() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "@scope1");
        Assertions.assertThat(result.kind).isEqualTo(Kind.WARNING);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_invalidCharInScopes());
    }

    @Test
    public void test_invalid_scopes() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "@");
        Assertions.assertThat(result.kind).isEqualTo(Kind.ERROR);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_invalidScopes());
    }

    @Test
    public void test_scopes_ok() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "scope1 scope2 scope3");
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    public void test_empty_server_url() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("");
        Assertions.assertThat(result.kind).isEqualTo(Kind.ERROR);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_emptyRegistryURL());
    }

    @Test
    public void test_server_url_that_contains_variable() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("${REGISTRY_URL}/root");
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
        result = descriptor.doCheckUrl("http://${SERVER_NAME}/root");
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
        result = descriptor.doCheckUrl("http://acme.com/${CONTEXT_ROOT}");
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    public void test_empty_server_url_is_ok() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("http://acme.com");
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    public void test_server_url_invalid_protocol() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("hpp://acme.com/root");
        Assertions.assertThat(result.kind).isEqualTo(Kind.ERROR);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_invalidRegistryURL());
    }

    @Test
    public void test_invalid_credentials() throws Exception {
        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.hasPermission(isA(Permission.class))).thenReturn(true);

        DescriptorImpl descriptor = mock(DescriptorImpl.class);
        when(descriptor.doCheckCredentialsId(any(Item.class), (String) any(), anyString())).thenCallRealMethod();

        String credentialsId = "secret";
        String serverURL = "http://acme.com";

        FormValidation result = descriptor.doCheckCredentialsId(prj, credentialsId, serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.ERROR);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_invalidCredentialsId());

        when(prj.hasPermission(isA(Permission.class))).thenReturn(false);
        result = descriptor.doCheckCredentialsId(prj, credentialsId, serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    public void test_empty_credentials() throws Exception {
        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.hasPermission(isA(Permission.class))).thenReturn(true);

        DescriptorImpl descriptor = mock(DescriptorImpl.class);
        when(descriptor.doCheckCredentialsId(any(Item.class), (String) any(), anyString())).thenCallRealMethod();

        String serverURL = "http://acme.com";

        FormValidation result = descriptor.doCheckCredentialsId(prj, "", serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.WARNING);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_emptyCredentialsId());
        result = descriptor.doCheckCredentialsId(prj, null, serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.WARNING);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_emptyCredentialsId());
    }

    @Test
    public void test_credentials_ok() throws Exception {
        String credentialsId = "secret";
        Credentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "", "user", "password");
        Map<Domain, List<Credentials>> credentialsMap = new HashMap<>();
        credentialsMap.put(Domain.global(), Arrays.asList(credentials));
        SystemCredentialsProvider.getInstance().setDomainCredentialsMap(credentialsMap);

        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.hasPermission(isA(Permission.class))).thenReturn(true);

        DescriptorImpl descriptor = mock(DescriptorImpl.class);
        when(descriptor.doCheckCredentialsId(any(Item.class), (String) any(), anyString())).thenCallRealMethod();

        String serverURL = "http://acme.com";

        FormValidation result = descriptor.doCheckCredentialsId(prj, credentialsId, serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
    }

}
