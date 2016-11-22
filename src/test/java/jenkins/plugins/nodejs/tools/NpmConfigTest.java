package jenkins.plugins.nodejs.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NpmConfigTest {

    private File file;

    @Before
    public void setUp() throws IOException {
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream("npmrc.config");
            file = File.createTempFile("npmrc", ".tmp");
            hudson.util.IOUtils.copy(is, file);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @After
    public void tearDown() {
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }

    @Test
    public void testLoad() throws Exception {
        NpmConfig npmrc = NpmConfig.load(file);
        assertTrue(npmrc.containsKey("always-auth"));
        assertEquals("true", npmrc.get("always-auth"));
        assertEquals("\"/var/lib/jenkins/tools/jenkins.plugins.nodejs.tools.NodeJSInstallation/Node_6.x\"", npmrc.get("prefix"));
    }

    @Test
    public void testAvoidParseError() throws Exception {
        NpmConfig npmrc = NpmConfig.load(file);
        assertFalse(npmrc.containsKey("browser"));
    }

    @Test
    public void testSave() throws Exception {
        String testKey = "test";
        String testValue = "value";

        NpmConfig npmrc = NpmConfig.load(file);
        npmrc.set(testKey, testValue);
        npmrc.save();

        // reload content
        npmrc = NpmConfig.load(file);
        assertTrue(npmrc.containsKey(testKey));
        assertEquals(testValue, npmrc.get(testKey));
    }

    @Test
    public void testCommandAtLast() throws Exception {
        String comment = "test comment";

        NpmConfig npmrc = NpmConfig.load(file);
        npmrc.addComment(comment);
        npmrc.save();

        List<String> lines = IOUtils.readLines(new FileInputStream(file), "UTF-8");
        assertEquals(';' + comment, lines.get(lines.size() - 1));
    }

}