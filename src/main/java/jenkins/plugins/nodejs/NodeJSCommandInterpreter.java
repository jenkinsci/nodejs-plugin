package jenkins.plugins.nodejs;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.jenkinsci.Symbol;
import org.jenkinsci.lib.configprovider.model.ConfigFile;
import org.jenkinsci.lib.configprovider.model.ConfigFileManager;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Environment;
import hudson.model.ItemGroup;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.CommandInterpreter;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
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

    private final String nodeJSInstallationName;
    private String configId;

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
        this(command, nodeJSInstallationName, null);
    }

    /**
     * Constructs a {@link NodeJSCommandInterpreter} with specified command.
     *
     * @param command
     *            the NodeJS script
     * @param nodeJSInstallationName
     *            the NodeJS label configured in Jenkins
     * @param configId
     *            the provided Config id
     */
    public NodeJSCommandInterpreter(final String command, final String nodeJSInstallationName, final String configId) {
        super(command);
        this.nodeJSInstallationName = Util.fixEmpty(nodeJSInstallationName);
        this.configId = Util.fixEmpty(configId);
    }

    /**
     * Gets the NodeJS to invoke, or null to invoke the default one.
     *
     * @return a NodeJS installation setup for this job, {@code null} otherwise.
     */
    public NodeJSInstallation getNodeJS() {
        return NodeJSUtils.getNodeJS(nodeJSInstallationName);
    }

    /*
     * (non-Javadoc)
     * @see hudson.tasks.CommandInterpreter#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.TaskListener)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws InterruptedException {
        try {
            EnvVars env = build.getEnvironment(listener);
            EnvVars newEnv = new EnvVars();

            // get specific installation for the node
            NodeJSInstallation ni = getNodeJS();
            if (ni == null) {
                if (nodeJSInstallationName != null) {
                    throw new AbortException(Messages.NodeJSBuilders_noInstallationFound(nodeJSInstallationName));
                }
                // use system NodeJS if any, in case let fails later
                nodeExec = (launcher.isUnix() ? Platform.LINUX : Platform.WINDOWS).nodeFileName;
            } else {
                Node node = build.getBuiltOn();
                if (node == null) {
                    throw new AbortException(Messages.NodeJSBuilders_nodeOffline());
                }

                ni = ni.forNode(node, listener);
                ni = ni.forEnvironment(env);
                String exec = ni.getExecutable(launcher);
                if (exec == null) {
                	listener.fatalError(Messages.NodeJSBuilders_noExecutableFound(ni.getHome()));
                    return false;
                }
                ni.buildEnvVars(newEnv);

                // enhance env with installation environment because is passed to supplyConfig
                env.overrideAll(newEnv);

                nodeExec = ni.getExecutable(launcher);
                if (nodeExec == null) {
                    throw new AbortException(Messages.NodeJSBuilders_noExecutableFound(ni.getHome()));
                }
            }

            if (configId != null) {
                // add npmrc config
                ConfigFile cf = new ConfigFile(configId, null, true);
                FilePath configFile = ConfigFileManager.provisionConfigFile(cf, env, build, build.getWorkspace(), listener, new ArrayList<String>());
                newEnv.put(NodeJSConstants.NPM_USERCONFIG, configFile.getRemote());
                build.addAction(new CleanTempFilesAction(configFile.getRemote()));
            }

            // add an Environment so when the in super class is called build.getEnviroment() gets the enhanced env
            build.getEnvironments().add(Environment.create(newEnv));

        } catch (AbortException e) {
            listener.fatalError(e.getMessage()); // NOSONAR
            return false;
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError(hudson.tasks.Messages.CommandInterpreter_CommandFailed()));
        }

        return internalPerform(build, launcher, listener);
    }

    protected boolean internalPerform(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws InterruptedException {
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
        return NodeJSConstants.JAVASCRIPT_EXT;
    }

    public String getNodeJSInstallationName() {
        return nodeJSInstallationName;
    }

    public String getConfigId() {
        return configId;
    }

    @DataBoundSetter
    public void setConfigId(String configId) {
        this.configId = Util.fixEmpty(configId);
    }

    /**
     * Provides builder details for the job configuration page.
     *
     * @author cliffano
     * @author Nikolas Falco
     */
    @Symbol("nodejsci")
    @Extension
    public static final class NodeJsDescriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }

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

        /**
         * Gather all defined npmrc config files.
         * 
         * @param context where loopup
         * @return a collection of user npmrc files.
         */
        public ListBoxModel doFillConfigIdItems(@AncestorInPath ItemGroup<?> context) {
            return NodeJSDescriptorUtils.getConfigs(context);
        }

        /**
         * Verify that the given configId exists in the given context.
         * 
         * @param context where lookup
         * @param configId the identifier of an npmrc file
         * @return an validation form for the given npmrc file identifier.
         */
        public FormValidation doCheckConfigId(@Nullable @AncestorInPath ItemGroup<?> context, @CheckForNull @QueryParameter final String configId) {
            return NodeJSDescriptorUtils.checkConfig(context, configId);
        }

    }

}