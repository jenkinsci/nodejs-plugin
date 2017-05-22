package jenkins.plugins.nodejs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ConfigFile;
import org.jenkinsci.lib.configprovider.model.ConfigFileManager;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMConfig.NPMConfigProvider;
import jenkins.plugins.nodejs.configfiles.VerifyConfigProviderException;
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

    private static class EnvVarsAdapter extends EnvVars {
        private static final long serialVersionUID = 1L;

        private final transient Context context; // NOSONAR

        public EnvVarsAdapter(@Nonnull Context context) {
            this.context = context;
        }

        @Override
        public String put(String key, String value) {
            context.env(key, value);
            return null; // old value does not exist, just one binding for key
        }

        @Override
        public void override(String key, String value) {
            put(key, value);
        }

    }

    private final String nodeJSInstallationName;
    private final String configId;

    @DataBoundConstructor
    public NodeJSBuildWrapper(String nodeJSInstallationName, String configId) {
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

    public String getNodeJSInstallationName() {
        return nodeJSInstallationName;
    }

    public String getConfigId() {
        return configId;
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
            throw new IOException(Messages.NodeJSBuilders_noInstallationFound(nodeJSInstallationName));
        }
        Computer computer = workspace.toComputer();
        if (computer == null) {
            throw new AbortException(Messages.NodeJSBuilders_nodeOffline());
        }
        Node node = computer.getNode();
        if (node == null) {
            throw new AbortException(Messages.NodeJSBuilders_nodeOffline());
        }
        ni = ni.forNode(node, listener);
        ni = ni.forEnvironment(initialEnvironment);
        ni.buildEnvVars(new EnvVarsAdapter(context));

        EnvVars env = initialEnvironment.overrideAll(context.getEnv());

        // add npmrc config
        if (configId != null) {
            ConfigFile cf = new ConfigFile(configId, null, true);
            FilePath configFile = ConfigFileManager.provisionConfigFile(cf, env, build, workspace, listener, new ArrayList<String>());
            context.env(NodeJSConstants.NPM_USERCONFIG,  configFile.getRemote());
            build.addAction(new CleanTempFilesAction(configFile.getRemote()));
        }
    }


    @Symbol("nodejs")
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

        public Collection<Config> getConfigs() {
            return GlobalConfigFiles.get().getConfigs(NPMConfigProvider.class);
        }

        public FormValidation doCheckConfigId(@CheckForNull @QueryParameter final String configId) {
            NPMConfig config = (NPMConfig) GlobalConfigFiles.get().getById(configId);
            if (config != null) {
                try {
                    config.doVerify();
                } catch (VerifyConfigProviderException e) { // NOSONAR I need only message
                    return FormValidation.error(e.getMessage());
                }
            }
            return FormValidation.ok();
        }

    }

}