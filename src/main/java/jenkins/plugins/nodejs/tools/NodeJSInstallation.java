/*
 * The MIT License
 *
 * Copyright (c) 2009-2010, Sun Microsystems, Inc., CloudBees, Inc.
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
package jenkins.plugins.nodejs.tools;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.plugins.nodejs.Messages;
import jenkins.plugins.nodejs.NodeJSConstants;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;

/**
 * Information about JDK installation.
 *
 * @author fcamblor
 * @author Nikolas Falco
 */
@SuppressWarnings("serial")
@SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID")
public class NodeJSInstallation extends ToolInstallation implements EnvironmentSpecific<NodeJSInstallation>, NodeSpecific<NodeJSInstallation> {

    @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "calculate at runtime, its value depends on the OS where it run")
    private transient Platform platform;

    @DataBoundConstructor
    public NodeJSInstallation(@Nonnull String name, @Nonnull String home, List<? extends ToolProperty<?>> properties) {
        this(name, home, properties, null);
    }

    protected NodeJSInstallation(@Nonnull String name, @Nonnull String home, List<? extends ToolProperty<?>> properties, Platform platform) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
        this.platform = platform;
    }

    /*
     * (non-Javadoc)
     * @see hudson.model.EnvironmentSpecific#forEnvironment(hudson.EnvVars)
     */
    @Override
    public NodeJSInstallation forEnvironment(EnvVars environment) {
        return new NodeJSInstallation(getName(), environment.expand(getHome()), getProperties().toList(), platform);
    }

    /*
     * (non-Javadoc)
     * @see hudson.slaves.NodeSpecific#forNode(hudson.model.Node, hudson.model.TaskListener)
     */
    @Override
    public NodeJSInstallation forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
        return new NodeJSInstallation(getName(), translateFor(node, log), getProperties().toList(), Platform.of(node));
    }

    /*
     * (non-Javadoc)
     * @see hudson.tools.ToolInstallation#buildEnvVars(hudson.EnvVars)
     */
    @Override
    public void buildEnvVars(EnvVars env) {
        String home = getHome();
        if (home == null) {
            return;
        }
        env.put(NodeJSConstants.ENVVAR_NODEJS_HOME, home);
        env.put(NodeJSConstants.ENVVAR_NODEJS_PATH, getBin());
    }

    /**
     * Gets the executable path of NodeJS on the given target system.
     *
     * @param launcher a way to start processes
     * @return the nodejs executable in the system is exists, {@code null}
     *         otherwise.
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong
     */
    public String getExecutable(final Launcher launcher) throws InterruptedException, IOException {
        // DO NOT REMOVE this callable otherwise paths constructed by File
        // and similar API will be based on the master node O.S.
        return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
            private static final long serialVersionUID = -8509941141741046422L;

            @Override
            public String call() throws IOException {
                Platform currentPlatform = getPlatform();
                File exe = new File(getBin(), currentPlatform.nodeFileName);
                if (exe.exists()) {
                    return exe.getPath();
                }
                return null;
            }
        });
    }

    /**
     * Calculate the NodeJS bin folder based on current Node platform. We can't
     * use {@link Computer#currentComputer()} because it's always null in case of
     * pipeline.
     *
     * @return path of the bin folder for the installation tool in the current
     *         Node.
     */
    private String getBin() {
        Platform currentPlatform = null;
        try {
            currentPlatform = getPlatform();
        } catch (DetectionFailedException e) {
            throw new RuntimeException(e);  // NOSONAR
        }

        String bin = getHome();
        if (!"".equals(currentPlatform.binFolder)) {
            switch (currentPlatform) {
            case WINDOWS:
                bin += '\\' + currentPlatform.binFolder;
                break;
            case LINUX:
            case OSX:
            default:
                bin += '/' + currentPlatform.binFolder;
            }
        }

        return bin;
    }

    private Platform getPlatform() throws DetectionFailedException {
        Platform currentPlatform = platform;

        // missed call method forNode
        if (currentPlatform == null) {
            Computer computer = Computer.currentComputer();
            if (computer != null) {
                Node node = computer.getNode();
                if (node == null) {
                    throw new DetectionFailedException(Messages.NodeJSBuilders_nodeOffline());
                }

                currentPlatform = Platform.of(node);
            } else {
                // pipeline or MasterToSlave use case
                currentPlatform = Platform.current();
            }

            platform = currentPlatform;
        }

        return currentPlatform;
    }


    @Symbol("nodejs")
    @Extension
    public static class DescriptorImpl extends ToolDescriptor<NodeJSInstallation> {

        public DescriptorImpl() {
            // load installations at Jenkins startup
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.NodeJSInstallation_displayName();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new NodeJSInstaller(null, null, 72));
        }

        /*
         * (non-Javadoc)
         * @see hudson.tools.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
            boolean result = super.configure(req, json);
            /*
             * Invoked when the global configuration page is submitted. If
             * installation are modified programmatically than it's a developer
             * task perform the call to save method on this descriptor.
             */
            save();
            return result;
        }

    }

}