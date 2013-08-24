package jenkins.plugins.nodejs.tools;

import jenkins.plugins.nodejs.tools.pathresolvers.LatestInstallerPathResolver;
import hudson.tools.DownloadFromUrlInstaller;

/**
 * @author fcamblor
 */
public interface InstallerPathResolver {
    String resolvePathFor(String version, NodeJSInstaller.Platform platform, NodeJSInstaller.CPU cpu);
    String extractArchiveIntermediateDirectoryName(String relativeDownloadPath);

    public static class Factory {
        public static InstallerPathResolver findResolverFor(DownloadFromUrlInstaller.Installable installable){
            if(isVersionBlacklisted(installable.id)){
                throw new IllegalArgumentException("Provided version ("+installable.id+") installer structure not (yet) supported !");
            } else {
                return new LatestInstallerPathResolver();
            }
        }

        public static boolean isVersionBlacklisted(String version){
            NodeJSVersion nodeJSVersion = new NodeJSVersion(version);
            return nodeJSVersion.isLowerThan("0.8.6") || "0.9.0".equals(version);
        }
    }
}
