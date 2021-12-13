/*
 * The MIT License
 *
 * Copyright (c) 2021, Riain Condon, Falco Nikolas
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
package jenkins.plugins.nodejs.tools;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.Messages;

/**
 * Automatic NodeJS installer from a nodejs.org mirror
 *
 * @author Riain Condon
 * @author Nikolas Falco
 * @since 1.4.0
 */
public class MirrorNodeJSInstaller extends NodeJSInstaller {
    private final static String PUBLIC_NODEJS_URL = "https://nodejs.org/dist";

    private final String mirrorURL;
    private String credentialsId;

    @DataBoundConstructor
    public MirrorNodeJSInstaller(@Nonnull String id, @Nonnull String mirrorURL, String npmPackages, long npmPackagesRefreshHours) {
        super(id, npmPackages, npmPackagesRefreshHours);
        this.mirrorURL = Util.fixEmptyAndTrim(mirrorURL);
    }

    public String getMirrorURL() {
        return this.mirrorURL;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    }

    public String getCredentialsId() {
        return this.credentialsId;
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        if (mirrorURL == null) {
            throw new NullPointerException("mirroURL is null");
        }
        return super.performInstallation(tool, node, log);
    }

    @Override
    public Installable getInstallable() throws IOException {
        Installable installable = super.getInstallable();
        return installable != null ? new MirrorNodeJSInstallable(installable) : installable;
    }

    protected final class MirrorNodeJSInstallable extends NodeSpecificInstallable {

        public MirrorNodeJSInstallable(Installable inst) {
            super(inst);
        }

        @Override
        public NodeSpecificInstallable forNode(Node node, TaskListener log) throws IOException {
            InstallerPathResolver installerPathResolver = InstallerPathResolver.Factory.findResolverFor(id);
            String relativeDownloadPath = installerPathResolver.resolvePathFor(id, ToolsUtils.getPlatform(node), ToolsUtils.getCPU(node));
            String baseURL;
            if (mirrorURL.endsWith("/")) {
                baseURL = url.replace(PUBLIC_NODEJS_URL, mirrorURL.substring(0, mirrorURL.length() - 1));
            } else {
                baseURL = url.replace(PUBLIC_NODEJS_URL, mirrorURL);
            }

            if (credentialsId != null) {
                // create a domain filter based on registry URL
                final URL mirror = toURL(mirrorURL);
                List<DomainRequirement> domainRequirements = URIRequirementBuilder.fromUri(mirrorURL).build();

                StandardUsernamePasswordCredentials credential = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, //
                        (ItemGroup<Item>) null, null, domainRequirements) //
                        .stream().filter(c -> credentialsId.equals(c.getId())).findFirst().orElse(null);
                if (credential != null) {
                    try {
                        baseURL = new URI(mirror.getProtocol(), //
                            bindCredentials(credential), //
                            mirror.getHost(), //
                            mirror.getPort(), //
                            mirror.getPath(), //
                            mirror.getQuery(), //
                            mirror.getRef()).toURL().toExternalForm();
                    } catch (URISyntaxException e) {
                        throw new IOException("Error composing URL with credentials", e);
                    }
                } else {
                    throw new IOException(Messages.MirrorNodeJSInstaller_invalidCredentialsId(credentialsId));
                }
            }

            url = baseURL + relativeDownloadPath;
            return this;
        }

        private String bindCredentials(StandardUsernamePasswordCredentials credential) {
            String password = Secret.toString(credential.getPassword());
            String username = credential.getUsername();
            return StringUtils.isNotBlank(password) ? username + ":" + password : username;
        }
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

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<MirrorNodeJSInstaller> { // NOSONAR
        private Pattern variableRegExp = Pattern.compile("\\$\\{.*\\}");

        @Override
        public String getDisplayName() {
            return Messages.MirrorNodeJSInstaller_DescriptorImpl_displayName();
        }

        public FormValidation doCheckCredentialsId(@CheckForNull @AncestorInPath Item item,
                                                   @QueryParameter String credentialsId,
                                                   @QueryParameter String mirrorURL) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
            if (StringUtils.isBlank(credentialsId)) {
                return FormValidation.ok();
            }

            List<DomainRequirement> domainRequirement = URIRequirementBuilder.fromUri(mirrorURL).build();
            if (CredentialsProvider.listCredentials(StandardUsernameCredentials.class, item, getAuthentication(item), domainRequirement, CredentialsMatchers.withId(credentialsId)).isEmpty()
                    && CredentialsProvider.listCredentials(StringCredentials.class, item, getAuthentication(item), domainRequirement, CredentialsMatchers.withId(credentialsId)).isEmpty()) {
                return FormValidation.error(Messages.NPMRegistry_DescriptorImpl_invalidCredentialsId());
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath Item item,
                                                     @QueryParameter String credentialsId,
                                                     final @QueryParameter String mirrorURL) {
            StandardListBoxModel result = new StandardListBoxModel();

            credentialsId = StringUtils.trimToEmpty(credentialsId);
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            Authentication authentication = getAuthentication(item);
            List<DomainRequirement> build = URIRequirementBuilder.fromUri(mirrorURL).build();
            CredentialsMatcher either = CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class);
            Class<StandardUsernamePasswordCredentials> type = StandardUsernamePasswordCredentials.class;

            result.includeEmptyValue();
            if (item != null) {
                result.includeMatchingAs(authentication, item, type, build, either);
            } else {
                result.includeMatchingAs(authentication, Jenkins.get(), type, build, either);
            }
            return result;
        }

        @Nonnull
        protected Authentication getAuthentication(Item item) {
            return item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM;
        }

        public FormValidation doCheckMirrorURL(@CheckForNull @QueryParameter final String mirrorURL) throws IOException {
            if (StringUtils.isBlank(mirrorURL)) {
                return FormValidation.error(Messages.MirrorNodeJSInstaller_DescriptorImpl_emptyMirrorURL());
            }

            if (!variableRegExp.matcher(mirrorURL).find() && toURL(mirrorURL) == null) {
                return FormValidation.error(Messages.MirrorNodeJSInstaller_DescriptorImpl_invalidURL());
            }

            return FormValidation.ok();
        }

        @Nonnull
        @Override
        public List<? extends Installable> getInstallables() throws IOException {
            return ToolsUtils.getInstallable();
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == NodeJSInstallation.class;
        }
    }

}