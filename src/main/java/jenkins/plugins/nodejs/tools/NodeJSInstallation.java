package jenkins.plugins.nodejs.tools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import jenkins.plugins.nodejs.Messages;
import jenkins.security.MasterToSlaveCallable;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Information about JDK installation.
 *
 * @author fcamblor
 * @author Nikolas Falco
 */
@SuppressWarnings("serial")
public class NodeJSInstallation extends ToolInstallation implements EnvironmentSpecific<NodeJSInstallation>, NodeSpecific<NodeJSInstallation> {

    @DataBoundConstructor
    public NodeJSInstallation(@Nonnull String name, @Nonnull String home, List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
    }

    /*
     * (non-Javadoc)
     * @see hudson.model.EnvironmentSpecific#forEnvironment(hudson.EnvVars)
     */
    @Override
    public NodeJSInstallation forEnvironment(EnvVars environment) {
        return new NodeJSInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    /*
     * (non-Javadoc)
     * @see hudson.slaves.NodeSpecific#forNode(hudson.model.Node, hudson.model.TaskListener)
     */
    @Override
    public NodeJSInstallation forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
        return new NodeJSInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    /*
     * (non-Javadoc)
     * @see hudson.tools.ToolInstallation#buildEnvVars(hudson.EnvVars)
     */
    @Override
    public void buildEnvVars(EnvVars env) {
        String home = getHome();
        if (home == null) {
            return;
        }
        env.put("NODEJS_HOME", home);
        env.override("PATH+NODEJS", getBin());
    }

    /**
     * Gets the executable path of NodeJS on the given target system.
     *
     * @param launcher a way to start processes
     * @return the nodejs executable in the system is exists, {@code null}
     *         otherwise.
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong
     */
    public String getExecutable(final Launcher launcher) throws InterruptedException, IOException {
        return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
            private static final long serialVersionUID = -8509941141741046422L;

            @Override
            public String call() throws IOException {
                Node node = Computer.currentComputer().getNode();
                if (node != null) {
                    final Platform platform = Platform.of(node);
                    File exe = getExeFile(platform);
                    if (exe.exists()) {
                        return exe.getPath();
                    }
                }
                return null;
            }
        });
    }

    private File getExeFile(@Nonnull Platform platform) {
        File bin = new File(getHome(), platform.binFolder);
        return new File(bin, platform.nodeFileName);
    }

    private String getBin() {
        // TODO improve the platform test case
        Boolean isUnix = Computer.currentComputer().isUnix(); // findbugs ... what a nut!
        return new File(getHome(), (isUnix == null || isUnix ? Platform.LINUX : Platform.WINDOWS).binFolder).getPath();
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<NodeJSInstallation> {

        @Override
        public String getDisplayName() {
            return Messages.NodeJSInstallation_displayName();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new NodeJSInstaller(null, null, 72));
        }

    }

}