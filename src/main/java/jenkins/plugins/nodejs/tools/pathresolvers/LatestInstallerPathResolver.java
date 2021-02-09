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
package jenkins.plugins.nodejs.tools.pathresolvers;

import jenkins.plugins.nodejs.Messages;
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
     *
     * @see
     * jenkins.plugins.nodejs.tools.InstallerPathResolver#resolvePathFor(java.
     * lang.String, jenkins.plugins.nodejs.tools.Platform,
     * jenkins.plugins.nodejs.tools.CPU)
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
        case SUNOS:
            os = "sunos";
            extension = EXTENSION;
            break;
        case AIX:
            os = "aix";
            extension = EXTENSION;
            break;
        default:
            throw new IllegalArgumentException(Messages.InstallerPathResolver_unsupportedOS(version, platform.name()));
        }

        NodeJSVersion nodeVersion = NodeJSVersion.parseVersion(version);
        switch (cpu) {
        case i386:
            if (platform == Platform.OSX && nodeVersion.compareTo(new NodeJSVersion(4, 0, 0)) >= 0 //
                    || ((platform == Platform.SUNOS || platform == Platform.LINUX)
                            && nodeVersion.compareTo(new NodeJSVersion(10, 0, 0)) >= 0)) {
                throw new IllegalArgumentException(Messages.InstallerPathResolver_unsupportedArch(version, cpu.name(), platform.name()));
            }
            arch = "x86";
            break;
        case amd64:
            if (platform == Platform.SUNOS && //
                    (new NodeJSVersionRange("[7, 7.5)").includes(nodeVersion) || nodeVersion.compareTo(new NodeJSVersion(0, 12, 18)) == 0)) {
                throw new IllegalArgumentException(Messages.InstallerPathResolver_unsupportedArch(version, cpu.name(), platform.name()));
            }
            if (isMSI && nodeVersion.compareTo(new NodeJSVersion(4, 0, 0)) < 0) {
                path = "x64/";
            }
            arch = "x64";
            break;
        case arm64:
            if (nodeVersion.compareTo(new NodeJSVersion(4, 0, 0)) < 0) {
                throw new IllegalArgumentException(Messages.InstallerPathResolver_unsupportedArch(version, cpu.name(), platform.name()));
            }
            arch = cpu.name();
            break;
        case armv6l:
            if (nodeVersion.compareTo(new NodeJSVersion(12, 0, 0)) >= 0 || nodeVersion.compareTo(new NodeJSVersion(8, 6, 0)) == 0 || nodeVersion.compareTo(new NodeJSVersion(4, 0, 0)) < 0) {
                throw new IllegalArgumentException(Messages.InstallerPathResolver_unsupportedArch(version, cpu.name(), platform.name()));
            }
            arch = cpu.name();
            break;
        case armv7l:
            if (nodeVersion.compareTo(new NodeJSVersion(4, 0, 0)) < 0) {
                throw new IllegalArgumentException(Messages.InstallerPathResolver_unsupportedArch(version, cpu.name(), platform.name()));
            }
            arch = cpu.name();
            break;
        case ppc64:
            if (platform != Platform.AIX || nodeVersion.compareTo(new NodeJSVersion(6, 7, 0)) < 0) {
                throw new IllegalArgumentException(Messages.InstallerPathResolver_unsupportedArch(version, cpu.name(), platform.name()));
            }
            arch = cpu.name();
            break;
        default:
            throw new IllegalArgumentException(Messages.InstallerPathResolver_unsupportedArch(version, cpu.name(), "unknown"));
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