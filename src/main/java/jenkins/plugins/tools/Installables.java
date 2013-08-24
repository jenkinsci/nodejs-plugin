package jenkins.plugins.tools;

import hudson.tools.DownloadFromUrlInstaller;

/**
 * @author fcamblor
 */
public class Installables {
    public static DownloadFromUrlInstaller.Installable clone(DownloadFromUrlInstaller.Installable inst){
        DownloadFromUrlInstaller.Installable clone = new DownloadFromUrlInstaller.Installable();
        clone.id = inst.id;
        clone.url = inst.url;
        clone.name = inst.name;
        return clone;
    }
}
