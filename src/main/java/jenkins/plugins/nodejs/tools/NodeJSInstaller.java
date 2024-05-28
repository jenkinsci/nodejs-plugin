/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco, Frédéric Camblor
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import edu.umd.cs.findbugs.annotations.NonNull;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.FilePath.TarCompression;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.Messages;
import jenkins.plugins.nodejs.NodeJSConstants;

/**
 * Automatic NodeJS installer from nodejs.org
 *
 * @author Frédéric Camblor
 * @author Nikolas Falco
 *
 * @since 0.2
 */
public class NodeJSInstaller extends DownloadFromUrlInstaller {

    private static boolean DISABLE_CACHE = Boolean.getBoolean(NodeJSInstaller.class.getName() + ".cache.disable");
    public static final String NPM_PACKAGES_RECORD_FILENAME = ".npmPackages";

    /**
     * Define the elapse time before perform a new npm install for defined
     * global packages.
     */
    public static final int DEFAULT_NPM_PACKAGES_REFRESH_HOURS = 72;

    private final String npmPackages;
    private final Long npmPackagesRefreshHours;
    private boolean force32Bit;

    @DataBoundConstructor
    public NodeJSInstaller(String id, String npmPackages, long npmPackagesRefreshHours) {
        super(id);
        this.npmPackages = Util.fixEmptyAndTrim(npmPackages);
        this.npmPackagesRefreshHours = npmPackagesRefreshHours;
    }

    public NodeJSInstaller(String id, String npmPackages, long npmPackagesRefreshHours, boolean force32bit) {
        this(id, npmPackages, npmPackagesRefreshHours);
        this.force32Bit = force32bit;
    }

    @Override
    public Installable getInstallable() throws IOException {
        Installable installable = super.getInstallable();
        return installable != null ? new NodeJSInstallable(installable) : installable;
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath expected = preferredLocation(tool, node);

        Installable installable = getInstallable();
        if (installable == null) {
            log.getLogger().println("Invalid tool ID " + id);
            return expected;
        }

        if (installable instanceof NodeSpecific) {
            installable = (Installable) ((NodeSpecific<?>) installable).forNode(node, log);
        }

        if (!isUpToDate(expected, installable)) {
            File cache = getLocalCacheFile(installable, node);
            boolean skipInstall = false;
            if (!DISABLE_CACHE && cache.exists()) {
                log.getLogger().println(Messages.NodeJSInstaller_installFromCache(cache, expected, node.getDisplayName()));
                try {
                    restoreCache(expected, cache, log);
                    skipInstall = true;
                } catch (IOException e) {
                    log.error("Use of caches failed: " + e.getMessage());
                }
            }
            if (!skipInstall) {
                String message = installable.url + " to " + expected + " on " + node.getDisplayName();
                boolean isMSI = installable.url.toLowerCase(Locale.ENGLISH).endsWith("msi");
                URL installableURL = new URL(installable.url);
    
                if (isMSI && installIfNecessaryMSI(expected, installableURL, log, "Installing " + message)
                        || expected.installIfNecessaryFrom(installableURL, log, "Unpacking " + message)) {
    
                    expected.child(".timestamp").delete(); // we don't use the timestamp
                    FilePath base = findPullUpDirectory(expected);
                    if (base != null && base != expected) {
                        base.moveAllChildrenTo(expected);
                    }
                    // leave a record for the next up-to-date check
                    expected.child(".installedFrom").write(installable.url, "UTF-8");
    
                    if (!DISABLE_CACHE) {
                        buildCache(expected, cache);
                    }
                }
            }
        }

        refreshGlobalPackages(node, log, expected);

        return expected;
    }

    private void restoreCache(FilePath expected, File cache, TaskListener log) throws IOException, InterruptedException {
        try (InputStream in = cache.toURI().toURL().openStream()) {
            CountingInputStream cis = new CountingInputStream(in);
            try {
                Objects.requireNonNull(expected).untarFrom(cis, TarCompression.GZIP);
            } catch (IOException e) {
                throw new IOException(Messages.NodeJSInstaller_failedToUnpack(cache.toURI().toURL(), cis.getByteCount()), e);
            }
        }
    }

    private void buildCache(FilePath expected, File cache) throws IOException, InterruptedException {
        // update the local cache on master
        // download to a temporary file and rename it in to handle concurrency and failure correctly,
        Path tmp = new File(cache.getPath() + ".tmp").toPath();
        try {
            Path tmpParent = tmp.getParent();
            if (tmpParent != null) {
                Files.createDirectories(tmpParent);
            }
            try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(tmp))) {
                // workaround to not store current folder as root folder in the archive
                // this prevent issue when tool name is renamed 
                expected.tar(out, "**");
            }
            Files.move(tmp, cache.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /*
     * Installing npm packages if needed
     */
    protected void refreshGlobalPackages(Node node, TaskListener log, FilePath expected) throws IOException, InterruptedException {
        String globalPackages = getNpmPackages();

        if (StringUtils.isNotBlank(globalPackages)) { // JENKINS-41876
            boolean skipNpmPackageInstallation = areNpmPackagesUpToDate(expected, globalPackages, getNpmPackagesRefreshHours());
            if (!skipNpmPackageInstallation) {
                expected.child(NPM_PACKAGES_RECORD_FILENAME).delete();

                Platform platform = ToolsUtils.getPlatform(node);

                ArgumentListBuilder npmScriptArgs = new ArgumentListBuilder();
                if (platform == Platform.WINDOWS) {
                    npmScriptArgs.add("cmd");
                    npmScriptArgs.add("/c");
                }

                FilePath binFolder = expected.child(platform.binFolder);
                FilePath npmExe = binFolder.child(platform.npmFileName);
                npmScriptArgs.add(npmExe);

                npmScriptArgs.add("install");
                npmScriptArgs.add("-g");
                for (String packageName : globalPackages.split("\\s")) {
                    npmScriptArgs.add(packageName);
                }

                EnvVars env = new EnvVars();
                env.put(NodeJSConstants.ENVVAR_NODEJS_PATH, binFolder.getRemote());
                try {
                    buildProxyEnvVars(env, log);
                } catch (URISyntaxException e) {
                    log.error("Wrong proxy URL: " + e.getMessage());
                }

                hudson.Launcher launcher = node.createLauncher(log);
                int returnCode = launcher.launch().envs(env).cmds(npmScriptArgs).stdout(log).join();

                if (returnCode == 0) {
                    // leave a record for the next up-to-date check
                    expected.child(NPM_PACKAGES_RECORD_FILENAME).write(globalPackages, "UTF-8");
                    expected.child(NPM_PACKAGES_RECORD_FILENAME).act(new ChmodRecAPlusX());
                }
            }
        }
    }

    private void buildProxyEnvVars(EnvVars env, TaskListener log) throws IOException, URISyntaxException {
        ProxyConfiguration proxycfg = Jenkins.get().getProxy();
        if (proxycfg == null) {
            // no proxy configured
            return;
        }

        String userInfo = proxycfg.getUserName();
        // append password only if userName if is defined
        if (userInfo != null && proxycfg.getSecretPassword() != null) {
            userInfo += ":" + Secret.toString(proxycfg.getSecretPassword());
        }

        String proxyURL = new URI("http", userInfo, proxycfg.name, proxycfg.port, null, null, null).toString();

        // refer to https://docs.npmjs.com/misc/config#https-proxy
        env.put("HTTP_PROXY", proxyURL);
        env.put("HTTPS_PROXY", proxyURL);
        String noProxyHosts = proxycfg.getNoProxyHost();
        if (noProxyHosts != null) {
            if (noProxyHosts.contains("*")) {
                log.getLogger().println("INFO: npm doesn't support wild card in no_proxy configuration");
            }
            // refer to https://github.com/npm/npm/issues/7168
            env.put("NO_PROXY", noProxyHosts.replaceAll("(\r?\n)+", ","));
        }
    }

    public static boolean areNpmPackagesUpToDate(FilePath expected, String npmPackages, long npmPackagesRefreshHours) throws IOException, InterruptedException {
        FilePath marker = expected.child(NPM_PACKAGES_RECORD_FILENAME);
        return marker.exists() && marker.readToString().equals(npmPackages) && System.currentTimeMillis() < marker.lastModified()+ TimeUnit.HOURS.toMillis(npmPackagesRefreshHours);
    }

    private boolean installIfNecessaryMSI(FilePath expected, URL archive, TaskListener listener, String message) throws IOException, InterruptedException {
        try {
            URLConnection con;
            try {
                con = ProxyConfiguration.open(archive);
                con.connect();
            } catch (IOException x) {
                if (expected.exists()) {
                    // Cannot connect now, so assume whatever was last unpacked is still OK.
                    if (listener != null) {
                        listener.getLogger().println("Skipping installation of " + archive + " to " + expected.getRemote() + ": " + x);
                    }
                    return false;
                } else {
                    throw x;
                }
            }
            long sourceTimestamp = con.getLastModified();
            FilePath timestamp = expected.child(".timestamp");

            if (expected.exists()) {
                if (timestamp.exists() && sourceTimestamp == timestamp.lastModified()) {
                    return false; // already up to date
                }
                expected.deleteContents();
            } else {
                expected.mkdirs();
            }

            if (listener != null) {
                listener.getLogger().println(message);
            }

            FilePath temp = expected.createTempDir("_temp", "");
            FilePath msi = temp.child("nodejs.msi");

            msi.copyFrom(archive);
            try {
                Launcher launch = temp.createLauncher(listener);
                ProcStarter starter = launch.launch().cmds(new File("cmd"), "/c", "for %A in (.) do msiexec TARGETDIR=\"%~sA\" /a "+ temp.getName() + "\\nodejs.msi /qn /L* " + temp.getName() + "\\log.txt");
                starter=starter.pwd(expected);

                int exitCode=starter.join();
                if (exitCode != 0) {
                    throw new IOException("msiexec failed. exit code: " + exitCode + " Please see the log file " + temp.child("log.txt").getRemote() + " for more informations.", null);
                }

                if (listener != null) {
                    listener.getLogger().println("msi install complete");
                }

                // remove temporary folder
                temp.deleteRecursive();

                // remove the double msi file in expected folder
                FilePath duplicatedMSI = expected.child("nodejs.msi");
                if (duplicatedMSI.exists()) {
                    duplicatedMSI.delete();
                }
            } catch (IOException e) {
                throw new IOException("Failed to install " + archive, e);
            }
            timestamp.touch(sourceTimestamp);
            return true;
        } catch (IOException e) {
            throw new IOException("Failed to install " + archive + " to " + expected.getRemote(), e);
        }
    }

    // update code from ZipExtractionInstaller
    static class /* ZipExtractionInstaller */ ChmodRecAPlusX extends MasterToSlaveFileCallable<Void> {
        private static final long serialVersionUID = 1L;

        @Override
        public Void invoke(File d, VirtualChannel channel) throws IOException {
            if (!Functions.isWindows())
                process(d);
            return null;
        }

        private void process(File f) {
            if (f.isFile()) {
                f.setExecutable(true, false);
            } else {
                File[] kids = f.listFiles();
                if (kids != null) {
                    for (File kid : kids) {
                        process(kid);
                    }
                }
            }
        }
    }

    public String getNpmPackages() {
        return npmPackages;
    }

    public Long getNpmPackagesRefreshHours() {
        return npmPackagesRefreshHours;
    }

    public boolean isForce32Bit() {
        return force32Bit;
    }

    @DataBoundSetter
    public void setForce32Bit(boolean force32Bit) {
        this.force32Bit = force32Bit;
    }

    protected File getLocalCacheFile(Installable installable, Node node) throws DetectionFailedException {
        Platform platform = ToolsUtils.getPlatform(node);
        CPU cpu = ToolsUtils.getCPU(node);
        // we store cache as tar.gz to preserve symlink
        return new File(Jenkins.get().getRootDir(), "caches/nodejs/" + platform + "/" + cpu + "/" + id + ".tar.gz");
    }

    protected final class NodeJSInstallable extends NodeSpecificInstallable {

        public NodeJSInstallable(Installable inst) {
            super(inst);
        }

        @Override
        public NodeSpecificInstallable forNode(Node node, TaskListener log) throws IOException, InterruptedException {
            InstallerPathResolver installerPathResolver = InstallerPathResolver.Factory.findResolverFor(id);
            String relativeDownloadPath = installerPathResolver.resolvePathFor(id, ToolsUtils.getPlatform(node), ToolsUtils.getCPU(node));
            url += relativeDownloadPath;
            return this;
        }

    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<NodeJSInstaller> { // NOSONAR
        @Override
        public String getDisplayName() {
            return Messages.NodeJSInstaller_DescriptorImpl_displayName();
        }

        @NonNull
        @Override
        public List<? extends Installable> getInstallables() throws IOException {
            return ToolsUtils.getInstallable();
        }

        @Override
        public String getId() {
            // For backward compatibility
            return "hudson.plugins.nodejs.tools.NodeJSInstaller";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == NodeJSInstallation.class;
        }
    }

}