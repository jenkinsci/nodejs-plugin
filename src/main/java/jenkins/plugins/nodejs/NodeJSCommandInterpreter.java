package jenkins.plugins.nodejs;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.tasks.Builder;
import hudson.tasks.CommandInterpreter;
import hudson.util.ArgumentListBuilder;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.Platform;

/**
 * This class executes a JavaScript file using node. The file should contain
 * NodeJS script specified in the job configuration.
 *
 * @author cliffano
 * @author Nikolas Falco
 */
public class NodeJSCommandInterpreter extends CommandInterpreter {
    private static final String JAVASCRIPT_EXT = ".js";

    private final String nodeJSInstallationName;
    private transient String nodeExec; // NOSONAR

    /**
     * Constructs a {@link NodeJSCommandInterpreter} with specified command.
     *
     * @param command
     *            the NodeJS script
     * @param nodeJSInstallationName
     *            the NodeJS label configured in Jenkins
     */
    @DataBoundConstructor
    public NodeJSCommandInterpreter(final String command, final String nodeJSInstallationName) {
        super(command);
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

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        try {
            EnvVars env = build.getEnvironment(listener);

            // get specific installation for the node
            NodeJSInstallation ni = getNodeJS();
            if (ni == null) {
                if (nodeJSInstallationName != null) {
                    throw new AbortException(Messages.NodeJSCommandInterpreter_noInstallationFound(nodeJSInstallationName));
                }
                // use system NodeJS if any, in case let fails later
                nodeExec = (launcher.isUnix() ? Platform.LINUX : Platform.WINDOWS).nodeFileName;
            } else {
                Node node = build.getBuiltOn();
                if (node == null) {
                    throw new AbortException(Messages.NodeJSCommandInterpreter_nodeOffline());
                }
                ni = ni.forNode(node, listener);
                ni = ni.forEnvironment(env);
                ni.buildEnvVars(env);
                nodeExec = ni.getExecutable(launcher);
                if (nodeExec == null) {
                    throw new AbortException(Messages.NodeJSCommandInterpreter_noExecutableFound(ni.getHome()));
                }
            }
        } catch (AbortException e) {
            listener.fatalError(e.getMessage()); // NOSONAR
            return false;
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError(hudson.tasks.Messages.CommandInterpreter_CommandFailed()));
        }

        return super.perform(build, launcher, listener);
    }

    @Override
    public String[] buildCommandLine(FilePath script) {
        if (nodeExec == null) {
            throw new IllegalStateException("Node executable not initialised");
        }

        ArgumentListBuilder args = new ArgumentListBuilder(nodeExec, script.getRemote());
        return args.toCommandArray();
    }

    @Override
    protected String getContents() {
        return getCommand();
    }

    @Override
    protected String getFileExtension() {
        return JAVASCRIPT_EXT;
    }

    public String getNodeJSInstallationName() {
        return nodeJSInstallationName;
    }

    /**
     * Provides builder details for the job configuration page.
     *
     * @author cliffano
     * @author Nikolas Falco
     */
    @Extension
    public static final class NodeJsDescriptor extends Descriptor<Builder> {
        /**
         * Customise the name of this job step.
         *
         * @return the builder name
         */
        @Override
        public String getDisplayName() {
            return Messages.NodeJSCommandInterpreter_displayName();
        }

        /**
         * Return the help file.
         *
         * @return the help file URL path
         */
        @Override
        public String getHelpFile() {
            return "/plugin/nodejs/help.html";
        }

        public NodeJSInstallation[] getInstallations() {
            return NodeJSUtils.getInstallations();
        }

    }

}