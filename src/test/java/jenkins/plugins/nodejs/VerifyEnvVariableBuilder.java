package jenkins.plugins.nodejs;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;

abstract class VerifyEnvVariableBuilder extends Builder {
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        verify(env);
        return true;
    }

    public abstract void verify(EnvVars env);

    public static final class FileVerifier extends VerifyEnvVariableBuilder {
        @Override
        public void verify(EnvVars env) {
            String var = NodeJSConstants.NPM_USERCONFIG;
            String value = env.get(var);

            assertTrue("variable " + var + " not set", env.containsKey(var));
            assertNotNull("empty value for environment variable " + var, value);
            assertTrue("file of variable " + var + " does not exists or is not a file", new File(value).isFile());
        }
    }
}