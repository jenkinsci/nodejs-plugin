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
package jenkins.plugins.nodejs.configfiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.common.StandardCredentials;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.plugins.nodejs.Messages;

/**
 * A config/provider to handle the special case of a npmrc config file
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public class NPMConfig extends Config {
    private static final long serialVersionUID = 1L;

    private final List<NPMRegistry> registries;
    private boolean npm9Format = false;

    @DataBoundConstructor
    public NPMConfig(@NonNull String id, String name, String comment, String content, List<NPMRegistry> registries) {
        super(id, Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(comment), Util.fixEmptyAndTrim(content));
        this.registries = registries == null ? new ArrayList<NPMRegistry>(3) : registries;
    }

    public List<NPMRegistry> getRegistries() {
        return registries;
    }

    /**
     * Perform a validation of the configuration.
     * <p>
     * If validation pass then no {@link VerifyConfigProviderException} will be
     * raised.
     *
     * @throws VerifyConfigProviderException
     *             in case this configuration is not valid.
     */
    public void doVerify() throws VerifyConfigProviderException {
        // check if more than registry is setup to be global
        NPMRegistry globalRegistry = null;

        for (NPMRegistry registry : registries) {
            registry.doVerify();

            if (!registry.isHasScopes()) {
                if (globalRegistry != null) {
                    throw new VerifyConfigProviderException(Messages.NPMConfig_verifyTooGlobalRegistry());
                }
                globalRegistry = registry;
            }
        }
    }

    public boolean isNpm9Format() {
        return npm9Format;
    }

    /**
     * Sets if the generated .npmrc format is compatible with NPM version 9.
     *
     * @param npm9Format enable NPM version 9 or not
     */
    @DataBoundSetter
    public void setNpm9Format(boolean npm9Format) {
        this.npm9Format = npm9Format;
    }

    @Extension
    public static class NPMConfigProvider extends AbstractConfigProviderImpl {

        public NPMConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return Messages.NPMConfig_displayName();
        }

        @Override
        public Config newConfig(@NonNull String configId) {
            return new NPMConfig(configId, "MyNpmrcConfig", "user config", loadTemplateContent(), null);
        }

        protected String loadTemplateContent() {
            try (InputStream is = this.getClass().getResourceAsStream("template.npmrc")) {
                return IOUtils.toString(is, "UTF-8");
            } catch (IOException e) { // NOSONAR
                return null;
            }
        }

        @Override
        public String supplyContent(Config configFile, Run<?, ?> build, FilePath workDir, TaskListener listener, List<String> tempFiles) throws IOException {
            String fileContent = configFile.content;
            if (configFile instanceof NPMConfig) {
                NPMConfig config = (NPMConfig) configFile;

                List<NPMRegistry> registries = config.getRegistries();
                RegistryHelper helper = new RegistryHelper(registries);
                if (!registries.isEmpty()) {
                    listener.getLogger().println("Adding all registry entries");
                    Map<String, StandardCredentials> registry2Credentials = helper.resolveCredentials(build);
                    fileContent = helper.fillRegistry(fileContent, registry2Credentials, config.npm9Format);
                }

                try {
                    if (StringUtils.isNotBlank(fileContent)) { // NOSONAR
                        config.doVerify();
                    }
                } catch (VerifyConfigProviderException e) {
                    throw new AbortException("Invalid user config: " + e.getMessage());
                }
            }
            return fileContent;
        }

        @Override
        public @NonNull List<String> getSensitiveContentForMasking(Config configFile, Run<?, ?> build) {
            List<String> sensitiveContent = new ArrayList<>();
            if (configFile instanceof NPMConfig) {
                NPMConfig config = (NPMConfig) configFile;
                List<NPMRegistry> registries = config.getRegistries();
                if (!registries.isEmpty()) {
                    RegistryHelper helper = new RegistryHelper(registries);
                    sensitiveContent = helper.secretsForMasking(build);
                }
            }
            return sensitiveContent;
        }
    }
}
