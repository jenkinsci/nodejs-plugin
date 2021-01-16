package jenkins.plugins.nodejs.tools;

import hudson.Extension;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import jenkins.plugins.nodejs.Messages;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class MirrorNodeJSInstaller extends NodeJSInstaller {
    private String mirrorUrl;
    private boolean ignoreTlsErrors;

    @DataBoundConstructor
    public MirrorNodeJSInstaller(String id, String npmPackages, long npmPackagesRefreshHours, String mirrorUrl) {
        super(id, npmPackages, npmPackagesRefreshHours);
        this.mirrorUrl = mirrorUrl;
    }

    public MirrorNodeJSInstaller(String id, String npmPackages, long npmPackagesRefreshHours, boolean force32Bit, String mirrorUrl) {
        super(id, npmPackages, npmPackagesRefreshHours, force32Bit);
        this.mirrorUrl = mirrorUrl;
    }

    public MirrorNodeJSInstaller(String id, String npmPackages, long npmPackagesRefreshHours, String mirrorUrl, boolean ignoreTlsErrors) {
        this(id, npmPackages, npmPackagesRefreshHours, mirrorUrl);
        this.ignoreTlsErrors = ignoreTlsErrors;
    }

    public MirrorNodeJSInstaller(String id, String npmPackages, long npmPackagesRefreshHours, boolean force32Bit, String mirrorUrl, boolean ignoreTlsErrors) {
        this(id, npmPackages, npmPackagesRefreshHours, force32Bit, mirrorUrl);
        this.ignoreTlsErrors = ignoreTlsErrors;
    }

    @DataBoundSetter
    public void setMirrorUrl(String mirrorUrl) { this.mirrorUrl = mirrorUrl; }

    public String getMirrorUrl() { return this.mirrorUrl; }

    @DataBoundSetter
    public void setIgnoreTlsErrors(boolean ignoreTlsErrors) { this.ignoreTlsErrors = ignoreTlsErrors; }

    public boolean isIgnoreTlsErrors() { return this.ignoreTlsErrors; }

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
            boolean useMirror = !mirrorUrl.isEmpty();
            String baseURL = useMirror ? url.replace("https://nodejs.org/dist", mirrorUrl) : url;
            url = baseURL + relativeDownloadPath;
            return this;
        }

    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<MirrorNodeJSInstaller> { // NOSONAR
        @Override
        public String getDisplayName() {
            return Messages.MirrorNodeJSInstaller_DescriptorImpl_displayName();
        }

        @Nonnull
        @Override
        public List<? extends Installable> getInstallables() throws IOException {
            // Filtering non blacklisted installables + sorting installables by
            // version number
            List<? extends Installable> filteredInstallables = super.getInstallables().stream() //
                    .filter(i -> !InstallerPathResolver.Factory.isVersionBlacklisted(i.id)) //
                    .collect(Collectors.toList());
            TreeSet<Installable> sortedInstallables = new TreeSet<>(new Comparator<Installable>() {
                @Override
                public int compare(Installable o1, Installable o2) {
                    return NodeJSVersion.parseVersion(o1.id).compareTo(NodeJSVersion.parseVersion(o2.id)) * -1;
                }
            });
            sortedInstallables.addAll(filteredInstallables);
            return new ArrayList<>(sortedInstallables);
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
