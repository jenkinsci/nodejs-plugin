package jenkins.plugins.nodejs.tools;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.plugins.nodejs.Messages;
import net.sf.json.JSONObject;

/**
 * Information about JDK installation.
 *
 * @author fcamblor
 * @author Nikolas Falco
 */
@SuppressWarnings("serial")
public class NodeJSInstallation extends ToolInstallation implements EnvironmentSpecific<NodeJSInstallation>, NodeSpecific<NodeJSInstallation> {

    private final transient Platform platform;

    @DataBoundConstructor
    public NodeJSInstallation(@Nonnull String name, @Nonnull String home, List<? extends ToolProperty<?>> properties) {
        this(name, home, properties, null);
    }

    protected NodeJSInstallation(@Nonnull String name, @Nonnull String home, List<? extends ToolProperty<?>> properties, Platform platform) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
        this.platform = platform;
    }

    /*
     * (non-Javadoc)
     * @see hudson.model.EnvironmentSpecific#forEnvironment(hudson.EnvVars)
     */
    @Override
    public NodeJSInstallation forEnvironment(EnvVars environment) {
        return new NodeJSInstallation(getName(), environment.expand(getHome()), getProperties().toList(), platform);
    }

    /*
     * (non-Javadoc)
     * @see hudson.slaves.NodeSpecific#forNode(hudson.model.Node, hudson.model.TaskListener)
     */
    @Override
    public NodeJSInstallation forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
        return new NodeJSInstallation(getName(), translateFor(node, log), getProperties().toList(), Platform.of(node));
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
     * Gets the executable path of NodeJS on the target system.
     *
     * @return the nodejs executable in the executable system is exists,
     *         {@code null} otherwise.
     * @throws IOException
     *             if something goes wrong
     */
    public String getExecutable() throws IOException {
        File exe = getExeFile(getPlatform());
        if (exe.exists()) {
            return exe.getPath();
        }
        return null;
    }

    private File getExeFile(@Nonnull Platform platform) {
        File bin = new File(getHome(), platform.binFolder);
        return new File(bin, platform.nodeFileName);
    }

    private String getBin() {
        try {
            return new File(getHome(), getPlatform().binFolder).getPath();
        } catch (DetectionFailedException e) {
            throw new RuntimeException(e);
        }
    }

    private Platform getPlatform() throws DetectionFailedException {
        Platform currentPlatform = platform;

        // missed call method forNode
        if (currentPlatform == null) {
            Computer computer = Computer.currentComputer();
            if (computer == null) {
                // pipeline use case
                throw new RuntimeException(Messages.NodeJSBuilders_nodeOffline());
            }

            Node node = computer.getNode();
            if (node == null) {
                throw new RuntimeException(Messages.NodeJSBuilders_nodeOffline());
            }

            currentPlatform = Platform.of(node);
        }

        return currentPlatform;
    }


    @Symbol("nodejs")
    @Extension
    public static class DescriptorImpl extends ToolDescriptor<NodeJSInstallation> {

        public DescriptorImpl() {
            // load installations at Jenkins startup
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.NodeJSInstallation_displayName();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new NodeJSInstaller(null, null, 72));
        }

        /*
         * (non-Javadoc)
         * @see hudson.tools.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
            boolean result = super.configure(req, json);
            /*
             * Invoked when the global configuration page is submitted. If
             * installation are modified programmatically than it's a developer
             * task perform the call to save method on this descriptor.
             */
            save();
            return result;
        }

    }

}