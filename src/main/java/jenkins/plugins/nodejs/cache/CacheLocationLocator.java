/*
 * The MIT License
 *
 * Copyright (c) 2019, Nikolas Falco
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
package jenkins.plugins.nodejs.cache;

import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Strategy pattern that decides the location of the NPM cache location for a
 * build.
 */
public abstract class CacheLocationLocator extends AbstractDescribableImpl<CacheLocationLocator> implements ExtensionPoint {

    /**
     * Called during the build on the master to determine the location of the
     * local cache location.
     *
     * @param workspace
     *            the workspace file path locator
     * @return null to let NPM uses its default location. Otherwise this must be
     *         located on the same node as described by this path.
     */
    public abstract FilePath locate(@NonNull FilePath workspace);

    @Override
    public CacheLocationLocatorDescriptor getDescriptor() {
        return (CacheLocationLocatorDescriptor) super.getDescriptor();
    }

}