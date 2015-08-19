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
    private boolean unix;

    @DataBoundConstructor
    public NodeJSInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, launderHome(home), properties);
        this.nodeJSHome = super.getHome();
    }

    public NodeJSInstallation(String name, String home, List<? extends ToolProperty<?>> properties, boolean unix) {
        this(name, home, properties);
        this.unix = unix;
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

    public NodeJSInstallation translate(@Nonnull Node node, EnvVars envs, TaskListener listener) throws IOException, InterruptedException {
        return (NodeJSInstallation) super.translate(node, envs, listener);
    }

    public NodeJSInstallation forEnvironment(EnvVars environment) {
        return new NodeJSInstallation(getName(), environment.expand(nodeJSHome), getProperties().toList(), unix);
    }

    public NodeJSInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        Computer computer = node.toComputer();
        // TODO post 1.624 use Computer#isUnix
        if (computer instanceof SlaveComputer)
            unix = ((SlaveComputer) computer).isUnix();
        else
            unix = !Functions.isWindows();

        return new NodeJSInstallation(getName(), translateFor(node, log), getProperties().toList(), unix);
    }

    public String getExecutable(final Launcher launcher) throws InterruptedException, IOException {

        final File exe = getExeFile();
        return launcher.getChannel().call(new CheckNodeExecutable(exe));
    }

    private File getExeFile() {
        String execName = unix ? UNIX_NODEJS_COMMAND : WINDOWS_NODEJS_COMMAND;
        return new File(getBinFolder(), execName);
    }

    protected String getBinFolder() {
        return unix ? getHome()+"/bin" : getHome();
    }


    @Extension
    public static class DescriptorImpl extends ToolDescriptor<NodeJSInstallation> {

        public DescriptorImpl() {
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

        public String call() throws IOException {
            if (exe.exists()) {
                return exe.getPath();
            }
            return null;
        }
    }
}
