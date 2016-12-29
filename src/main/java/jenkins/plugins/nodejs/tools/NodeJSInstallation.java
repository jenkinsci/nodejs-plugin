package jenkins.plugins.nodejs.tools;

import hudson.*;
import hudson.model.Computer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.SlaveComputer;
import jenkins.plugins.nodejs.NodeJSPlugin;
import hudson.remoting.Callable;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * @author fcamblor
 */
public class NodeJSInstallation extends ToolInstallation
        implements EnvironmentSpecific<NodeJSInstallation>, NodeSpecific<NodeJSInstallation>, Serializable {

    private static final String WINDOWS_NODEJS_COMMAND = "node.exe";
    private static final String UNIX_NODEJS_COMMAND = "node";

    private final String nodeJSHome;
    private Platform platform;

    @DataBoundConstructor
    public NodeJSInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, launderHome(home), properties);
        this.nodeJSHome = super.getHome();
    }

    public NodeJSInstallation(String name, String home, List<? extends ToolProperty<?>> properties, Platform platform) {
        this(name, home, properties);
        this.platform = platform;
    }

    private static String launderHome(String home) {
        if (home.endsWith("/") || home.endsWith("\\")) {
            // see https://issues.apache.org/bugzilla/show_bug.cgi?id=26947
            // Ant doesn't like the trailing slash, especially on Windows
            return home.substring(0, home.length() - 1);
        } else {
            return home;
        }
    }

    @Override
    public String getHome() {
        if (nodeJSHome != null) {
            return nodeJSHome;
        }
        return super.getHome();
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        String home = getHome();
        if (home == null) {
            return;
        }
        env.put("PATH+NODEJS", home + "/bin");
    }

    /*
     * (non-Javadoc)
     * @see hudson.tools.ToolInstallation#translate(hudson.model.Node, hudson.EnvVars, hudson.model.TaskListener)
     */
    @Override
    public NodeJSInstallation translate(@Nonnull Node node, EnvVars envs, TaskListener listener) throws IOException, InterruptedException {
        return (NodeJSInstallation) super.translate(node, envs, listener);
    }

    /*
     * (non-Javadoc)
     * @see hudson.model.EnvironmentSpecific#forEnvironment(hudson.EnvVars)
     */
    @Override
    public NodeJSInstallation forEnvironment(EnvVars environment) {
        return new NodeJSInstallation(getName(), environment.expand(nodeJSHome), getProperties().toList(), platform);
    }

    /*
     * (non-Javadoc)
     * @see hudson.slaves.NodeSpecific#forNode(hudson.model.Node, hudson.model.TaskListener)
     */
    @Override
    public NodeJSInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new NodeJSInstallation(getName(), translateFor(node, log), getProperties().toList(), Platform.of(node));
    }

    public String getExecutable(final Launcher launcher) throws InterruptedException, IOException {
        final File exe = getExeFile();
        return launcher.getChannel().call(new CheckNodeExecutable(exe));
    }

    private File getExeFile() {
        return new File(getBinFolder(), platform.nodeFileName);
    }

    protected String getBinFolder() {
        return new File(getHome(), platform.binFolder).getAbsolutePath();
    }


    @Symbol("nodejs")
    @Extension
    public static class DescriptorImpl extends ToolDescriptor<NodeJSInstallation> {

        public DescriptorImpl() {
            // default constructor
        }

        @Override
        public String getDisplayName() {
            return jenkins.plugins.nodejs.tools.Messages.installer_displayName();
        }

        // Persistence is done by NodeJSPlugin

        @Override
        public NodeJSInstallation[] getInstallations() {
            return NodeJSPlugin.instance().getInstallations();
        }

        @Override
        public void setInstallations(NodeJSInstallation... installations) {
            NodeJSPlugin.instance().setInstallations(installations);
        }

    }

    private static class CheckNodeExecutable extends MasterToSlaveCallable<String, IOException> {
        private static final long serialVersionUID = 1L;
        private final File exe;

        public CheckNodeExecutable(File exe) {
            this.exe = exe;
        }

        @Override
        public String call() {
            if (exe.exists()) {
                return exe.getPath();
            }
            return null;
        }
    }
}
