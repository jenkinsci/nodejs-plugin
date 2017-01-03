package jenkins.plugins.nodejs;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;

import javax.annotation.Nonnull;

import jenkins.plugins.nodejs.tools.Messages;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.tasks.SimpleBuildWrapper;

import org.kohsuke.stapler.DataBoundConstructor;

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
        this.nodeJSInstallationName = nodeJSInstallationName;
    }

    /**
     * Gets the NodeJS to invoke, or null to invoke the default one.
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
            throw new IOException(Messages.NodeJsCommandInterpreter_noInstallation(nodeJSInstallationName));
        }
        ni = ni.forNode(workspace.toComputer().getNode(), listener); // NOSONAR
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
            return jenkins.plugins.nodejs.tools.Messages.NpmPackagesBuildWrapper_displayName();
        }

        public NodeJSInstallation[] getInstallations() {
            return NodeJSUtils.getInstallations();
        }

    }

}