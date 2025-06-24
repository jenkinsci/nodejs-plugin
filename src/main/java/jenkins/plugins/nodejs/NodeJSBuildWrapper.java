/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco, Frédéric Camblor
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
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jenkins.plugins.nodejs.cache.CacheLocationLocator;
import jenkins.plugins.nodejs.cache.DefaultCacheLocationLocator;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.tasks.SimpleBuildWrapper;
import org.jenkinsci.Symbol;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ConfigFile;
import org.jenkinsci.lib.configprovider.model.ConfigFileManager;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.credentialsbinding.masking.SecretPatterns;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

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

        public EnvVarsAdapter(@NonNull Context context) {
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
    private String configId;
    private CacheLocationLocator cacheLocationStrategy;

    @DataBoundConstructor
    public NodeJSBuildWrapper(String nodeJSInstallationName) {
        this(nodeJSInstallationName, null);
    }

    public NodeJSBuildWrapper(String nodeJSInstallationName, String configId) {
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
        String exec = ni.getExecutable(launcher);
        if (exec == null) {
            throw new AbortException(Messages.NodeJSBuilders_noExecutableFound(ni.getHome()));
        }
        ni.buildEnvVars(new EnvVarsAdapter(context));

        EnvVars env = initialEnvironment.overrideAll(context.getEnv());

        // configure cache location
        FilePath cacheLocation = cacheLocationStrategy.locate(workspace);
        if (cacheLocation != null) {
            context.env(NodeJSConstants.NPM_CACHE_LOCATION, cacheLocation.getRemote());
        }

        // add npmrc config
        if (configId != null) {
            ConfigFile cf = new ConfigFile(configId, null, true);
            FilePath configFile = ConfigFileManager.provisionConfigFile(cf, env, build, workspace, listener, new ArrayList<String>());
            context.env(NodeJSConstants.NPM_USERCONFIG, configFile.getRemote());
            context.setDisposer(new TempFileCleaner(Arrays.asList(configFile.getRemote())));
        }
    }

    @Override
    public ConsoleLogFilter createLoggerDecorator(@NonNull Run<?, ?> build) {
        if (configId != null) {
            Config conf = ConfigFiles.getByIdOrNull(build, configId);
            List<String> sensitiveContentForMasking = conf.getProvider().getSensitiveContentForMasking(conf, build);
            if (!sensitiveContentForMasking.isEmpty()) {
                return new SecretFilter(sensitiveContentForMasking, build.getCharset());
            }
        }
        return null;
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

    private static class TempFileCleaner extends Disposer {
        private final static Logger LOGGER = Logger.getLogger(TempFileCleaner.class.getName());

        private static final long serialVersionUID = 1;

        private final List<String> tempFiles;

        TempFileCleaner(List<String> tempFiles) {
            this.tempFiles = tempFiles;
        }

        @Override
        public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            listener.getLogger().println("Deleting " + tempFiles.size() + " temporary files");
            for (String tempFile : tempFiles) {
                LOGGER.log(Level.FINE, "Delete: {0}", new Object[]{tempFile});
                new FilePath(workspace, tempFile).delete();
            }
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

        /**
         * Returns all tools defined in the tool page.
         *
         * @param item context against check permission
         * @return a collection of tools name.
         */
        @POST
        public ListBoxModel doFillNodeJSInstallationNameItems(@Nullable @AncestorInPath Item item) {
            return NodeJSDescriptorUtils.getNodeJSInstallations(item, false);
        }

        /**
         * Gather all defined npmrc config files.
         *
         * @param context where lookup
         * @return a collection of user npmrc files.
         */
        @POST
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
        public FormValidation doCheckConfigId(@Nullable @AncestorInPath ItemGroup<?> context,
                                              @CheckForNull @QueryParameter final String configId) {
            return NodeJSDescriptorUtils.checkConfig(context, configId);
        }

    }

    private static final class SecretFilter extends ConsoleLogFilter implements Serializable {

        private static final long serialVersionUID = 1;

        private Secret pattern;
        private String charset;

        SecretFilter(Collection<String> secrets, Charset cs) {
            pattern = Secret.fromString(SecretPatterns.getAggregateSecretPattern(secrets).pattern());
            charset = cs.name();
        }

        @Override
        public OutputStream decorateLogger(@SuppressWarnings("rawtypes") Run build, OutputStream logger) {
            return new SecretPatterns.MaskingOutputStream(logger, () -> Pattern.compile(pattern.getPlainText()), charset);
        }

    }

}
