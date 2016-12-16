package jenkins.plugins.nodejs.tools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ListBoxModel;

import java.io.IOException;

import jenkins.plugins.nodejs.NodeJSPlugin;
import jenkins.tasks.SimpleBuildWrapper;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A simple build wrapper that contribute the NodeJS bin path to the PATH
 * environment variable.
 * 
 * @author fcamblor
 * @author Nikolas Falco
 */
public class NpmPackagesBuildWrapper extends SimpleBuildWrapper {

    private final String nodeJSInstallationName;

    @DataBoundConstructor
    public NpmPackagesBuildWrapper(String nodeJSInstallationName){
        this.nodeJSInstallationName = nodeJSInstallationName;
    }

    public String getNodeJSInstallationName() {
        return nodeJSInstallationName;
    }

    /*
     * (non-Javadoc)
     * @see jenkins.tasks.SimpleBuildWrapper#setUp(jenkins.tasks.SimpleBuildWrapper.Context, hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener, hudson.EnvVars)
     */
    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars env) throws IOException, InterruptedException {
        final Computer computer = workspace.toComputer();
        if (computer == null) {
            throw new IllegalStateException("Build computer is null");
        }

        NodeJSInstallation nodeJSInstallation =
            NodeJSPlugin.instance().findInstallationByName(nodeJSInstallationName);

        final Node node = computer.getNode();
        if (node == null) {
            throw new IllegalStateException("Build node is null");
        }

        nodeJSInstallation = nodeJSInstallation.translate(node, env, listener);
        context.env("PATH+NODEJS", nodeJSInstallation.getBinFolder());
    }


    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        /**
         * Return all configured Node JS installations.
         * 
         * @return an array of Node JS installations
         */
        public NodeJSInstallation[] getInstallations() {
            return NodeJSPlugin.instance().getInstallations();
        }

        @Override
        public String getDisplayName() {
            return jenkins.plugins.nodejs.tools.Messages.NpmPackagesBuildWrapper_displayName();
        }

        public ListBoxModel doFillNodeJSInstallationNameItems() {
            final ListBoxModel options = new ListBoxModel();
            for (NodeJSInstallation installation : getInstallations()) {
                options.add(installation.getName(), installation.getName());
            }
            return options;
        }
    }
}
