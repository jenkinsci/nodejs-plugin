package jenkins.plugins.nodejs.tools.pathresolvers;

import java.text.MessageFormat;

import jenkins.plugins.nodejs.tools.CPU;
import jenkins.plugins.nodejs.tools.InstallerPathResolver;
import jenkins.plugins.nodejs.tools.NodeJSVersion;
import jenkins.plugins.nodejs.tools.NodeJSVersionRange;
import jenkins.plugins.nodejs.tools.Platform;

/**
 * Calculate the name of the installer for the specified version according the
 * architecture and CPU of the destination node.
 *
 * @author fcamblor
 * @author Nikolas Falco
 */
public class LatestInstallerPathResolver implements InstallerPathResolver {
    private static final String EXTENSION = "tar.gz";
    private static final String EXTENSION_ZIP = "zip";
    private static final String EXTENSION_MSI = "msi";

    private static final NodeJSVersionRange[] MSI_RANGES = new NodeJSVersionRange[] { new NodeJSVersionRange("[0, 4.5)"),
                                                                                     new NodeJSVersionRange("[5, 6.2]") };

    /*
     * (non-Javadoc)
     * @see jenkins.plugins.nodejs.tools.InstallerPathResolver#resolvePathFor(java.lang.String, jenkins.plugins.nodejs.tools.Platform, jenkins.plugins.nodejs.tools.CPU)
     */
    @Override
    public String resolvePathFor(String version, Platform platform, CPU cpu) {
        String path = "";
        String os = null;
        String arch;
        String extension;
        boolean isMSI = false;

        switch (platform) {
        case WINDOWS:
        	isMSI = isMSI(version);
            if (!isMSI) {
                os = "win";
                extension = EXTENSION_ZIP;
            } else {
                extension = EXTENSION_MSI;
            }
            break;
        case LINUX:
            os = "linux";
            extension = EXTENSION;
            break;
        case OSX:
            os = "darwin";
            extension = EXTENSION;
            break;
        default:
            throw new IllegalArgumentException("Unresolvable nodeJS installer for version=" + version + ", platform=" + platform.name());
        }

        switch (cpu) {
        case i386:
            if (platform == Platform.OSX && NodeJSVersion.parseVersion(version).compareTo(new NodeJSVersion(4, 0, 0)) >= 0) {
            	throw new IllegalArgumentException("Unresolvable nodeJS installer for version=" + version + ", cpu=" + cpu.name() + ", platform=" + platform.name());
            }
            arch = "x86";
            break;
        case amd64:
            if (isMSI && NodeJSVersion.parseVersion(version).compareTo(new NodeJSVersion(4, 0, 0)) < 0) {
                path = "x64/";
            }
            arch = "x64";
            break;
        case arm64:
        case armv6l:
        case armv7l:
            if (NodeJSVersion.parseVersion(version).compareTo(new NodeJSVersion(4, 0, 0)) < 0) {
                throw new IllegalArgumentException("Unresolvable nodeJS installer for version=" + version + ", cpu=" + cpu.name() + ", platform=" + platform.name());
            }
            arch = cpu.name();
            break;
        default:
            throw new IllegalArgumentException("Unresolvable nodeJS installer for version=" + version + ", cpu=" + cpu.name());
        }

        if (os == null) {
            return MessageFormat.format("{0}node-v{1}-{2}.{3}", path, version, arch, extension);
        } else {
            return MessageFormat.format("{0}node-v{1}-{2}-{3}.{4}", path, version, os, arch, extension);
        }
    }

    public boolean isMSI(String version) {
        NodeJSVersion currentVersion = new NodeJSVersion(version);
        for (NodeJSVersionRange msiRange : MSI_RANGES) {
            if (msiRange.includes(currentVersion)) {
                return true;
            }
        }
        return false;
    }

}