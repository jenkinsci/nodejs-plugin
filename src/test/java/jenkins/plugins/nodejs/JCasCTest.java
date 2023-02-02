/*
 * The MIT License
 *
 * Copyright (c) 2020, Evaristo Gutierrez
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.junit.Assert;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import hudson.tools.BatchCommandInstaller;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.tools.ZipExtractionInstaller;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.NodeJSInstaller;

public class JCasCTest extends RoundTripAbstractTest {

    @Override
    protected void assertConfiguredAsExpected(RestartableJenkinsRule restartableJenkinsRule, String s) {
        checkConfigFile(restartableJenkinsRule.j.jenkins);
        checkInstallations(restartableJenkinsRule.j.jenkins);
    }

    private void checkConfigFile(Jenkins j) {
        Config config = ConfigFiles.getByIdOrNull(j, "myconfigfile");
        assertThat(config, instanceOf(NPMConfig.class));

        NPMConfig npmConfig = (NPMConfig) config;
        assertEquals("myComment", npmConfig.comment);
        assertEquals("myContent", npmConfig.content);
        assertEquals("myConfig", npmConfig.name);
        assertEquals(true, npmConfig.isNpm9Format());

        List<NPMRegistry> registries = npmConfig.getRegistries();
        Assert.assertTrue(registries.size() == 1);

        NPMRegistry registry = registries.get(0);
        assertTrue(registry.isHasScopes());
        assertEquals("myScope", registry.getScopes());
        assertEquals("registryUrl", registry.getUrl());
    }

    private void checkInstallations(Jenkins j) {
        final ToolDescriptor descriptor = (ToolDescriptor) j.getDescriptor(NodeJSInstallation.class);
        final ToolInstallation[] installations = descriptor.getInstallations();
        assertThat(installations, arrayWithSize(2));

        ToolInstallation withInstaller = installations[0];
        assertEquals("myNode", withInstaller.getName());

        final DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = withInstaller.getProperties();
        assertThat(properties, hasSize(1));
        final ToolProperty<?> property = properties.get(0);

        assertThat(((InstallSourceProperty)property).installers,
                containsInAnyOrder(
                        allOf(instanceOf(NodeJSInstaller.class),
                                hasProperty("npmPackagesRefreshHours", equalTo(75L)),
                                hasProperty("npmPackages", equalTo("globalPackages")),
                                hasProperty("force32Bit", equalTo(true))),
                        allOf(instanceOf(CommandInstaller.class),
                                hasProperty("command", equalTo("install npm")),
                                hasProperty("toolHome", equalTo("/my/path/1")),
                                hasProperty("label", equalTo("npm command"))),
                        allOf(instanceOf(ZipExtractionInstaller.class),
                                hasProperty("url", equalTo("http://fake.com")),
                                hasProperty("subdir", equalTo("/my/path/2")),
                                hasProperty("label", equalTo("npm zip"))),
                        allOf(instanceOf(BatchCommandInstaller.class),
                                hasProperty("command", equalTo("run batch command")),
                                hasProperty("toolHome", equalTo("/my/path/3")),
                                hasProperty("label", equalTo("npm batch")))
                ));

        ToolInstallation withoutInstaller = installations[1];
        assertThat(withoutInstaller,
                allOf(
                        hasProperty("name", equalTo("anotherNodeWithNoInstall")),
                        hasProperty("home", equalTo("/onePath")
                )));
    }

    @Override
    protected String stringInLogExpected() {
        return "Setting class jenkins.plugins.nodejs.configfiles.NPMConfig.id";
    }
}
