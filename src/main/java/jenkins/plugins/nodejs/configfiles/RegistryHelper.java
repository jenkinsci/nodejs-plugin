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

import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_ALWAYS_AUTH;
import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_AUTH;
import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_AUTHTOKEN;
import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_PASSWORD;
import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_REGISTRY;
import static jenkins.plugins.nodejs.NodeJSConstants.NPM_SETTINGS_USER;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Util;
import hudson.model.Run;
import hudson.util.Secret;

/**
 * Helper to fill properly credentials in the the user configuration file.
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public final class RegistryHelper {

    private final Collection<NPMRegistry> registries;

    public RegistryHelper(@CheckForNull Collection<NPMRegistry> registries) {
        this.registries = registries;
    }

    /**
     * Resolves all registry credentials and returns a map paring registry URL
     * to credential.
     *
     * @param build a build being run
     * @return map of registry URL - credential
     */
    public Map<String, StandardCredentials> resolveCredentials(Run<?, ?> build) {
        Map<String, StandardCredentials> registry2credential = new HashMap<>();
        for (NPMRegistry registry : registries) {
            String credentialsId = registry.getCredentialsId();
            if (credentialsId != null) {

                // create a domain filter based on registry URL
                final URL registryURL = toURL(registry.getUrl());
                List<DomainRequirement> domainRequirements = Collections.emptyList();
                if (registryURL != null) {
                    domainRequirements = URIRequirementBuilder.fromUri(registry.getUrl()).build();
                }

                StandardCredentials c = CredentialsProvider.findCredentialById(credentialsId, StandardCredentials.class, build, domainRequirements);
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
    public String fillRegistry(String npmrcContent, Map<String, StandardCredentials> registry2Credentials) {
        return fillRegistry(npmrcContent, registry2Credentials, false);
    }

    /**
     * Fill the npmpc user config with the given registries.
     *
     * @param npmrcContent .npmrc user config
     * @param registry2Credentials the credentials to be inserted into the user
     *        config (key: registry URL, value: Jenkins credentials)
     * @param npm9Format use npm version 9 format
     * @return the updated version of the {@code npmrcContent} with the registry
     *         credentials added
     */
    public String fillRegistry(String npmrcContent, Map<String, StandardCredentials> registry2Credentials, boolean npm9Format) {
        Npmrc npmrc = new Npmrc();
        npmrc.from(npmrcContent);

        for (NPMRegistry registry : registries) {
            StandardCredentials credentials = null;
            if (registry2Credentials.containsKey(registry.getUrl())) {
                credentials = registry2Credentials.get(registry.getUrl());
            }

            if (registry.isHasScopes()) {
                for (String scope : registry.getScopesAsList()) {
                    // remove protocol from the registry URL
                    String registryPrefix = calculatePrefix(registry.getUrl());
                    // ensure that URL ends with the / or will not match with scoped entries
                    String registryURL = fixURL(registry.getUrl());

                    // add scoped values to the user config file
                    npmrc.set(compose('@' + scope, NPM_SETTINGS_REGISTRY), registryURL);
                    if (credentials != null) { // NOSONAR
                        npmrc.set(compose(registryPrefix, NPM_SETTINGS_ALWAYS_AUTH), credentials != null);

                        // the _auth directive seems not be considered for scoped registry
                        // only authToken or username/password works
                        if (credentials instanceof UsernamePasswordCredentials) {
                            UsernamePasswordCredentials usernamePassowrd = (UsernamePasswordCredentials) credentials;
                            String passwordValue = Base64.encodeBase64String(Secret.toString(usernamePassowrd.getPassword()).getBytes(StandardCharsets.UTF_8));
                            npmrc.set(compose(registryPrefix, NPM_SETTINGS_USER), usernamePassowrd.getUsername());
                            npmrc.set(compose(registryPrefix, NPM_SETTINGS_PASSWORD), passwordValue);
                        } else if (credentials instanceof StringCredentials) {
                            StringCredentials stringCredentials = (StringCredentials) credentials;
                            String tokenValue = Secret.toString(stringCredentials.getSecret());
                            npmrc.set(compose(registryPrefix, NPM_SETTINGS_AUTHTOKEN), tokenValue);
                        }
                    }
                }
            } else {
                String registryPrefix = npm9Format ? calculatePrefix(registry.getUrl()) : null;

                // add values to the user config file
                npmrc.set(NPM_SETTINGS_REGISTRY, registry.getUrl());
                if (credentials != null) {
                    npmrc.set(compose(registryPrefix, NPM_SETTINGS_ALWAYS_AUTH), credentials != null);
                    if (credentials instanceof UsernamePasswordCredentials) {
                        UsernamePasswordCredentials usernamePassowrd = (UsernamePasswordCredentials) credentials;
                        String authValue = usernamePassowrd.getUsername() + ':' + Secret.toString(usernamePassowrd.getPassword());
                        authValue = Base64.encodeBase64String(authValue.getBytes(StandardCharsets.UTF_8));
                        npmrc.set(compose(registryPrefix, NPM_SETTINGS_AUTH), authValue);
                    } else if (credentials instanceof StringCredentials) {
                        StringCredentials stringCredentials = (StringCredentials) credentials;
                        String tokenValue = Secret.toString(stringCredentials.getSecret());
                        npmrc.set(compose(registryPrefix, NPM_SETTINGS_AUTHTOKEN), tokenValue);
                    }
                }
            }
        }

        return npmrc.toString();
    }

    @NonNull
    private String fixURL(@NonNull final String registryURL) {
        String url = registryURL;
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    @NonNull
    public String calculatePrefix(@NonNull final String registryURL) {
        String trimmedURL = trimSlash(registryURL);

        URL url = toURL(trimmedURL);
        if (url == null) {
            throw new IllegalArgumentException("Invalid url " + registryURL);
        }

        return "//" + trimmedURL.substring((url.getProtocol() + "://").length()) + '/';
    }

    @NonNull
    public String compose(@NonNull final String registryPrefix, @NonNull final String setting) {
        if (StringUtils.isBlank(registryPrefix)) {
            return setting;
        }
        return registryPrefix + ":" + setting;
    }

    @NonNull
    private String trimSlash(@NonNull final String url) {
        if (url != null && url.endsWith("/")) {
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

    public @NonNull List<String> secretsForMasking(Run<?, ?> build) {
        List<String> secretsForMasking = new ArrayList<>();
        Map<String, StandardCredentials> resolveCredentials = resolveCredentials(build);
        for (StandardCredentials credential : resolveCredentials.values()) {
            if (credential instanceof UsernamePasswordCredentials) {
                UsernamePasswordCredentials userPassCredential = (UsernamePasswordCredentials) credential;
                // we could be passed separately, or as a basic token.
                String username = userPassCredential.getUsername();
                if (userPassCredential.isUsernameSecret()) {
                    secretsForMasking.add(userPassCredential.getUsername());
                }
                String password = Secret.toString(userPassCredential.getPassword());
                secretsForMasking.add(password);
                // and base64 encoded in some npmrc files
                if (!password.isBlank()) {
                    secretsForMasking.add(Base64.encodeBase64String(password.getBytes(StandardCharsets.UTF_8)));
                }
                // and HTTP basic...
                secretsForMasking.add(Base64.encodeBase64String((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
            } else if (credential instanceof StringCredentials) {
                String tokenValue = Secret.toString(((StringCredentials) credential).getSecret());
                secretsForMasking.add(tokenValue);
            }
        }
        return secretsForMasking;
    }
}
