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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.tools.ant.types.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.powermock.reflect.Whitebox;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.NodeJSBuildWrapper;
import jenkins.plugins.nodejs.cache.DefaultCacheLocationLocator;
import jenkins.plugins.nodejs.tools.NodeJSInstallation.DescriptorImpl;

public class NodeJSInstallationTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    /**
     * Verify node executable is begin initialised correctly on a slave
     * node where {@link Computer#currentComputer()} is {@code null}.
     */
    @Issue("JENKINS-42232")
    @Test
    public void test_executable_resolved_on_slave_node() throws Exception {
        assertNull(Computer.currentComputer());
        NodeJSInstallation installation = new NodeJSInstallation("test_executable_resolved_on_slave_node", "/home/nodejs", null);
        Platform platform = Whitebox.invokeMethod(installation, "getPlatform");
        assertEquals(Platform.current(), platform);
    }

    /**
     * Verify that the singleton installation descriptor load data from disk
     * when its constructor is called.
     */
    @LocalData
    @Test
    @Issue("JENKINS-41535")
    public void test_load_at_startup() throws Exception {
        File jenkinsHome = r.jenkins.getRootDir();
        File installationsFile = new File(jenkinsHome, NodeJSInstallation.class.getName() + ".xml");

        assertTrue("NodeJS installations file has not been copied",  installationsFile.exists());
        verify();
    }

    /**
     * Verify that the serialisation is backward compatible.
     */
    @LocalData
    @Test
    @Issue("JENKINS-57844")
    public void test_serialisation_is_compatible_with_version_1_2_x() throws Exception {
        FreeStyleProject prj = r.jenkins.getAllItems(hudson.model.FreeStyleProject.class) //
                .stream() //
                .filter(p -> "test".equals(p.getName())) //
                .findFirst().get();

        NodeJSBuildWrapper step = prj.getBuildWrappersList().get(NodeJSBuildWrapper.class);
        assertThat(step.getCacheLocationStrategy(), CoreMatchers.instanceOf(DefaultCacheLocationLocator.class));
    }

    /**
     * Simulates the addition of the new NodeJS via UI and makes sure it works
     * and persistent file was created.
     */
    @Test
    @Issue("JENKINS-41535")
    public void test_persist_of_nodejs_installation() throws Exception {
        File jenkinsHome = r.jenkins.getRootDir();
        File installationsFile = new File(jenkinsHome, NodeJSInstallation.class.getName() + ".xml");

        assertFalse("NodeJS installations file already exists", installationsFile.exists());

        HtmlPage p = getConfigurePage();
        HtmlForm f = p.getFormByName("config");
        HtmlButton b = r.getButtonByCaption(f, "Add NodeJS");
        b.click();
        r.findPreviousInputElement(b, "name").setValueAttribute("myNode");
        r.findPreviousInputElement(b, "home").setValueAttribute("/tmp/foo");
        r.submit(f);
        verify();

        assertTrue("NodeJS installations file has not been saved",  installationsFile.exists());

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
        assertEquals(1, l.length);
        r.assertEqualBeans(l[0], new NodeJSInstallation("myNode", "/tmp/foo", JenkinsRule.NO_PROPERTIES), "name,home");

        // by default we should get the auto installer
        DescribableList<ToolProperty<?>, ToolPropertyDescriptor> props = l[0].getProperties();
        assertEquals(1, props.size());
        InstallSourceProperty isp = props.get(InstallSourceProperty.class);
        assertEquals(1, isp.installers.size());
        assertNotNull(isp.installers.get(NodeJSInstaller.class));
    }

}