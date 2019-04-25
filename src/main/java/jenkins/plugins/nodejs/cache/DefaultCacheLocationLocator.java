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

import hudson.Extension;
import hudson.FilePath;
import javax.annotation.Nonnull;
import jenkins.plugins.nodejs.Messages;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Uses NPM's default global cache, which is actually {@code ~/.npm} on Unix
 * system or {@code %APP_DATA%\npm-cache} on Windows system.
 */
public class DefaultCacheLocationLocator extends CacheLocationLocator {

    @DataBoundConstructor
    public DefaultCacheLocationLocator() {
    }

    @Override
    public FilePath locate(@Nonnull FilePath workspace) {
        return null;
    }

    @Extension
    @Symbol("default")
    public static class DescriptorImpl extends CacheLocationLocatorDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.DefaultCacheLocationLocator_displayName();
        }
    }

}