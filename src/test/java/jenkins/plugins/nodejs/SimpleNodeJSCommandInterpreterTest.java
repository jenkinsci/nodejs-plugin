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

import hudson.FilePath;
import hudson.model.Descriptor;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import hudson.tasks.Builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@WithJenkins
class SimpleNodeJSCommandInterpreterTest {

    private static final String COMMAND = "var sys = require('sys'); sys.puts('build number: ' + process.env['BUILD_NUMBER']);";

    private NodeJSCommandInterpreter interpreter;
    private Descriptor<Builder> descriptor;
    private NodeJSInstallation installation;

    @TempDir
    private File tempFolder;

    @BeforeEach
    void setUp() {
        installation = new NodeJSInstallation("11.0.0", "", Collections.emptyList());
        interpreter = new NodeJSCommandInterpreter(COMMAND, installation.getName(), null);
        descriptor = new NodeJSCommandInterpreter.NodeJsDescriptor();
    }

    @Test
    void testGetContentsShouldGiveExpectedValue() {
        assertThat(interpreter.getCommand()).isEqualTo(COMMAND);
    }

    @Test
    void testGetContentWithEmptyCommandShouldGiveExpectedValue() {
        assertThat(new NodeJSCommandInterpreter("", installation.getName(), null).getCommand()).isEmpty();
    }

    @Test
    void testGetContentWithNullCommandShouldGiveExpectedValue() {
        assertThat(new NodeJSCommandInterpreter(null, installation.getName(), null).getCommand()).isNull();
    }

    @Test
    void testGetFileExtensionShouldGiveExpectedValue() throws IOException, InterruptedException {
        assertThat(interpreter.createScriptFile(new FilePath(newFolder(tempFolder, "junit"))).getName()).endsWith(".js");
    }

    @Test
    void testGetDescriptorShouldGiveExpectedValue() {
        assertThat(descriptor)
                .isNotNull()
                .isInstanceOf(Descriptor.class);
    }

    @Test
    void testDescriptorGetDisplayNameShouldGiveExpectedValue() {
        assertThat(descriptor.getDisplayName()).isEqualTo(Messages.NodeJSCommandInterpreter_displayName());
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