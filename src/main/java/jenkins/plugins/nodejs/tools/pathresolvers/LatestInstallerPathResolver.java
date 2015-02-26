package jenkins.plugins.nodejs.tools.pathresolvers;

import jenkins.plugins.nodejs.tools.InstallerPathResolver;
import jenkins.plugins.nodejs.tools.NodeJSInstaller;

/**
 * @author fcamblor
 */
public class LatestInstallerPathResolver implements InstallerPathResolver {
    private static final String EXTENSION = ".tar.gz";
    private static final String EXTENSION_WIN = ".msi";
    
    public String resolvePathFor(String version, NodeJSInstaller.Platform platform, NodeJSInstaller.CPU cpu) {
        if(platform== NodeJSInstaller.Platform.MAC){
            if(cpu == NodeJSInstaller.CPU.amd64){
                return "node-v"+version+"-darwin-x64"+EXTENSION;
            } else if(cpu == NodeJSInstaller.CPU.i386){
                return "node-v"+version+"-darwin-x86"+EXTENSION;
            }
        } else if(platform == NodeJSInstaller.Platform.LINUX){
            if(cpu == NodeJSInstaller.CPU.amd64){
                return "node-v"+version+"-linux-x64"+EXTENSION;
            } else if(cpu == NodeJSInstaller.CPU.i386){
                return "node-v"+version+"-linux-x86"+EXTENSION;
            }
        } else if (platform == NodeJSInstaller.Platform.WINDOWS){
        	 if(cpu == NodeJSInstaller.CPU.amd64){
                 return "x64/node-v"+version+"-x64"+EXTENSION_WIN;
             } else if(cpu == NodeJSInstaller.CPU.i386){
                 return "node-v"+version+"-x86"+EXTENSION_WIN;
             }
        }
        throw new IllegalArgumentException("Unresolvable nodeJS installer for version="+version+", platform="+platform.name()+", cpu="+cpu.name());
    }

    public String extractArchiveIntermediateDirectoryName(String relativeDownloadPath) {
        return relativeDownloadPath.substring(relativeDownloadPath.lastIndexOf("/")+1, relativeDownloadPath.lastIndexOf(EXTENSION));
    }
}
