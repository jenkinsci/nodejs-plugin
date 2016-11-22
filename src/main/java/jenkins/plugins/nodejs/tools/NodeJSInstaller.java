/*
 * The MIT License
 *
 * Copyright (c) 2009-2010, Sun Microsystems, Inc., CloudBees, Inc.
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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.os.PosixAPI;
import jenkins.MasterToSlaveFileCallable;
import jenkins.plugins.tools.Installables;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.jna.GNUCLibrary;
import hudson.util.IOException2;

import jenkins.security.MasterToSlaveCallable;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static jenkins.plugins.nodejs.tools.NodeJSInstaller.Preference.*;

/**
 * Install NodeJS from nodejs.org
 *
 * @author Frédéric Camblor
 * @since 0.2
 */
public class NodeJSInstaller extends DownloadFromUrlInstaller {

    public static final String NPM_PACKAGES_RECORD_FILENAME = ".npmPackages";
    private final String npmPackages;
    private final Long npmPackagesRefreshHours;

    @DataBoundConstructor
    public NodeJSInstaller(String id, String npmPackages, long npmPackagesRefreshHours)    {
        super(id);
        this.npmPackages = npmPackages;
        this.npmPackagesRefreshHours = npmPackagesRefreshHours;
    }

    /**
     * COPY PASTER ToolInstaller.preferredLocation() in order to make it static...
     * Weird
     *
     * Convenience method to find a location to install a tool.
     * @param tool the tool being installed
     * @param node the computer on which to install the tool
     * @return {@link ToolInstallation#getHome} if specified, else a path within the local
     *         Jenkins work area named according to {@link ToolInstallation#getName}
     * @since 1.310
     */
    protected static FilePath _preferredLocation(ToolInstallation tool, Node node) {
        if (node == null) {
            throw new IllegalArgumentException("must pass non-null node");
        }
        String home = Util.fixEmptyAndTrim(tool.getHome());
        if (home == null) {
            home = sanitize(tool.getDescriptor().getId()) + File.separatorChar + sanitize(tool.getName());
        }
        FilePath root = node.getRootPath();
        if (root == null) {
            throw new IllegalArgumentException("Node " + node.getDisplayName() + " seems to be offline");
        }
        return root.child("tools").child(home);
    }

    private static String sanitize(String s) {
        return s != null ? s.replaceAll("[^A-Za-z0-9_.-]+", "_") : null;
    }

    // Overriden performInstallation() in order to provide a custom
    // url (installable.url should be platform+cpu dependant)
    // + pullUp directory impl should differ from DownloadFromUrlInstaller
    // implementation
    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath expected = preferredLocation(tool, node);

        Installable inst = getInstallable();
        if(inst==null) {
            log.getLogger().println("Invalid tool ID "+id);
            return expected;
        }

        // Cloning the installable since we're going to update its url (not cloning it wouldn't be threadsafe)
        inst = Installables.clone(inst);

        InstallerPathResolver installerPathResolver = InstallerPathResolver.Factory.findResolverFor(inst);
        String relativeDownloadPath = createDownloadUrl(installerPathResolver, inst, node, log);
        inst.url += relativeDownloadPath;

        Platform platform = null;
        try {
            platform = Platform.of(node);
        } catch (DetectionFailedException e) {
            throw new IOException(e);
        }

        boolean skipNodeJSInstallation = isUpToDate(expected, inst);
        if(!skipNodeJSInstallation) {
            boolean result = false;
            
            if (platform == NodeJSInstaller.Platform.WINDOWS)
            {
                result = installIfNecessaryMSI(expected, new URL(inst.url), log, "Installing " + inst.url + " to " + expected + " on " + node.getDisplayName());
            } else {
                result = expected.installIfNecessaryFrom(new URL(inst.url), log, "Unpacking " + inst.url + " to " + expected + " on " + node.getDisplayName());
            }
            if(result) {
                expected.child(".timestamp").delete(); // we don't use the timestamp
                
                String archiveIntermediateDirectoryName = (platform == NodeJSInstaller.Platform.WINDOWS) ? 
                            "nodejs" : installerPathResolver.extractArchiveIntermediateDirectoryName(relativeDownloadPath);
                this.pullUpDirectory(expected, archiveIntermediateDirectoryName);
                // leave a record for the next up-to-date check
                expected.child(".installedFrom").write(inst.url,"UTF-8");
                expected.act(new ChmodRecAPlusX());
            }
        }
        
        // Installing npm packages if needed
        if(this.npmPackages != null && !"".equals(this.npmPackages)){
            boolean skipNpmPackageInstallation = areNpmPackagesUpToDate(expected, this.npmPackages, this.npmPackagesRefreshHours);
            if(!skipNpmPackageInstallation){
                expected.child(NPM_PACKAGES_RECORD_FILENAME).delete();
                ArgumentListBuilder npmScriptArgs = new ArgumentListBuilder();

                FilePath npmExe = expected.child(platform == Platform.WINDOWS ? "npm.cmd" : "bin/npm");
                if (platform ==  Platform.WINDOWS)
                {
                    npmScriptArgs.add("cmd");
                    npmScriptArgs.add("/c");
                }
                npmScriptArgs.add(npmExe);
                npmScriptArgs.add("install");
                npmScriptArgs.add("-g");
                for(String packageName : this.npmPackages.split("\\s")){
                    npmScriptArgs.add(packageName);
                }

                hudson.Launcher launcher = node.createLauncher(log);

                int returnCode = launcher.launch()
                        .envs("PATH+NODEJS="+expected.child("bin").getRemote())
                        .cmds(npmScriptArgs).stdout(log).join();

                if(returnCode == 0){
                    // leave a record for the next up-to-date check
                    expected.child(NPM_PACKAGES_RECORD_FILENAME).write(this.npmPackages,"UTF-8");
                    expected.child(NPM_PACKAGES_RECORD_FILENAME).act(new ChmodRecAPlusX());
                }
            }
        }

        return expected;
    }

    private static boolean areNpmPackagesUpToDate(FilePath expected, String npmPackages, long npmPackagesRefreshHours) throws IOException, InterruptedException {
        FilePath marker = expected.child(NPM_PACKAGES_RECORD_FILENAME);
        return marker.exists() && marker.readToString().equals(npmPackages) && System.currentTimeMillis() < marker.lastModified()+ TimeUnit.HOURS.toMillis(npmPackagesRefreshHours);
    }

    private void pullUpDirectory(final FilePath rootNodeHome, final String archiveIntermediateDirectoryName) throws IOException, InterruptedException {
        // Deleting every sub files/directory other than archiveIntermediateDirectoryName
        List<FilePath> subfiles = rootNodeHome.list();
        for(FilePath subfile : subfiles){
            if(!archiveIntermediateDirectoryName.equals(subfile.getName())){
                subfile.deleteRecursive();
            }
        }

        // Moving up files in archiveIntermediateDirectoryName
        FilePath archiveIntermediateDirectoryNameFP = rootNodeHome.child(archiveIntermediateDirectoryName);
        archiveIntermediateDirectoryNameFP.moveAllChildrenTo(rootNodeHome);
    }

    private String createDownloadUrl(InstallerPathResolver installerPathResolver, Installable installable, Node node, TaskListener log) throws InterruptedException, IOException {
        try {
            Platform platform = Platform.of(node);
            CPU cpu = CPU.of(node);
            return installerPathResolver.resolvePathFor(installable.id, platform, cpu);
        } catch (DetectionFailedException e) {
            throw new IOException(e);
        }
    }
    
    private boolean installIfNecessaryMSI(FilePath expected, URL archive, TaskListener listener, String message) throws IOException, InterruptedException {
    
         listener.getLogger().println("expected: "+expected.getRemote());
        
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
                    return false;   // already up to date
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
                ProcStarter starter = launch.launch().cmds(new File("cmd"), "/c", "for %A in (.) do msiexec TARGETDIR=%~sA /a "+temp.getName()+"\\nodejs.msi /qn /l* log.txt");
                starter=starter.pwd(expected);
                
                int exitCode=starter.join();
                if (exitCode != 0) {
                    throw new IOException2("msiexec failed. exit code: "+exitCode, null);
                }
                
                if (listener != null) {
                    listener.getLogger().println("msi install complete");
                }
                
                FilePath installed = temp.child("nodejs");
                installed.copyRecursiveTo(expected);
                temp.deleteRecursive();
            } catch (IOException e) {
                throw new IOException2("Failed to install "+ archive, e);
            }
            timestamp.touch(sourceTimestamp);
            return true;
        } catch (IOException e) {
            throw new IOException2("Failed to install " + archive + " to " + expected.getRemote(), e);
        }
    }

    /**
     * Weird : copy/pasted from ZipExtractionInstaller since this is a package-protected class
     *
     * Sets execute permission on all files, since unzip etc. might not do this.
     * Hackish, is there a better way?
     */
    static class ChmodRecAPlusX extends MasterToSlaveFileCallable<Void> {
        private static final long serialVersionUID = 1L;
        public Void invoke(File d, VirtualChannel channel) throws IOException {
            if(!Functions.isWindows())
                process(d);
            return null;
        }
        @IgnoreJRERequirement
        private void process(File f) {
            if (f.isFile()) {
                if(Functions.isMustangOrAbove())
                    f.setExecutable(true, false);
                else {
                    try {
                        GNUCLibrary.LIBC.chmod(f.getAbsolutePath(),0755);
                    } catch (LinkageError e) {
                        // if JNA is unavailable, fall back.
                        // we still prefer to try JNA first as PosixAPI supports even smaller platforms.
                        PosixAPI.get().chmod(f.getAbsolutePath(),0755);
                    }
                }
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

    public enum Preference {
        PRIMARY, SECONDARY, UNACCEPTABLE
    }

    /**
     * Supported platform.
     */
    public enum Platform {
        LINUX("node"), WINDOWS("node.exe"), MAC("node");

        /**
         * Choose the file name suitable for the downloaded Node bundle.
         */
        public final String bundleFileName;

        Platform(String bundleFileName) {
            this.bundleFileName = bundleFileName;
        }

        public boolean is(String line) {
            return line.contains(name());
        }

        /**
         * Determines the platform of the given node.
         */
        public static Platform of(Node n) throws IOException,InterruptedException,DetectionFailedException {
            return n.getChannel().call(new GetCurrentPlatform());
        }

        public static Platform current() throws DetectionFailedException {
            String arch = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
            if(arch.contains("linux"))  return LINUX;
            if(arch.contains("windows"))   return WINDOWS;
            if(arch.contains("mac"))   return MAC;
            throw new DetectionFailedException("Unknown CPU name: "+arch);
        }

        static class GetCurrentPlatform extends MasterToSlaveCallable<Platform,DetectionFailedException> {
            private static final long serialVersionUID = 1L;
            public Platform call() throws DetectionFailedException {
                return current();
            }
        }

    }

    /**
     * CPU type.
     */
    public enum CPU {
        i386, amd64;

        /**
         * In JDK5u3, I see platform like "Linux AMD64", while JDK6u3 refers to "Linux x64", so
         * just use "64" for locating bits.
         */
        public Preference accept(String line) {
            switch (this) {
            // 64bit Solaris, Linux, and Windows can all run 32bit executable, so fall back to 32bit if 64bit bundle is not found
            case amd64:
                if(line.contains("SPARC") || line.contains("IA64"))  return UNACCEPTABLE;
                if(line.contains("64"))     return PRIMARY;
                return SECONDARY;
            case i386:
                if(line.contains("64") || line.contains("SPARC") || line.contains("IA64"))     return UNACCEPTABLE;
                return PRIMARY;
            }
            return UNACCEPTABLE;
        }

        /**
         * Determines the CPU of the given node.
         */
        public static CPU of(Node n) throws IOException,InterruptedException, DetectionFailedException {
            return n.getChannel().call(new GetCurrentCPU());
        }

        /**
         * Determines the CPU of the current JVM.
         *
         * http://lopica.sourceforge.net/os.html was useful in writing this code.
         */
        public static CPU current() throws DetectionFailedException {
            String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
            if(arch.contains("amd64") || arch.contains("86_64"))    return amd64;
            if(arch.contains("86"))    return i386;
            throw new DetectionFailedException("Unknown CPU architecture: "+arch);
        }

        static class GetCurrentCPU extends MasterToSlaveCallable<CPU,DetectionFailedException> {
            private static final long serialVersionUID = 1L;
            public CPU call() throws DetectionFailedException {
                return current();
            }
        }

    }

    /**
     * Indicates the failure to detect the OS or CPU.
     */
    private static final class DetectionFailedException extends Exception {
        private DetectionFailedException(String message) {
            super(message);
        }
    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<NodeJSInstaller> {
        public String getDisplayName() {
            return Messages.NodeJSInstaller_DescriptorImpl_displayName();
        }

        @Override
        public List<? extends Installable> getInstallables() throws IOException {
            // Filtering non blacklisted installables + sorting installables by version number
            Collection<? extends Installable> filteredInstallables = Collections2.filter(super.getInstallables(),
                    new Predicate<Installable>() {
                        public boolean apply(@Nullable Installable input) {
                            return !InstallerPathResolver.Factory.isVersionBlacklisted(input.id);
                        }
                    });
            TreeSet<Installable> sortedInstallables = new TreeSet<Installable>(new Comparator<Installable>(){
                public int compare(Installable o1, Installable o2) {
                    return NodeJSVersion.compare(o1.id, o2.id)*-1;
                }
            });
            sortedInstallables.addAll(filteredInstallables);
            return new ArrayList<Installable>(sortedInstallables);
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

    private static final Logger LOGGER = Logger.getLogger(NodeJSInstaller.class.getName());
}
