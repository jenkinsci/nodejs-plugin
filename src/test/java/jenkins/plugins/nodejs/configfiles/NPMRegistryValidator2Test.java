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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import jenkins.plugins.nodejs.configfiles.NPMRegistry.DescriptorImpl;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Test input form validation.
 *
 * @author Nikolas Falco
 */
@WithJenkins
class NPMRegistryValidator2Test {

    private static JenkinsRule rule;

    @BeforeAll
    static void setUp(JenkinsRule r) {
        rule = r;
    }

    @Test
    void test_credentials_ok() throws Exception {
        String credentialsId = "secret";
        Credentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "", "user", "password");
        Map<Domain, List<Credentials>> credentialsMap = new HashMap<>();
        credentialsMap.put(Domain.global(), List.of(credentials));
        SystemCredentialsProvider.getInstance().setDomainCredentialsMap(credentialsMap);

        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.hasPermission(isA(Permission.class))).thenReturn(true);

        DescriptorImpl descriptor = mock(DescriptorImpl.class);
        when(descriptor.doCheckCredentialsId(any(Item.class), any(), anyString())).thenCallRealMethod();

        String serverURL = "http://acme.com";

        FormValidation result = descriptor.doCheckCredentialsId(prj, credentialsId, serverURL);
        assertThat(result.kind).isEqualTo(Kind.OK);
    }

}
