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

    @DataBoundConstructor
    public NodeJSInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, launderHome(home), properties);
        this.nodeJSHome = super.getHome();
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
        return new NodeJSInstallation(getName(), environment.expand(nodeJSHome), getProperties().toList());
    }

    public NodeJSInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new NodeJSInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    public String getExecutable(Launcher launcher) throws InterruptedException, IOException {
        return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
            private static final long serialVersionUID = 1L;

            public String call() throws IOException {
                File exe = getExeFile();
                if (exe.exists()) {
                    return exe.getPath();
                }
                return null;
            }
        });
    }

    private File getExeFile() {
        String execName = (Functions.isWindows()) ? WINDOWS_NODEJS_COMMAND : UNIX_NODEJS_COMMAND;
        String nodeJSHome = Util.replaceMacro(this.nodeJSHome, EnvVars.masterEnvVars);
        return new File(nodeJSHome, (Functions.isWindows() ? "":"bin/") + execName);
    }

    String getBinFolder() {
        final Computer computer = Computer.currentComputer();

        // TODO post 1.624 use Computer#isUnix
        boolean unix = !Functions.isWindows();
        if (computer instanceof SlaveComputer)
            unix = ((SlaveComputer) computer).isUnix();
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
}
