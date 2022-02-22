/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco
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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.NodeJSInstallation.DescriptorImpl;

/*package*/final class NodeJSUtils {

    private NodeJSUtils() {
        // default constructor
    }

    /**
     * Gets the NodeJS to invoke, or null to invoke the default one.
     *
     * @param name
     *            the name of NodeJS installation
     * @return a NodeJS installation for the given name if exists, {@code null}
     *         otherwise.
     */
    @Nullable
    public static NodeJSInstallation getNodeJS(@Nullable String name) {
        if (name != null) {
            for (NodeJSInstallation installation : getInstallations()) {
                if (name.equals(installation.getName()))
                    return installation;
            }
        }
        return null;
    }

    /**
     * Get all NodeJS installation defined in Jenkins.
     *
     * @return an array of NodeJS tool installation
     */
    @NonNull
    public static NodeJSInstallation[] getInstallations() {
        DescriptorImpl descriptor = Jenkins.get().getDescriptorByType(NodeJSInstallation.DescriptorImpl.class);
        if (descriptor == null) {
            throw new IllegalStateException("Impossible retrieve NodeJSInstallation descriptor");
        }
        return descriptor.getInstallations();
    }

}