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
package hudson.plugins.nodejs.tools;

import hudson.Extension;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Install NodeJS from nodejs.org
 *
 * @author Frédéric Camblor
 * @since 0.2
 */
public class NodeJSInstaller extends DownloadFromUrlInstaller {

    private final String npmPackages;

    @DataBoundConstructor
    public NodeJSInstaller(String id, String npmPackages)    {
        super(id);
        this.npmPackages = npmPackages;
    }


    public String getNpmPackages() {
        return npmPackages;
    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<NodeJSInstaller> {
        public String getDisplayName() {
            return Messages.NodeJSInstaller_DescriptorImpl_displayName();
        }

        @Override
        public List<? extends Installable> getInstallables() throws IOException {
            return super.getInstallables();
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == NodeJSInstallation.class;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(NodeJSInstaller.class.getName());
}