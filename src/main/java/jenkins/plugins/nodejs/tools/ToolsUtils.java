/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import hudson.model.Node;
import hudson.model.DownloadService.Downloadable;
import hudson.tools.DownloadFromUrlInstaller.Installable;
import hudson.tools.DownloadFromUrlInstaller.InstallableList;
import jenkins.plugins.nodejs.Messages;
import net.sf.json.JSONObject;

/*package */ class ToolsUtils {

    private ToolsUtils() {
    }

    public static Platform getPlatform(Node node) throws DetectionFailedException {
        return Platform.of(node);
    }

    public static CPU getCPU(Node node) throws DetectionFailedException {
        return getCPU(node, false);
    }

    public static CPU getCPU(Node node, boolean force32bit) throws DetectionFailedException {
        CPU nodeCPU = CPU.of(node);
        if (force32bit) {
            if (!support32Bit(nodeCPU)) {
                throw new DetectionFailedException(Messages.SystemTools_unsupported32bitArchitecture());
            }

            // force 32 bit architecture 
            if (nodeCPU == CPU.amd64) {
                nodeCPU = CPU.i386;
            }
        }
        return nodeCPU;
    }

    private static boolean support32Bit(CPU cpu) {
        switch (cpu) {
        case armv6l:
            // 64bit start with ARMv8
        case armv7l:
        case i386:
        case amd64:
            return true;
        default:
            return false;
        }
    }

    public static List<? extends Installable> getInstallable() throws IOException {
        List<Installable> installables = Collections.emptyList();

        Downloadable downloadable = Downloadable.get("hudson.plugins.nodejs.tools.NodeJSInstaller");
        if (downloadable != null) {
            JSONObject d = downloadable.getData();
            if (d != null) {
                installables = Arrays.asList(((InstallableList) JSONObject.toBean(d, InstallableList.class)).list).stream() //
                        .filter(i -> !InstallerPathResolver.Factory.isVersionBlacklisted(i.id)) //
                        .sorted(new InstallableComparator()) //
                        .collect(Collectors.toList());
            }
        }
        return installables;
    }

}