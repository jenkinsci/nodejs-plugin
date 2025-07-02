/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco, Cliffano Subagio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.nodejs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Environment;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.CommandInterpreter;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.ArrayList;
import jenkins.plugins.nodejs.cache.CacheLocationLocator;
import jenkins.plugins.nodejs.cache.DefaultCacheLocationLocator;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.Platform;
import org.jenkinsci.Symbol;
import org.jenkinsci.lib.configprovider.model.ConfigFile;
import org.jenkinsci.lib.configprovider.model.ConfigFileManager;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

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
    private CacheLocationLocator cacheLocationStrategy;

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
        this.cacheLocationStrategy = new DefaultCacheLocationLocator();
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

            FilePath workspace = build.getWorkspace();
            if (workspace != null) {
                // configure cache location
                FilePath cacheLocation = getCacheLocationStrategy().locate(workspace);
                if (cacheLocation != null) {
                    newEnv.put(NodeJSConstants.NPM_CACHE_LOCATION, cacheLocation.getRemote());
                }
            }

            if (configId != null) {
                // add npmrc config
                ConfigFile cf = new ConfigFile(configId, null, true);
                FilePath configFile = ConfigFileManager.provisionConfigFile(cf, env, build, workspace, listener, new ArrayList<String>());
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
            e.printStackTrace(listener.fatalError(Messages.NodeJSCommandInterpreter_commandFailed()));
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

    public CacheLocationLocator getCacheLocationStrategy() {
        return cacheLocationStrategy;
    }

    @DataBoundSetter
    public void setCacheLocationStrategy(CacheLocationLocator cacheLocationStrategy) {
        this.cacheLocationStrategy = cacheLocationStrategy == null ? new DefaultCacheLocationLocator() : cacheLocationStrategy;
    }

    /**
     * Migrate old data, set cacheLocationStrategy
     *
     * @see <a href=
     *      "https://wiki.jenkins-ci.org/display/JENKINS/Hint+on+retaining+backward+compatibility">
     *      Jenkins wiki entry on the subject</a>
     *
     * @return must be always 'this'
     */
    private Object readResolve() {
        // this.cacheLocationStrategy is null if this plugin gets updated from 1.2.9 to 1.3.0 because it would be
        // missing in the xml config in this case. Otherwise it equals the value from xml-config.
        this.setCacheLocationStrategy(this.cacheLocationStrategy); // use null-check in the default setter method
        return this;
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
         * Returns all tools defined in the tool page.
         *
         * @param item context against check permission
         * @return a collection of tools name.
         */
        @RequirePOST
        public ListBoxModel doFillNodeJSInstallationNameItems(@Nullable @AncestorInPath Item item) {
            return NodeJSDescriptorUtils.getNodeJSInstallations(item, true);
        }

        /**
         * Gather all defined npmrc config files.
         *
         * @param context where lookup
         * @return a collection of user npmrc files.
         */
        @RequirePOST
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
        @RequirePOST
        public FormValidation doCheckConfigId(@Nullable @AncestorInPath ItemGroup<?> context, @CheckForNull @QueryParameter final String configId) {
            return NodeJSDescriptorUtils.checkConfig(context, configId);
        }

    }

}
