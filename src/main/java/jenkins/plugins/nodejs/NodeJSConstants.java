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

public final class NodeJSConstants {

    private NodeJSConstants() {
        // constructor
    }

    /**
     * Default extension for javascript file.
     */
    public static final String JAVASCRIPT_EXT = ".js";

    /**
     * Default NPM registry.
     */
    public static final String DEFAULT_NPM_REGISTRY = "registry.npmjs.org";

    /**
     * The name of environment variable that point to the NodeJS installation
     * home.
     */
    public static final String ENVVAR_NODEJS_HOME = "NODEJS_HOME";

    /**
     * The name of environment variable that contribute the PATH value.
     */
    public static final String ENVVAR_NODEJS_PATH = "PATH+NODEJS";

    /**
     * The location of NPM cache.
     */
    public static final String NPM_CACHE_LOCATION = "npm_config_cache";

    /**
     * The location of user-level configuration settings.
     */
    public static final String NPM_USERCONFIG = "npm_config_userconfig";

    /**
     * Force npm to always require authentication when accessing the registry,
     * even for GET requests.
     * <p>
     * Default: false<br>
     * Type: Boolean
     * </p>
     */
    public static final String NPM_SETTINGS_ALWAYS_AUTH = "always-auth";
    /**
     * The base URL of the npm package registry.
     * <p>
     * Default: https://registry.npmjs.org/<br>
     * Type: url
     * </p>
     */
    public static final String NPM_SETTINGS_REGISTRY = "registry";
    /**
     * The authentication base64 string &gt;USER&lt;:&gt;PASSWORD&lt; used to
     * login to the global registry.
     */
    public static final String NPM_SETTINGS_AUTH = "_auth";
    /**
     * The authentication token used to login to the global registry or scoped
     * registry.
     */
    public static final String NPM_SETTINGS_AUTHTOKEN = "_authToken";
    /**
     * The user name used to login to the scoped registry.
     */
    public static final String NPM_SETTINGS_USER = "username";
    /**
     * The authentication base64 string &gt;PASSWORD&lt; used to
     * login to the scoped registry.
     */
    public static final String NPM_SETTINGS_PASSWORD = "_password";

}
