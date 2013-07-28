package hudson.plugins.nodejs.tools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.nodejs.NodeJSPlugin;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author fcamblor
 */
public class NodeJSInstallation extends ToolInstallation
        implements EnvironmentSpecific<NodeJSInstallation>, NodeSpecific<NodeJSInstallation>, Serializable {

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

    public NodeJSInstallation forEnvironment(EnvVars environment) {
        return new NodeJSInstallation(getName(), environment.expand(nodeJSHome), getProperties().toList());
    }

    public NodeJSInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new NodeJSInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<NodeJSInstallation> {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return Messages.installer_displayName();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new NodeJSInstaller(null, ""));
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
