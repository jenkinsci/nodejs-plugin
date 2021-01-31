/*
 * The MIT License
 *
 * Copyright (c) 2021, Riain Condon
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

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import jenkins.plugins.nodejs.Messages;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Automatic NodeJS installer from a nodejs.org mirror
 *
 * @author Riain Condon
 * @author Nikolas Falco
 */

public class MirrorNodeJSInstaller extends NodeJSInstaller {
    private String mirrorURL;
    private boolean skipTLSVerify;
    private String credentialsId;

    private static final String PUBLIC_NODEJS_URL = "https://nodejs.org/dist";

    @DataBoundConstructor
    public MirrorNodeJSInstaller(String id, String npmPackages, long npmPackagesRefreshHours, @Nonnull String mirrorURL, boolean skipTLSVerify, String credentialsId) {
        super(id, npmPackages, npmPackagesRefreshHours);
        this.mirrorURL = mirrorURL;
        this.skipTLSVerify = skipTLSVerify;
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setMirrorURL(String mirrorURL) {
        this.mirrorURL = mirrorURL;
    }

    public String getMirrorURL() {
        return this.mirrorURL;
    }

    @DataBoundSetter
    public void setSkipTLSVerify(boolean skipTLSVerify) {
        this.skipTLSVerify = skipTLSVerify;
    }

    public boolean isSkipTLSVerify() {
        return this.skipTLSVerify;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
        return this.credentialsId;
    }

    TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
    };

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        if (this.isSkipTLSVerify()) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
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
            String baseURL = url.replace(PUBLIC_NODEJS_URL, mirrorURL);
            url = baseURL + relativeDownloadPath;
            return this;
        }

    }

    @Extension
    public static final class DescriptorImpl extends NodeJSInstaller.DescriptorImpl { // NOSONAR
        @Override
        public String getDisplayName() {
            return Messages.MirrorNodeJSInstaller_DescriptorImpl_displayName();
        }

        public FormValidation doCheckMirrorURL(@CheckForNull @QueryParameter final String mirrorURL) throws IOException {
            if (!mirrorURL.isEmpty()) {
                URL url = new URL(mirrorURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    return FormValidation.ok();
                } else return FormValidation.error(Messages.MirrorNodeJSInstaller_DescriptorImpl_unableToReachMirror());
            } else {
                return FormValidation.error(Messages.MirrorNodeJSInstaller_DescriptorImpl_emptyMirrorURL());
            }
        }
    }
}
