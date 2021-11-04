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
package jenkins.plugins.nodejs.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstallerDescriptor;
import hudson.tools.DownloadFromUrlInstaller.Installable;

public class MirrorNodeJSInstallerTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    public static class MockMirrorNodeJSInstaller extends MirrorNodeJSInstaller {

        private Installable installable;

        public MockMirrorNodeJSInstaller(Installable installable, String mirrorURL) {
            super(installable.id, mirrorURL, null, 0);
            this.installable = installable;
        }

        @Override
        public boolean isUpToDate(FilePath expectedLocation, Installable i) throws IOException, InterruptedException {
            return true;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public ToolInstallerDescriptor<?> getDescriptor() {
            hudson.tools.DownloadFromUrlInstaller.DescriptorImpl<?> descriptor = mock(hudson.tools.DownloadFromUrlInstaller.DescriptorImpl.class);
            try {
                when(descriptor.getInstallables()).thenReturn((List) Arrays.asList(installable));
            } catch (IOException e) {
            }
            return descriptor;
        }
    }

    @Test
    public void verify_mirror_url_replacing() throws Exception {
        String installationId = "8.2.1";
        String mirror = "http://npm.taobao.org/mirrors/node/";

        Installable installable = new Installable();
        installable.id = installationId;
        installable.url = "https://nodejs.org/dist/v8.2.1/";

        MockMirrorNodeJSInstaller installer = Mockito.spy(new MockMirrorNodeJSInstaller(installable, mirror));

        ToolInstallation tool = mock(ToolInstallation.class);
        when(tool.getHome()).thenReturn("home");

        Node node = r.getInstance().getComputer("").getNode();
        installer.performInstallation(tool, node, mock(TaskListener.class));

        ArgumentCaptor<Installable> captor = ArgumentCaptor.forClass(Installable.class);
        verify(installer).isUpToDate(any(FilePath.class), captor.capture());
        Assertions.assertThat(captor.getValue().url).startsWith("http://npm.taobao.org/mirrors/node/v8.2.1/node-v8.2.1");
    }

    @Test
    public void verify_credentials_on_mirror_url() throws Exception {
        String credentialsId = "secret";
        String installationId = "8.2.1";
        String mirror = "http://npm.taobao.org/mirrors/node/";

        Installable installable = new Installable();
        installable.id = installationId;
        installable.url = "https://nodejs.org/dist/v8.2.1/";

        Credentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "", "user", "password");
        Map<Domain, List<Credentials>> credentialsMap = new HashMap<>();
        credentialsMap.put(Domain.global(), Arrays.asList(credentials));
        SystemCredentialsProvider.getInstance().setDomainCredentialsMap(credentialsMap);
        
        MockMirrorNodeJSInstaller installer = Mockito.spy(new MockMirrorNodeJSInstaller(installable, mirror));
        installer.setCredentialsId(credentialsId);

        ToolInstallation tool = mock(ToolInstallation.class);
        when(tool.getHome()).thenReturn("home");

        Node node = r.getInstance().getComputer("").getNode();
        installer.performInstallation(tool, node, mock(TaskListener.class));

        ArgumentCaptor<Installable> captor = ArgumentCaptor.forClass(Installable.class);
        verify(installer).isUpToDate(any(FilePath.class), captor.capture());
        Assertions.assertThat(captor.getValue().url).startsWith("http://user:password@npm.taobao.org/mirrors/node/node-v8.2.1");
    }

}
