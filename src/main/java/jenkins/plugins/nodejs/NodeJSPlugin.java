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

import hudson.Plugin;
import hudson.model.Items;

import java.io.IOException;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.NodeJSInstallation.DescriptorImpl;

/**
 * @author fcamblor
 * @author Nikolas Falco
 * @deprecated Do not use this anymore. This class will be removed, actually is
 *             kept to migrate persistence.
 */
@Deprecated
public class NodeJSPlugin extends Plugin {

    private NodeJSInstallation[] installations;

    @Override
    public void start() throws Exception {
        super.start();
        Items.XSTREAM2.addCompatibilityAlias("jenkins.plugins.nodejs.tools.NpmPackagesBuildWrapper", NodeJSBuildWrapper.class);
        Items.XSTREAM2.addCompatibilityAlias("jenkins.plugins.nodejs.NodeJsCommandInterpreter", NodeJSCommandInterpreter.class);
        try {
            load();
        } catch (IOException e) { // NOSONAR
            // ignore read XStream errors
        }
    }

    @SuppressFBWarnings("UWF_NULL_FIELD")
    @Override
    public void postInitialize() throws Exception {
        super.postInitialize();
        // If installations have been read in nodejs.xml, let's convert them to
        // the default persistence
        if (installations != null) {
            setInstallations(installations);
            getConfigXml().delete();
            installations = null;
        }
    }

    /**
     * Get all available NodeJS defined installation.
     *
     * @return an array of defined {@link NodeJSInstallation}
     * @deprecated Use {@link NodeJSUtils#getInstallations()} instead of this.
     */
    @Deprecated
    @NonNull
    public NodeJSInstallation[] getInstallations() {
        return NodeJSUtils.getInstallations();
    }

    @Nullable
    public NodeJSInstallation findInstallationByName(@Nullable String name) {
        return NodeJSUtils.getNodeJS(name);
    }

    /**
     * Set the NodeJS installation.
     *
     * @param installations an array of {@link NodeJSInstallation}
     * @deprecated You should not set manually system NodeJS installation, in
     *             case use the standard
     *             {@link Jenkins#getDescriptorByType(Class)
     *             #setInstallations(NodeJSInstallation[])}
     */
    @Deprecated
    public void setInstallations(@NonNull NodeJSInstallation[] installations) {
        DescriptorImpl descriptor = Jenkins.getActiveInstance().getDescriptorByType(NodeJSInstallation.DescriptorImpl.class);
        if (descriptor != null) {
            descriptor.setInstallations(installations != null ? installations : new NodeJSInstallation[0]);
            descriptor.save();
        }
    }

}