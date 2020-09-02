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
package jenkins.plugins.nodejs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ConfigFile;
import org.jenkinsci.lib.configprovider.model.ConfigFileManager;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.configfiles.Npmrc;

public class NpmrcFileSupplyTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test_supply_npmrc_with_registry() throws Exception {
        StandardUsernameCredentials user = createUser("test-user-id", "myuser", "mypassword");
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", user.getId(), null);
        NPMRegistry officalRegistry = new NPMRegistry("https://registry.npmjs.org/", null, "@user1 user2");

        Config config = createSetting("mytest", "email=guest@example.com", Arrays.asList(privateRegistry, officalRegistry));

        FreeStyleBuild build = j.buildAndAssertSuccess(j.createFreeStyleProject());

        FilePath npmrcFile = ConfigFileManager.provisionConfigFile(new ConfigFile(config.id, null, true), null, build, build.getWorkspace(), j.createTaskListener(), new ArrayList<String>(1));
        assertTrue(npmrcFile.exists());
        assertTrue(npmrcFile.length() > 0);

        Npmrc npmrc = Npmrc.load(new File(npmrcFile.getRemote()));
        assertTrue("Missing setting email", npmrc.contains("email"));
        assertEquals("Unexpected value from settings email", "guest@example.com", npmrc.get("email"));
    }

    private StandardUsernameCredentials createUser(String id, String username, String password) {
        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, null, username, password);
    }

    private Config createSetting(String id, String content, List<NPMRegistry> registries) {
        Config config = new NPMConfig(id, null, null, content, registries);

        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class)
                .get(GlobalConfigFiles.class);
        globalConfigFiles.save(config);
        return config;
    }

}