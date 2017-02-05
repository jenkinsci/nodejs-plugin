package jenkins.plugins.nodejs;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMRegistry;
import jenkins.plugins.nodejs.configfiles.RegistryHelper;
import jenkins.plugins.nodejs.configfiles.VerifyConfigProviderException;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.NodeJSInstallation.DescriptorImpl;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFileUtil;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;

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
    @Nonnull
    public static NodeJSInstallation[] getInstallations() {
        DescriptorImpl descriptor = Jenkins.getActiveInstance().getDescriptorByType(NodeJSInstallation.DescriptorImpl.class);
        if (descriptor == null) {
            throw new IllegalStateException("Impossible retrieve NodeJSInstallation descriptor");
        }
        return descriptor.getInstallations();
    }

    /**
     * Create a copy of the given configuration in a no accessible folder for
     * the user.
     * <p>
     * This file will be deleted at the end of job also in case of user
     * interruption.
     * </p>
     *
     * @param configId the configuration identification
     * @param build a build being run
     * @param workspace a workspace of the build
     * @param listener a way to report progress
     * @param env the environment variables set at the outset
     * @throws AbortException in case the provided configId is not valid
     */
    @CheckForNull
    public static FilePath supplyConfig(String configId, Run<?, ?> build, FilePath workspace, TaskListener listener, EnvVars env) throws AbortException {
        if (StringUtils.isNotBlank(configId)) {
            Config c = ConfigFiles.getByIdOrNull(build, configId);

            if (c == null) {
                throw new AbortException("this NodeJS build is setup to use a config with id " + configId + " but can not be find");
            } else {
                NPMConfig config;
                if (c instanceof NPMConfig) {
                    config = (NPMConfig) c;
                } else {
                    config = new NPMConfig(c.id, c.name, c.comment, c.content, c.getProviderId(), null);
                }

                listener.getLogger().println("using user config with name " + config.name);
                String fileContent = config.content;

                List<NPMRegistry> registries = config.getRegistries();
                RegistryHelper helper = new RegistryHelper(registries);
                if (!registries.isEmpty()) {
                    listener.getLogger().println("Adding all registry entries");
                    Map<String, StandardUsernameCredentials> registry2Credentials = helper.resolveCredentials(build);
                    fileContent = helper.fillRegistry(fileContent, registry2Credentials);
                }

                try {
                    if (StringUtils.isNotBlank(fileContent)) { // NOSONAR
                        config.doVerify();

                        FilePath workDir = ManagedFileUtil.tempDir(workspace);
                        final FilePath f = workDir.createTextTempFile(".npmrc", "", Util.replaceMacro(fileContent, env), true);
                        listener.getLogger().printf("Created %s", f);

                        build.addAction(new CleanTempFilesAction(f.getRemote()));
                        return f;
                    }
                } catch (VerifyConfigProviderException e) {
                    throw new AbortException("Invalid user config: " + e.getMessage());
                } catch (Exception e) {
                    throw new IllegalStateException("the npmrc user config could not be supplied for the current build", e);
                }
            }
        }

        return null;
    }

}