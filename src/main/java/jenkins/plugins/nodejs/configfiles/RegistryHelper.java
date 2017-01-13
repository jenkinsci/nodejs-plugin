package jenkins.plugins.nodejs.configfiles;

import hudson.Util;
import hudson.model.Run;
import hudson.util.Secret;

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
            final URL registryURL = toURL(registry.getUrl());

            List<DomainRequirement> domainRequirements = Collections.emptyList();
            if (registryURL != null) {
                domainRequirements = Collections.<DomainRequirement> singletonList(new HostnameRequirement(registryURL.getHost()));
            }

            String credentialsId = registry.getCredentialsId();
            if (credentialsId != null) {
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
     * @param registries a list of registry to insert into the user config
     * @param registry2Credentials the credentials to be inserted into the user
     *        config (key: registry URL, value: Jenkins credentials)
     * @return the updated version of the {@code npmrcContent} with the registry
     *         credentials added
     */
    public String fillRegistry(String npmrcContent, Map<String, StandardUsernameCredentials> registry2Credentials) {
        Npmrc npmrc = new Npmrc();
        npmrc.from(npmrcContent);

        for (NPMRegistry registry : registries) {
            String registryValue = trimSlash(registry.getUrl());

            String authValue = null;
            if (registry2Credentials.containsKey(registry.getUrl())) {
                StandardUsernamePasswordCredentials credentials = (StandardUsernamePasswordCredentials) registry2Credentials.get(registry.getUrl());
                authValue = credentials.getUsername() + ':' + Secret.toString(credentials.getPassword());
                authValue = Base64.encodeBase64String(authValue.getBytes());
            }

            if (registry.isHasScopes()) {
                for (String scope : registry.getScopesAsList()) {
                    // remove protocol from the registry URL
                    String registryPrefix = "//" + registry.getUrl().substring((toURL(registryValue).getProtocol() + "://").length()) + '/';

                    // add scoped values to the user config file
                    npmrc.set('@' + scope + ":registry", registryValue);
                    npmrc.set(registryPrefix + ":always-auth", authValue != null);
                    if (authValue != null) { // NOSONAR
                        npmrc.set(registryPrefix + ":_auth", authValue);
                    }
                }
            } else {
                // add values to the user config file
                npmrc.set("registry", registryValue);
                npmrc.set("always-auth", authValue != null);
                if (authValue != null) {
                    npmrc.set("_auth", authValue);
                }
            }
        }

        return npmrc.toString();
    }

    @Nonnull
    private String trimSlash(@Nonnull final String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    @CheckForNull
    private URL toURL(@Nullable final String url) {
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
