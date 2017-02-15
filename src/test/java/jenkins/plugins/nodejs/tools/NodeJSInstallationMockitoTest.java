package jenkins.plugins.nodejs.tools;

import static jenkins.plugins.nodejs.NodeJSConstants.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.EnvVars;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NodeJSInstallation.class)
public class NodeJSInstallationMockitoTest {

    /**
     * Ensure the use of {@link EnvVars#put(String, String)} instead
     * {@code EnvVars#override(String, String)} to respect the
     * {@link ToolInstallation#buildEnvVars(EnvVars)) API documentation.
     * <p>
     * A lot stuff rely on that logic.
     */
    @Issue("JENKINS-41947")
    @Test
    public void test_installer_environment() throws Exception {
        String nodeJSHome = "/home/nodejs";
        String bin = nodeJSHome + "/bin";

        NodeJSInstallation installer = PowerMockito.spy(new NodeJSInstallation("test", nodeJSHome, null));
        PowerMockito.doReturn(bin).when(installer, "getBin");

        EnvVars env = PowerMockito.spy(new EnvVars());
        installer.buildEnvVars(env);
        Mockito.verify(env, Mockito.never()).override(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(env, Mockito.never()).overrideAll(Mockito.<String, String>anyMap());

        assertEquals("Unexpected value for " + ENVVAR_NODEJS_HOME, nodeJSHome, env.get(ENVVAR_NODEJS_HOME));
        assertEquals("Unexpected value for " + ENVVAR_NODEJS_PATH, bin, env.get(ENVVAR_NODEJS_PATH));
        assertNull("PATH variable should not appear in this environment", env.get("PATH"));
    }

}