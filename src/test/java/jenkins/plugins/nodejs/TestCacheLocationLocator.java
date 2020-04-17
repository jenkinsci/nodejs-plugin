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
package jenkins.plugins.nodejs;

import hudson.Extension;
import hudson.FilePath;
import jenkins.plugins.nodejs.cache.CacheLocationLocator;
import jenkins.plugins.nodejs.cache.CacheLocationLocatorDescriptor;

public class TestCacheLocationLocator extends CacheLocationLocator {

    private FilePath location;

    public TestCacheLocationLocator(FilePath location) {
        this.location = location;
    }

    @Override
    public FilePath locate(FilePath workspace) {
        return location;
    }

    @Extension
    public static class DescriptorImpl extends CacheLocationLocatorDescriptor {
        @Override
        public String getDisplayName() {
            return "test cache locator";
        }
    }
}
