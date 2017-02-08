package jenkins.plugins.nodejs.tools;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.tools.NodeJSInstallation.DescriptorImpl;

public class NodeJSInstallationTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    /**
     * Verify that the singleton installation descriptor load data from disk
     * when on its constructor
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