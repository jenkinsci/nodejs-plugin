package jenkins.plugins.nodejs;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.tasks.SimpleBuildWrapper;

/**
 * A simple build wrapper that contribute the NodeJS bin path to the PATH
 * environment variable.
 *
 * @author fcamblor
 * @author Nikolas Falco
 */
public class NodeJSBuildWrapper extends SimpleBuildWrapper {

    @SuppressWarnings("serial")
    private class EnvVarsAdapter extends EnvVars { // NOSONAR
        private final transient Context context;

        public EnvVarsAdapter(@Nonnull Context context) {
            this.context = context;
        }

        @Override
        public String put(String key, String value) {
            context.env(key, value);
            return null;
        }

    }

    private final String nodeJSInstallationName;

    @DataBoundConstructor
    public NodeJSBuildWrapper(String nodeJSInstallationName){
        this.nodeJSInstallationName = Util.fixEmpty(nodeJSInstallationName);
    }

    /**
     * Gets the NodeJS to invoke, or null to invoke the default one.
     *
     * @return a NodeJS installation setup for this job, {@code null} otherwise.
     */
    public NodeJSInstallation getNodeJS() {
        return NodeJSUtils.getNodeJS(nodeJSInstallationName);
    }

    public String getNodeJSInstallationName() {
        return nodeJSInstallationName;
    }

    /*
     * (non-Javadoc)
     * @see jenkins.tasks.SimpleBuildWrapper#setUp(jenkins.tasks.SimpleBuildWrapper.Context, hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener, hudson.EnvVars)
     */
    @Override
    public void setUp(final Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
        // get specific installation for the node
        NodeJSInstallation ni = getNodeJS();
        if (ni == null) {
            throw new IOException(Messages.NodeJSCommandInterpreter_noInstallationFound(nodeJSInstallationName));
        }
        Node node = workspace.toComputer().getNode();
        if (node == null) {
            throw new AbortException(Messages.NodeJSCommandInterpreter_nodeOffline());
        }
        ni = ni.forNode(node, listener);
        ni = ni.forEnvironment(initialEnvironment);
        ni.buildEnvVars(new EnvVarsAdapter(context));
    }


    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.NodeJSBuildWrapper_displayName();
        }

        public NodeJSInstallation[] getInstallations() {
            return NodeJSUtils.getInstallations();
        }

    }

}