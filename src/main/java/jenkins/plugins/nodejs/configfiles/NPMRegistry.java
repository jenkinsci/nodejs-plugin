package jenkins.plugins.nodejs.configfiles;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.ItemGroup;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;

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
    public NPMRegistry(@Nonnull String url, String credentialsId, String scopes) {
        this.url = Util.fixEmpty(url);
        this.credentialsId = Util.fixEmpty(credentialsId);
        this.scopes = fixScope(Util.fixEmpty(scopes));
    }

    /**
     * Default constructor used by jelly page for the optional block.
     *
     * @param url url of a npm registry
     * @param credentialsId credentials identifier
     * @param scoped if this registry was designed for a specific scope
     * @param scopes url-safe characters, no leading dots or underscores
     */
    @DataBoundConstructor
    public NPMRegistry(@Nonnull String url, String credentialsId, boolean hasScopes, String scopes) {
        this.url = Util.fixEmpty(url);
        this.credentialsId = Util.fixEmpty(credentialsId);
        this.scopes = hasScopes ? fixScope(Util.fixEmpty(scopes)) : null;
    }

    private String fixScope(String scope) {
        if (scope != null && scope.startsWith("@")) {
            return scope.substring(1);
        }
        return scope;
    }

    public String getUrl() {
        return url;
    }

    public String getScopes() {
        return scopes;
    }

    public boolean isHasScopes() {
        return scopes != null;
    }

    public List<String> getScopesAsList() {
        List<String> result = Collections.emptyList();
        if (isHasScopes()) {
            result = Arrays.asList(StringUtils.split(scopes));
        }
        return result;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<NPMRegistry> {

        public FormValidation doCheckScopes(@QueryParameter boolean hasScopes, @QueryParameter String scopes) {
            scopes = Util.fixEmptyAndTrim(scopes);
            if (hasScopes) {
                if (scopes == null) {
                    return FormValidation.error("Scopes is empty");
                }
                StringTokenizer st = new StringTokenizer(scopes);
                while (st.hasMoreTokens()) {
                    String aScope = st.nextToken();
                    if (aScope.startsWith("@")) {
                        if (aScope.length() == 1) {
                            return FormValidation.error("Invalid scope");
                        }
                        return FormValidation.warning("Remove the '@' character from scope");
                    }
                }
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckUrl(@QueryParameter String url) {
            if (StringUtils.isBlank(url)) {
                return FormValidation.error("Empty URL");
            }

            // test malformed URL
            if (toURL(url) == null) {
                return FormValidation.error("Invalid URL, should start with https://");
            }

            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup<?> context,
                                                     @QueryParameter String credentialsId,
                                                     @QueryParameter String url) {
            if (!hasPermission(context)) {
                return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
            }

            List<DomainRequirement> domainRequirements;
            URL registryURL = toURL(url);
            if (registryURL != null) {
                domainRequirements = Collections.<DomainRequirement> singletonList(new HostnameRequirement(registryURL.getHost()));
            } else {
                domainRequirements = Collections.emptyList();
            }

            return new StandardUsernameListBoxModel()
                    .includeMatchingAs(ACL.SYSTEM, context, StandardUsernameCredentials.class, domainRequirements, CredentialsMatchers.always())
                    .includeCurrentValue(credentialsId);
        }

        private boolean hasPermission(ItemGroup<?> context) {
            AccessControlled controller = context instanceof AccessControlled ? (AccessControlled) context : Jenkins.getInstance();
            return controller != null && controller.hasPermission(Computer.CONFIGURE);
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