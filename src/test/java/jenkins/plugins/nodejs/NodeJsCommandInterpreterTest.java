package jenkins.plugins.nodejs;

import hudson.FilePath;
import hudson.model.Descriptor;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import hudson.tasks.Builder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Collections;

import static de.regnis.q.sequence.core.QSequenceAssert.assertTrue;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class NodeJsCommandInterpreterTest {

    private static final String COMMAND = "var sys = require('sys'); sys.puts('build number: ' + process.env['BUILD_NUMBER']);";

    private NodeJsCommandInterpreter interpreter;
    private Descriptor<Builder> descriptor;
    private NodeJSInstallation installation;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        installation = new NodeJSInstallation("11.0.0", "", Collections.EMPTY_LIST);
        interpreter = new NodeJsCommandInterpreter(COMMAND, installation.getName());
        descriptor = interpreter.getDescriptor();
    }

    @Test
    public void testGetContentsShouldGiveExpectedValue() {
        assertEquals(COMMAND, interpreter.getCommand());
    }

    @Test
    public void testGetContentWithEmptyCommandShouldGiveExpectedValue() {
        assertEquals("", new NodeJsCommandInterpreter("", installation.getName()).getCommand());
    }

    @Test
    public void testGetContentWithNullCommandShouldGiveExpectedValue() {
        assertNull(new NodeJsCommandInterpreter(null, installation.getName()).getCommand());
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
        assertEquals("Execute NodeJS script", descriptor.getDisplayName());
    }

    @Test
    public void testDescriptorGetHelpFileShouldGiveExpectedValue() {
        assertEquals("/plugin/nodejs/help.html", descriptor.getHelpFile());
    }
}