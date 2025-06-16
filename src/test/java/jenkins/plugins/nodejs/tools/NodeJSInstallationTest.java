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

import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import hudson.model.Computer;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.tools.NodeJSInstallation.DescriptorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;

@WithJenkins
class NodeJSInstallationTest {

    private JenkinsRule r;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        r = rule;
    }

    /**
     * Verify node executable is being initialised correctly on a slave
     * node where {@link Computer#currentComputer()} is {@code null}.
     */
    @Issue("JENKINS-42232")
    @Test
    void test_executable_resolved_on_slave_node() throws Exception {
        assertThat(Computer.currentComputer()).isNull();
        NodeJSInstallation installation = new NodeJSInstallation("test_executable_resolved_on_slave_node", "/home/nodejs", null);
        Method method = installation.getClass().getDeclaredMethod("getPlatform");
        method.setAccessible(true);
        Platform platform = (Platform) method.invoke(installation);
        assertThat(platform).isEqualTo(Platform.current());
    }

    /**
     * Verify that the singleton installation descriptor load data from disk
     * when its constructor is called.
     */
    @LocalData
    @Test
    @Issue("JENKINS-41535")
    void test_load_at_startup() throws Exception {
        File jenkinsHome = r.jenkins.getRootDir();
        File installationsFile = new File(jenkinsHome, NodeJSInstallation.class.getName() + ".xml");

        assertThat(installationsFile).as("NodeJS installations file has not been copied").exists();
        verify();
    }

    /**
     * Simulates the addition of the new NodeJS via UI and makes sure it works
     * and persistent file was created.
     */
    @Test
    @Issue("JENKINS-41535")
    void test_persist_of_nodejs_installation() throws Exception {
        File jenkinsHome = r.jenkins.getRootDir();
        File installationsFile = new File(jenkinsHome, NodeJSInstallation.class.getName() + ".xml");

        assertThat(installationsFile).as("NodeJS installations file already exists").doesNotExist();

        HtmlPage p = getConfigurePage();
        HtmlForm f = p.getFormByName("config");
        HtmlButton b = r.getButtonByCaption(f, "Add NodeJS");
        b.click();
        r.findPreviousInputElement(b, "name").setValue("myNode");
        r.findPreviousInputElement(b, "home").setValue("/tmp/foo");
        r.submit(f);
        verify();

        assertThat(installationsFile).as("NodeJS installations file has not been saved").exists();

        // another submission and verify it survives a roundtrip
        p = getConfigurePage();
        f = p.getFormByName("config");
        r.submit(f);
        verify();
    }

    private HtmlPage getConfigurePage() throws IOException, SAXException {
        return Jenkins.getVersion().toString().startsWith("2") ?
                r.createWebClient().goTo("configureTools")
                : r.createWebClient().goTo("configure");
    }

    private void verify() throws Exception {
        NodeJSInstallation[] l = r.get(DescriptorImpl.class).getInstallations();
        assertThat(l).hasSize(1);
        r.assertEqualBeans(l[0], new NodeJSInstallation("myNode", "/tmp/foo", JenkinsRule.NO_PROPERTIES), "name,home");

        // by default we should get the auto installer
        DescribableList<ToolProperty<?>, ToolPropertyDescriptor> props = l[0].getProperties();
        assertThat(props).hasSize(1);
        InstallSourceProperty isp = props.get(InstallSourceProperty.class);
        assertThat(isp.installers).hasSize(1);
        assertThat(isp.installers.get(NodeJSInstaller.class)).isNotNull();
    }

}