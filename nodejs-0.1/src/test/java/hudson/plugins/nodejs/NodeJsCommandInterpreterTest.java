package hudson.plugins.nodejs;

import hudson.model.Descriptor;
import hudson.plugins.nodejs.NodeJsCommandInterpreter;
import hudson.tasks.Builder;
import junit.framework.TestCase;

public class NodeJsCommandInterpreterTest extends TestCase {

    private static final String COMMAND = "var sys = require('sys'); sys.puts('build number: ' + process.env['BUILD_NUMBER']);";

    private NodeJsCommandInterpreter interpreter;
    private Descriptor<Builder> descriptor;

    @Override
    public void setUp() {
        interpreter = new NodeJsCommandInterpreter(COMMAND);
        descriptor = interpreter.getDescriptor();
    }

    public void testGetContentsShouldGiveExpectedValue() {
        assertEquals(COMMAND, interpreter.getCommand());
    }

    public void testGetContentWithEmptyCommandShouldGiveExpectedValue() {
        assertEquals("", new NodeJsCommandInterpreter("").getCommand());
    }

    public void testGetContentWithNullCommandShouldGiveExpectedValue() {
        assertNull(new NodeJsCommandInterpreter(null).getCommand());
    }

    public void testGetFileExtensionShouldGiveExpectedValue() {
        assertEquals(".js", interpreter.getFileExtension());
    }

    public void testGetDescriptorShouldGiveExpectedValue() {
        assertNotNull(descriptor);
        assertTrue(descriptor instanceof Descriptor<?>);
    }

    public void testDescriptorGetDisplayNameShouldGiveExpectedValue() {
        assertEquals("Execute NodeJS script", descriptor.getDisplayName());
    }

    public void testDescriptorGetHelpFileShouldGiveExpectedValue() {
        assertEquals("/plugin/nodejs/help.html", descriptor.getHelpFile());
    }
}