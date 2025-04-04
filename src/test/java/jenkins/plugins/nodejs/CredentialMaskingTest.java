/*
 * The MIT License
 *
 * Copyright (c) 2023, CloudBees Inc.
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
package jenkins.plugins.nodejs;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.tools.InstallSourceProperty;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.NodeJSInstaller;
import jenkins.plugins.nodejs.tools.Platform;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CredentialMaskingTest {

    @TempDir
    private File temp;

    private static JenkinsRule r;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        r = rule;
    }

    @BeforeEach
    void setupConfigWithCredentials() throws Exception {
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "usercreds", "", "bot", "s3cr3t");
        credential.setUsernameSecret(true);
        SystemCredentialsProvider.getInstance().getCredentials().add(credential);

        credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "usercreds2", "", "luser", "pa55w0rd");
        credential.setUsernameSecret(true);
        SystemCredentialsProvider.getInstance().getCredentials().add(credential);

        SystemCredentialsProvider.getInstance().getCredentials().add(new StringCredentialsImpl(CredentialsScope.GLOBAL, "stringcreds", "", Secret.fromString("sensitive")));

        GlobalConfigFiles.get().save(new NPMConfig("npm", "npm config", "", ";empty config to populate with repos",
                                                   // Refusing to marshal java.util.ImmutableCollections$ListN
                                                   List.of(new NPMRegistry("https://npmjs.example.com", "usercreds", "scope1"),
                                                           new NPMRegistry("https://npmjs2.example.com", "usercreds2", null),
                                                           new NPMRegistry("https://npmjs3.example.com", "stringcreds", "scope2"))));
    }

    @Test
    @Issue("SECURITY-3196")
    void testNPMConfigFileWithConfigFileProviderBlock() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class, "p1");
        p.setDefinition(new CpsFlowDefinition(
                String.join("\n",
                  "node {",
                  "  configFileProvider([configFile(fileId: 'npm', ",
                  "                                 variable: 'NPM_RC_LOCATION')]) {",
                  "    String content = readFile(env.NPM_RC_LOCATION)",
                  "    echo content", //echo the content to validate log masking
                  // test the content is correctly updated"
                  "    assert content.contains('bot')",
                  "    assert content.contains('czNjcjN0')", // base64 encoding of s3cr3t
                  "    assert content.contains('bHVzZXI6cGE1NXcwcmQ=')",   // base64 encoding of luser:pa55w0rd
                  "    assert content.contains('sensitive')",
                  // echo the plain text that is not in the file to check if it is base64 decoded it is still masked
                  "    echo 's3cr3t'",
                  "    echo 'luser'",
                  "    echo 'pa55w0rd'",
                  "  }",
                  "}"),
                true));

        WorkflowRun b1 = r.buildAndAssertSuccess(p);
        // usercreds
        r.assertLogNotContains("bot", b1);
        r.assertLogNotContains("s3cr3t", b1);
        r.assertLogNotContains("czNjcjN0", b1);

        // usercreds2
        r.assertLogNotContains("luser", b1);
        r.assertLogNotContains("pa55w0rd", b1);
        r.assertLogNotContains("bHVzZXI6cGE1NXcwcmQ", b1);

        // stringcreds
        r.assertLogNotContains("sensitive", b1);
    }

    @Test
    @Issue("SECURITY-3196")
    void testNPMConfigFileWithNodejsBlock() throws Exception {
        // fake enough of a nodejs install.
        Platform platform = Platform.current();
        File dir = newFolder(temp, "node-js-dist");
        File bin = new File(dir, platform.binFolder);
        bin.mkdir();
        new File(bin, platform.nodeFileName).createNewFile();
        new File(bin, platform.npmFileName).createNewFile();

        NodeJSInstallation installation = new NodeJSInstallation("bogus-nodejs", dir.toString(),
                Collections.singletonList(new InstallSourceProperty(Collections.singletonList(new NodeJSInstaller("anything", null, Long.MAX_VALUE)))));
        Jenkins.get().getDescriptorByType(NodeJSInstallation.DescriptorImpl.class).setInstallations(installation);

        WorkflowJob p = r.createProject(WorkflowJob.class, "p2");
        p.setDefinition(new CpsFlowDefinition(
                String.join("\n",
                  "node {",
                  "  nodejs(nodeJSInstallationName:'bogus-nodejs', configId:'npm') {",
                  "    String content = readFile(env.npm_config_userconfig)",
                  "    echo content", //echo the content to validate log masking
                  // test the content is correctly updated"
                  "    assert content.contains('bot')",
                  "    assert content.contains('czNjcjN0')", // base64 encoding of s3cr3t
                  "    assert content.contains('bHVzZXI6cGE1NXcwcmQ=')",   // base64 encoding of luser:pa55w0rd
                  "    assert content.contains('sensitive')",
                  // echo the plain text that is not in the file to check if it is base64 decoded it is still masked
                  "    echo 's3cr3t'",
                  "    echo 'luser'",
                  "    echo 'pa55w0rd'",
                  "  }",
                  "}"),
                true));

        WorkflowRun b1 = r.buildAndAssertSuccess(p);
        // usercreds
        r.assertLogNotContains("bot", b1);
        r.assertLogNotContains("s3cr3t", b1);
        r.assertLogNotContains("czNjcjN0", b1);

        // usercreds2
        r.assertLogNotContains("luser", b1);
        r.assertLogNotContains("pa55w0rd", b1);
        r.assertLogNotContains("bHVzZXI6cGE1NXcwcmQ", b1);

        // stringcreds
        r.assertLogNotContains("sensitive", b1);
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
