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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.Messages;

/**
 * Holder of all informations about a npm public/private registry.
 * <p>
 * This class keep all necessary information to access a npm registry that must
 * be stored in a user config file.
 * Typically information are:
 * <ul>
 * <li>the registry URL</li>
 * <li>list of scope for the registry, used typical in private registry</li>
 * <li>account credentials to access the registry</li>
 * </ul>
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public class NPMRegistry extends AbstractDescribableImpl<NPMRegistry> implements Serializable {
    private static final long serialVersionUID = -5199710867477461372L;

    private final String url;
    private final String scopes;
    private final String credentialsId;

    /**
     * Default constructor.
     *
     * @param url url of a npm registry
     * @param credentialsId credentials identifier
     * @param scopes url-safe characters, no leading dots or underscores
     */
    public NPMRegistry(@NonNull String url, String credentialsId, String scopes) {
        this.url = Util.fixEmpty(url);
        this.credentialsId = Util.fixEmpty(credentialsId);
        this.scopes = fixScope(Util.fixEmpty(scopes));
    }

    /**
     * Default constructor used by jelly page for the optional block.
     *
     * @param url url of a npm registry
     * @param credentialsId credentials identifier
     * @param hasScopes if this registry was designed for a specific scope
     * @param scopes url-safe characters, no leading dots or underscores
     */
    @DataBoundConstructor
    public NPMRegistry(@NonNull String url, String credentialsId, boolean hasScopes, String scopes) {
        this.url = Util.fixEmpty(url);
        this.credentialsId = Util.fixEmpty(credentialsId);
        this.scopes = hasScopes ? fixScope(Util.fixEmpty(scopes)) : null;
    }

    @Nullable
    private String fixScope(final @Nullable String scope) {
        if (scope != null && scope.startsWith("@")) {
            return scope.substring(1);
        }
        return scope;
    }

    /**
     * Get the registry URL
     *
     * @return the registry URL
     */
    @Nullable
    public String getUrl() {
        return url;
    }

    /**
     * Get list of scope for this registry.
     * <p>
     * The scope are not prefixed with {@literal @} character.
     *
     * @return a space separated list of scope.
     */
    public String getScopes() {
        return scopes;
    }

    public boolean isHasScopes() {
        return scopes != null;
    }

    /**
     * Provide a list of scope for this registry.
     * <p>
     * The scope are not prefixed with {@literal @} character.
     *
     * @return list of scope.
     */
    public List<String> getScopesAsList() {
        List<String> result = Collections.emptyList();
        if (isHasScopes()) {
            result = Arrays.asList(StringUtils.split(scopes));
        }
        return result;
    }

    /**
     * Get list of scope for this registry.
     * <p>
     * The scope are not prefixed with {@literal @} character.
     *
     * @return a space separated list of scope.
     */
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Perform the validation of current registry.
     * <p>
     * If validation pass then no {@link VerifyConfigProviderException} will be
     * raised.
     *
     * @throws VerifyConfigProviderException
     *             in case this configuration is not valid.
     */
    public void doVerify() throws VerifyConfigProviderException {
        // recycle validations from descriptor
        DescriptorImpl descriptor = new DescriptorImpl();

        throwException(descriptor.doCheckUrl(getUrl()));
        throwException(descriptor.doCheckScopes(isHasScopes(), getScopes()));
    }

    private void throwException(FormValidation form) throws VerifyConfigProviderException {
        if (form.kind == Kind.ERROR) {
            throw new VerifyConfigProviderException(form.getLocalizedMessage());
        }
    }

    @Override
    public String toString() {
        return "url: " + url + (scopes != null ? " scopes: [" + scopes + "]" : "") + (credentialsId != null ? " credentialId: " + credentialsId : "");
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<NPMRegistry> {

        private Pattern variableRegExp = Pattern.compile ( "\\$\\{.*\\}" );

        public FormValidation doCheckScopes(@CheckForNull @QueryParameter final boolean hasScopes,
                                            @CheckForNull @QueryParameter String scopes) {
            scopes = Util.fixEmptyAndTrim(scopes);
            if (hasScopes) {
                if (scopes == null) {
                    return FormValidation.error(Messages.NPMRegistry_DescriptorImpl_emptyScopes());
                }
                StringTokenizer st = new StringTokenizer(scopes);
                while (st.hasMoreTokens()) {
                    String aScope = st.nextToken();
                    if (aScope.startsWith("@")) {
                        if (aScope.length() == 1) {
                            return FormValidation.error(Messages.NPMRegistry_DescriptorImpl_invalidScopes());
                        }
                        return FormValidation.warning(Messages.NPMRegistry_DescriptorImpl_invalidCharInScopes());
                    }
                }
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckUrl(@CheckForNull @QueryParameter final String url) {
            if (StringUtils.isBlank(url)) {
                return FormValidation.error(Messages.NPMRegistry_DescriptorImpl_emptyRegistryURL());
            }

            // test malformed URL
            if (!variableRegExp.matcher(url).find() && toURL(url) == null) {
                return FormValidation.error(Messages.NPMRegistry_DescriptorImpl_invalidRegistryURL());
            }

            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckCredentialsId(@CheckForNull @AncestorInPath Item projectOrFolder,
                                                   @QueryParameter String credentialsId,
                                                   @QueryParameter String serverUrl) {
            if ((projectOrFolder == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)) ||
                    (projectOrFolder != null && !projectOrFolder.hasPermission(Item.EXTENDED_READ) && !projectOrFolder.hasPermission(CredentialsProvider.USE_ITEM))) {
                return FormValidation.ok();
            }
            if (StringUtils.isBlank(credentialsId)) {
                return FormValidation.warning(Messages.NPMRegistry_DescriptorImpl_emptyCredentialsId());
            }

            List<DomainRequirement> domainRequirement = URIRequirementBuilder.fromUri(serverUrl).build();
            Authentication authentication = getAuthentication(projectOrFolder);
            CredentialsMatcher matcher = CredentialsMatchers.withId(credentialsId);
            if (CredentialsProvider.listCredentials(StandardUsernameCredentials.class, projectOrFolder, authentication, domainRequirement, matcher).isEmpty()
                    && CredentialsProvider.listCredentials(StringCredentials.class, projectOrFolder, authentication, domainRequirement, matcher).isEmpty()) {
                return FormValidation.error(Messages.NPMRegistry_DescriptorImpl_invalidCredentialsId());
            }
            return FormValidation.ok();
        }

        @POST
        public ListBoxModel doFillCredentialsIdItems(final @CheckForNull @AncestorInPath ItemGroup<?> context,
                                                     final @CheckForNull @AncestorInPath Item projectOrFolder,
                                                     @QueryParameter String credentialsId,
                                                     final @QueryParameter String url) {
            Permission permToCheck = projectOrFolder == null ? Jenkins.ADMINISTER : Item.CONFIGURE;
            AccessControlled contextToCheck = projectOrFolder == null ? Jenkins.get() : projectOrFolder;
            credentialsId = StringUtils.trimToEmpty(credentialsId);

            // If we're on the global page and we don't have administer
            // permission or if we're in a project or folder
            // and we don't have configure permission there
            if (!contextToCheck.hasPermission(permToCheck)) {
                return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
            }

            Authentication authentication = getAuthentication(projectOrFolder);
            List<DomainRequirement> domainRequirements = URIRequirementBuilder.fromUri(url).build();
            CredentialsMatcher either = CredentialsMatchers.either( //
                    CredentialsMatchers.instanceOf(StandardUsernameCredentials.class), //
                    CredentialsMatchers.instanceOf(StringCredentials.class) //
            );
            Class<StandardCredentials> type = StandardCredentials.class;

            return new StandardListBoxModel() //
                    .includeMatchingAs(authentication, context, type, domainRequirements, either) //
                    .includeEmptyValue();
        }

        @NonNull
        protected Authentication getAuthentication(Item item) {
            return item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM;
        }

        @Override
        public String getDisplayName() {
            return "";
        }

        private static URL toURL(final String url) {
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

}
