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
package jenkins.plugins.nodejs.tools;

import jenkins.plugins.nodejs.tools.pathresolvers.LatestInstallerPathResolver;
import hudson.tools.DownloadFromUrlInstaller;

/**
 * Contract to resolve parts of an URL path given some specific inputs.
 *
 * @author fcamblor
 * @author Nikolas Falco
 */
public interface InstallerPathResolver {
	/**
	 * Resolve the URL path for the given parameters.
	 *
	 * @param version
	 *            string version of an installable unit
	 * @param platform
	 *            of the node where this installable is designed
	 * @param cpu
	 *            of the node where this installable is designed
	 * @return the relative path URL for the given specifics
	 */
    String resolvePathFor(String version, Platform platform, CPU cpu);

    /**
     * Factory that return lookup for an implementation of {@link InstallerPathResolver}.
     */
    public static class Factory {
		/**
		 * Return an implementation adapt for the given installable.
		 *
		 * @param installable an installable
		 * @return an instance of {@link InstallerPathResolver}
		 * @throws IllegalArgumentException
		 *             in case the given installable is not supported.
		 */
        public static InstallerPathResolver findResolverFor(DownloadFromUrlInstaller.Installable installable){
            if(isVersionBlacklisted(installable.id)){
                throw new IllegalArgumentException("Provided version ("+installable.id+") installer structure not (yet) supported !");
            } else {
                return new LatestInstallerPathResolver();
            }
        }

        public static boolean isVersionBlacklisted(String version){
            NodeJSVersion nodeJSVersion = NodeJSVersion.parseVersion(version);
            return new NodeJSVersionRange("[0, 0.8.6)").includes(nodeJSVersion) || NodeJSVersion.parseVersion("0.9.0").equals(nodeJSVersion);
        }
    }
}
