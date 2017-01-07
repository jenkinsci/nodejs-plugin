package jenkins.plugins.nodejs.tools.pathresolvers;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.annotation.Nonnull;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Platform;
import hudson.model.Computer;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ReflectionUtils;

//@Issue("JENKINS-14807")
@Extension(ordinal = -200)
public class FixEnvVarEnvironmentContributor extends EnvironmentContributor {

    @Override
    public void buildEnvironmentFor(@SuppressWarnings("rawtypes") @Nonnull Run run, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        Computer c = Computer.currentComputer();
        if (c != null) {
            Field platformField = ReflectionUtils.findField(EnvVars.class, "platform", Platform.class);
            ReflectionUtils.makeAccessible(platformField);
            Platform currentPlatform = (Platform) ReflectionUtils.getField(platformField, envs);
            if (currentPlatform == null) {
                // try to fix value with than one that comes from current computer
                EnvVars remoteEnv = c.getEnvironment();
                Platform computerPlatform = (Platform) ReflectionUtils.getField(platformField, remoteEnv);
                if (computerPlatform != null) {
                    ReflectionUtils.setField(platformField, envs, computerPlatform);
                }
            }
        }
    }
}
