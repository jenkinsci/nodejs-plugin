package jenkins.plugins.nodejs.configfiles;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        assertThat(result.kind, is(Kind.ERROR));
        assertThat(result.getMessage(), is(Messages.NPMRegistry_DescriptorImpl_emptyScopes()));
    }

    @Test
    public void test_scopes_with_at_in_name() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "@scope1");
        assertThat(result.kind, is(Kind.WARNING));
        assertThat(result.getMessage(), is(Messages.NPMRegistry_DescriptorImpl_invalidCharInScopes()));
    }

    @Test
    public void test_invalid_scopes() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "@");
        assertThat(result.kind, is(Kind.ERROR));
        assertThat(result.getMessage(), is(Messages.NPMRegistry_DescriptorImpl_invalidScopes()));
    }

    @Test
    public void test_scopes_ok() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckScopes(true, "scope1 scope2 scope3");
        assertThat(result.kind, is(Kind.OK));
    }

    @Test
    public void test_empty_server_url() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("");
        assertThat(result.kind, is(Kind.ERROR));
        assertThat(result.getMessage(), is(Messages.NPMRegistry_DescriptorImpl_emptyRegistryURL()));
    }

    @Test
    public void test_server_url_that_contains_variable() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("${REGISTRY_URL}/root");
        assertThat(result.kind, is(Kind.OK));
        result = descriptor.doCheckUrl("http://${SERVER_NAME}/root");
        assertThat(result.kind, is(Kind.OK));
        result = descriptor.doCheckUrl("http://acme.com/${CONTEXT_ROOT}");
        assertThat(result.kind, is(Kind.OK));
    }

    @Test
    public void test_empty_server_url_is_ok() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("http://acme.com");
        assertThat(result.kind, is(Kind.OK));
    }

    @Test
    public void test_server_url_invalid_protocol() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("hpp://acme.com/root");
        assertThat(result.kind, is(Kind.ERROR));
        assertThat(result.getMessage(), is(Messages.NPMRegistry_DescriptorImpl_invalidRegistryURL()));
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
        assertThat(result.kind, is(Kind.ERROR));
        assertThat(result.getMessage(), is(Messages.NPMRegistry_DescriptorImpl_invalidCredentialsId()));

        when(prj.hasPermission(isA(Permission.class))).thenReturn(false);
        result = descriptor.doCheckCredentialsId(prj, credentialsId, serverURL);
        assertThat(result.kind, is(Kind.OK));
    }

    @Test
    public void test_empty_credentials() throws Exception {
        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.hasPermission(isA(Permission.class))).thenReturn(true);

        DescriptorImpl descriptor = mock(DescriptorImpl.class);
        when(descriptor.doCheckCredentialsId(any(Item.class), (String) any(), anyString())).thenCallRealMethod();

        String serverURL = "http://acme.com";

        FormValidation result = descriptor.doCheckCredentialsId(prj, "", serverURL);
        assertThat(result.kind, is(Kind.WARNING));
        assertThat(result.getMessage(), is(Messages.NPMRegistry_DescriptorImpl_emptyCredentialsId()));
        result = descriptor.doCheckCredentialsId(prj, null, serverURL);
        assertThat(result.kind, is(Kind.WARNING));
        assertThat(result.getMessage(), is(Messages.NPMRegistry_DescriptorImpl_emptyCredentialsId()));
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
        assertThat(result.kind, is(Kind.OK));
    }

}
