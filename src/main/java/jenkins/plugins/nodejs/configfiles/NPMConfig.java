package jenkins.plugins.nodejs.configfiles;

import hudson.Extension;
import hudson.Util;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.Messages;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A config/provider to handle the special case of a npmrc config file
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public class NPMConfig extends Config {
    private static final long serialVersionUID = 1L;

    private final List<NPMRegistry> registries;

    @DataBoundConstructor
    public NPMConfig(@Nonnull String id, String name, String comment, String content, @Nonnull String providerId, List<NPMRegistry> registries) {
        super(id, Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(comment), Util.fixEmptyAndTrim(content), providerId);
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

    /*
     * (non-Javadoc)
     * @see org.jenkinsci.lib.configprovider.model.Config#getDescriptor()
     */
    @Override
    public ConfigProvider getDescriptor() {
        // boilerplate template
        return (ConfigProvider) Jenkins.getActiveInstance().getDescriptorOrDie(getClass());
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
        public Config newConfig(@Nonnull String configId) {
            return new NPMConfig(configId, "MyNpmrcConfig", "user config", loadTemplateContent(), getProviderId(), null);
        }

        protected String loadTemplateContent() {
            try (InputStream is = this.getClass().getResourceAsStream("template.npmrc")) {
                return IOUtils.toString(is, "UTF-8");
            } catch (IOException e) { // NOSONAR
                return null;
            }
        }

    }
}
