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
     * The user name used to login to the scoped registry.
     */
    public static final String NPM_SETTINGS_USER = "username";
    /**
     * The authentication base64 string &gt;PASSWORD&lt; used to
     * login to the scoped registry.
     */
    public static final String NPM_SETTINGS_PASSWORD = "_password";

}