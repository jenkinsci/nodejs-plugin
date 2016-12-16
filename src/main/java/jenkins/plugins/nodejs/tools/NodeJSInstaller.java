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

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import jenkins.MasterToSlaveFileCallable;
import jenkins.plugins.tools.Installables;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * Install NodeJS from nodejs.org
 *
 * @author Frédéric Camblor
 * @since 0.2
 */
public class NodeJSInstaller extends DownloadFromUrlInstaller {

    public static final String NPM_PACKAGES_RECORD_FILENAME = ".npmPackages";
    private final String npmConfigs;
    private final String npmPackages;
    private final Long npmPackagesRefreshHours;
    private Platform platform;
    private CPU cpu;

    @DataBoundConstructor
    public NodeJSInstaller(String id, String npmConfigs, String npmPackages, long npmPackagesRefreshHours)    {
        super(id);
        this.npmConfigs = npmConfigs;
        this.npmPackages = npmPackages;
        this.npmPackagesRefreshHours = npmPackagesRefreshHours;
    }

    @Override
    public Installable getInstallable() throws IOException {
        Installable installable = super.getInstallable();
        if(installable==null) {
            return null;
        }

        // Cloning the installable since we're going to update its url (not cloning it wouldn't be threadsafe)
        installable = Installables.clone(installable);

        InstallerPathResolver installerPathResolver = InstallerPathResolver.Factory.findResolverFor(installable);
        String relativeDownloadPath = installerPathResolver.resolvePathFor(installable.id, platform, cpu);
        installable.url += relativeDownloadPath;
        return installable;
    }

    // Overriden performInstallation() in order to provide a custom
    // url (installable.url should be platform+cpu dependant)
    // + pullUp directory impl should differ from DownloadFromUrlInstaller
    // implementation
    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        this.platform = Platform.of(node);
        this.cpu = CPU.of(node);

        FilePath expected;
        Installable installable = getInstallable();
        if (installable == null || !installable.url.toLowerCase(Locale.ENGLISH).endsWith("msi")) {
            expected = super.performInstallation(tool, node, log);
        } else {
            expected = preferredLocation(tool, node);
            if (!isUpToDate(expected, installable)) {
                if (installIfNecessaryMSI(expected, new URL(installable.url), log, "Installing " + installable.url + " to " + expected + " on " + node.getDisplayName())) {
                    expected.child(".timestamp").delete(); // we don't use the timestamp
                    FilePath base = findPullUpDirectory(expected);
                    if (base != null && base != expected)
                        base.moveAllChildrenTo(expected);
                    // leave a record for the next up-to-date check
                    expected.child(".installedFrom").write(installable.url, "UTF-8");
                }
            }
        }
        
        //Setting global configs
       if(this.npmConfigs != null && !"".equals(this.npmConfigs)){
          
         for(String configKeyValue : this.npmConfigs.split("\\n")){
           String[] keyValue = configKeyValue.split("\\s");
           if(keyValue.length > 1 && !"".equals(keyValue[0])){
         		ArgumentListBuilder npmScriptArgs = new ArgumentListBuilder();
 	        	FilePath npmExe = expected.child("bin/npm");
 	            npmScriptArgs.add(npmExe);
 	            npmScriptArgs.add("config");
 	            npmScriptArgs.add("set");
 	            npmScriptArgs.add("-g");
                npmScriptArgs.add(keyValue[0]);
                npmScriptArgs.add(keyValue[1]);
                
                hudson.Launcher launcher = node.createLauncher(log);

                int returnCode = launcher.launch()
                        .envs("PATH+NODEJS="+expected.child("bin").getRemote())
                        .cmds(npmScriptArgs).stdout(log).join();
           }
         }
       }

        // Installing npm packages if needed
        if (this.npmPackages != null && !"".equals(this.npmPackages)) {
            boolean skipNpmPackageInstallation = areNpmPackagesUpToDate(expected, this.npmPackages, this.npmPackagesRefreshHours);
            if (!skipNpmPackageInstallation) {
                expected.child(NPM_PACKAGES_RECORD_FILENAME).delete();

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
                for (String packageName : this.npmPackages.split("\\s")) {
                    npmScriptArgs.add(packageName);
                }

                hudson.Launcher launcher = node.createLauncher(log);
				int returnCode = launcher.launch().envs("PATH+NODEJS=" + binFolder.getRemote()).cmds(npmScriptArgs).stdout(log).join();

                if (returnCode == 0) {
                    // leave a record for the next up-to-date check
                    expected.child(NPM_PACKAGES_RECORD_FILENAME).write(this.npmPackages, "UTF-8");
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
                ProcStarter starter = launch.launch().cmds(new File("cmd"), "/c", "for %A in (.) do msiexec TARGETDIR=%~sA /a "+ temp.getName() + "\\nodejs.msi /qn /L* " + temp.getName() + "\\log.txt");
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
                throw new IOException("Failed to install "+ archive, e);
            }
            timestamp.touch(sourceTimestamp);
            return true;
        } catch (IOException e) {
            throw new IOException("Failed to install " + archive + " to " + expected.getRemote(), e);
        }
    }

    // update code from ZipExtractionInstaller
    static class /*ZipExtractionInstaller*/ChmodRecAPlusX extends MasterToSlaveFileCallable<Void> {
        private static final long serialVersionUID = 1L;
        public Void invoke(File d, VirtualChannel channel) throws IOException {
            if(!Functions.isWindows())
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

    public String getNpmConfigs() {
        return npmConfigs;
    }
    
    public String getNpmPackages() {
        return npmPackages;
    }

    public Long getNpmPackagesRefreshHours() {
        return npmPackagesRefreshHours;
    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<NodeJSInstaller> {
        @Override
        public String getDisplayName() {
            return Messages.NodeJSInstaller_DescriptorImpl_displayName();
        }

        @Override
        public List<? extends Installable> getInstallables() throws IOException {
            // Filtering non blacklisted installables + sorting installables by version number
            Collection<? extends Installable> filteredInstallables = Collections2.filter(super.getInstallables(),
                    new Predicate<Installable>() {
                        @Override
                        public boolean apply(@Nullable Installable input) {
                            return !InstallerPathResolver.Factory.isVersionBlacklisted(input.id);
                        }
                    });
            TreeSet<Installable> sortedInstallables = new TreeSet<Installable>(new Comparator<Installable>(){
                @Override
                public int compare(Installable o1, Installable o2) {
                    return NodeJSVersion.parseVersion(o1.id).compareTo(NodeJSVersion.parseVersion(o2.id))*-1;
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

}