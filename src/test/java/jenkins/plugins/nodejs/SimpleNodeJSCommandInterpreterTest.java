package jenkins.plugins.nodejs;

import hudson.FilePath;
import hudson.model.Descriptor;
import jenkins.plugins.nodejs.Messages;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import hudson.tasks.Builder;
import hudson.tools.ToolProperty;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

public class SimpleNodeJSCommandInterpreterTest {

    private static final String COMMAND = "var sys = require('sys'); sys.puts('build number: ' + process.env['BUILD_NUMBER']);";

    private NodeJSCommandInterpreter interpreter;
    private Descriptor<Builder> descriptor;
    private NodeJSInstallation installation;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        installation = new NodeJSInstallation("11.0.0", "", Collections.<ToolProperty<?>>emptyList());
        interpreter = new NodeJSCommandInterpreter(COMMAND, installation.getName(), null);
        descriptor = new NodeJSCommandInterpreter.NodeJsDescriptor();
    }

    @Test
    public void testGetContentsShouldGiveExpectedValue() {
        assertEquals(COMMAND, interpreter.getCommand());
    }

    @Test
    public void testGetContentWithEmptyCommandShouldGiveExpectedValue() {
        assertEquals("", new NodeJSCommandInterpreter("", installation.getName(), null).getCommand());
    }

    @Test
    public void testGetContentWithNullCommandShouldGiveExpectedValue() {
        assertNull(new NodeJSCommandInterpreter(null, installation.getName(), null).getCommand());
    }

    @Test
    public void testGetFileExtensionShouldGiveExpectedValue() throws IOException, InterruptedException {
        assertEquals(true, interpreter.createScriptFile(new FilePath(tempFolder.newFolder())).getName().endsWith(".js"));
    }

    @Test
    public void testGetDescriptorShouldGiveExpectedValue() {
        assertNotNull(descriptor);
        assertTrue(descriptor instanceof Descriptor<?>);
    }

    @Test
    public void testDescriptorGetDisplayNameShouldGiveExpectedValue() {
        assertEquals(Messages.NodeJSCommandInterpreter_displayName(), descriptor.getDisplayName());
    }

    @Test
    public void testDescriptorGetHelpFileShouldGiveExpectedValue() {
        assertEquals("/plugin/nodejs/help.html", descriptor.getHelpFile());
    }
}