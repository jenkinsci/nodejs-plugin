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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
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
class NPMRegistryValidatorTest {

    @Test
    void test_empty_scopes() {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "");
        assertThat(result.kind).isEqualTo(Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_emptyScopes());
    }

    @Test
    void test_scopes_with_at_in_name() {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "@scope1");
        assertThat(result.kind).isEqualTo(Kind.WARNING);
        assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_invalidCharInScopes());
    }

    @Test
    void test_invalid_scopes() {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "@");
        assertThat(result.kind).isEqualTo(Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_invalidScopes());
    }

    @Test
    void test_scopes_ok() {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "scope1 scope2 scope3");
        assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    void test_empty_server_url() {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("");
        assertThat(result.kind).isEqualTo(Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_emptyRegistryURL());
    }

    @Test
    void test_server_url_that_contains_variable() {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("${REGISTRY_URL}/root");
        assertThat(result.kind).isEqualTo(Kind.OK);
        result = descriptor.doCheckUrl("http://${SERVER_NAME}/root");
        assertThat(result.kind).isEqualTo(Kind.OK);
        result = descriptor.doCheckUrl("http://acme.com/${CONTEXT_ROOT}");
        assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    void test_empty_server_url_is_ok() {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("http://acme.com");
        assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    void test_server_url_invalid_protocol() {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("hpp://acme.com/root");
        assertThat(result.kind).isEqualTo(Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_invalidRegistryURL());
    }

    @Test
    void test_invalid_credentials() {
        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.hasPermission(isA(Permission.class))).thenReturn(true);

        DescriptorImpl descriptor = mock(DescriptorImpl.class);
        when(descriptor.doCheckCredentialsId(any(Item.class), any(), anyString())).thenCallRealMethod();

        String credentialsId = "secret";
        String serverURL = "http://acme.com";

        FormValidation result = descriptor.doCheckCredentialsId(prj, credentialsId, serverURL);
        assertThat(result.kind).isEqualTo(Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_invalidCredentialsId());

        when(prj.hasPermission(isA(Permission.class))).thenReturn(false);
        result = descriptor.doCheckCredentialsId(prj, credentialsId, serverURL);
        assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    void test_empty_credentials() {
        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.hasPermission(isA(Permission.class))).thenReturn(true);

        DescriptorImpl descriptor = mock(DescriptorImpl.class);
        when(descriptor.doCheckCredentialsId(any(Item.class), any(), anyString())).thenCallRealMethod();

        String serverURL = "http://acme.com";

        FormValidation result = descriptor.doCheckCredentialsId(prj, "", serverURL);
        assertThat(result.kind).isEqualTo(Kind.WARNING);
        assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_emptyCredentialsId());
        result = descriptor.doCheckCredentialsId(prj, null, serverURL);
        assertThat(result.kind).isEqualTo(Kind.WARNING);
        assertThat(result.getMessage()).isEqualTo(Messages.NPMRegistry_DescriptorImpl_emptyCredentialsId());
    }

}
