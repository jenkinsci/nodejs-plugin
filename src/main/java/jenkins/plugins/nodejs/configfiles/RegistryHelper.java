package jenkins.plugins.nodejs.configfiles;

import static jenkins.plugins.nodejs.NodeJSConstants.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Base64;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;

import hudson.Util;
import hudson.model.Run;
import hudson.util.Secret;

public final class RegistryHelper {

    private final List<NPMRegistry> registries;

    public RegistryHelper(@CheckForNull List<NPMRegistry> registries) {
        this.registries = registries;
    }

    /**
     * Resolves all registry credentials and returns a map paring registry URL
     * to credential.
     *
     * @param build a build being run
     * @return map of registry URL - credential
     */
    public Map<String, StandardUsernameCredentials> resolveCredentials(Run<?, ?> build) {
        Map<String, StandardUsernameCredentials> registry2credential = new HashMap<>();
        for (NPMRegistry registry : registries) {
            String credentialsId = registry.getCredentialsId();
            if (credentialsId != null) {

                // create a domain filter based on registry URL
                final URL registryURL = toURL(registry.getUrl());
                List<DomainRequirement> domainRequirements = Collections.emptyList();
                if (registryURL != null) {
                    domainRequirements = Collections.<DomainRequirement> singletonList(new HostnameRequirement(registryURL.getHost()));
                }

                StandardUsernameCredentials c = CredentialsProvider.findCredentialById(credentialsId, StandardUsernameCredentials.class, build, domainRequirements);
                if (c != null) {
                    registry2credential.put(registry.getUrl(), c);
                }
            }
        }
        return registry2credential;
    }

    /**
     * Fill the npmpc user config with the given registries.
     *
     * @param npmrcContent .npmrc user config
     * @param registry2Credentials the credentials to be inserted into the user
     *        config (key: registry URL, value: Jenkins credentials)
     * @return the updated version of the {@code npmrcContent} with the registry
     *         credentials added
     */
    public String fillRegistry(String npmrcContent, Map<String, StandardUsernameCredentials> registry2Credentials) {
        Npmrc npmrc = new Npmrc();
        npmrc.from(npmrcContent);

        NPMRegistry global = null;

        for (NPMRegistry registry : registries) {
            String authValue = null;
            if (registry2Credentials.containsKey(registry.getUrl())) {
                StandardUsernamePasswordCredentials credentials = (StandardUsernamePasswordCredentials) registry2Credentials.get(registry.getUrl());
                authValue = credentials.getUsername() + ':' + Secret.toString(credentials.getPassword());
                authValue = Base64.encodeBase64String(authValue.getBytes());
            }

            if (registry.isHasScopes()) {
                for (String scope : registry.getScopesAsList()) {
                    // remove protocol from the registry URL
                    String registryPrefix = calculatePrefix(registry.getUrl());

                    // add scoped values to the user config file
                    npmrc.set(compose('@' + scope, NPM_SETTINGS_REGISTRY), registry.getUrl());
                    npmrc.set(compose(registryPrefix, NPM_SETTINGS_ALWAYS_AUTH), authValue != null);
                    if (authValue != null) { // NOSONAR
                        npmrc.set(compose(registryPrefix, NPM_SETTINGS_AUTH), authValue);
                    }
                }
            } else {
                if (global != null) {
                    throw new NpmConfigException("Too many registries configured as global, only one is permitted.\n"
                            + "- " + global.getUrl() + "\n"
                            + "- " + registry.getUrl());
                }
                global = registry;

                // add values to the user config file
                npmrc.set(NPM_SETTINGS_REGISTRY, registry.getUrl());
                npmrc.set(NPM_SETTINGS_ALWAYS_AUTH, authValue != null);
                if (authValue != null) {
                    npmrc.set(NPM_SETTINGS_AUTH, authValue);
                }
            }
        }

        return npmrc.toString();
    }

    @Nonnull
    public String calculatePrefix(@Nonnull final String registryURL) {
        String url = trimSlash(registryURL);
        return "//" + url.substring((toURL(url).getProtocol() + "://").length()) + '/';
    }

    @Nonnull
    public String compose(@Nonnull final String registryPrefix, @Nonnull final String setting) {
        return registryPrefix + ":" + setting;
    }

    @Nonnull
    private String trimSlash(@Nonnull final String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    @CheckForNull
    private static URL toURL(@Nullable final String url) {
        URL result = null;

        String fixedURL = Util.fixEmptyAndTrim(url);
        if (fixedURL != null) {
            try {
                return new URL(fixedURL);
            } catch (MalformedURLException e) {
                // no filter based on hostname
            }
        }

        return result;
    }

}
