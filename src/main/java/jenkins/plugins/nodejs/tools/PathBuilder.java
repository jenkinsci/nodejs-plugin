package jenkins.plugins.nodejs.tools;

import hudson.FilePath;
import hudson.model.Computer;

import java.io.File;
import java.io.IOException;

import jenkins.plugins.nodejs.tools.NodeJSInstaller.Platform;

import com.google.common.base.Throwables;

public class PathBuilder {
	public static String getPathSeperator() throws IOException {
		String pathSeparator = File.pathSeparator;

		try {
			Computer slave = Computer.currentComputer();
			String slavePathSeparator = (String) slave.getSystemProperties()
					.get("path.separator");

			if (slavePathSeparator != null) {
				pathSeparator = slavePathSeparator;
			}
		} catch (InterruptedException e) {
			Throwables.propagate(e);
		}
		return pathSeparator;
	}

	public static String buildPathEnvOverwrite(Platform platform,
			FilePath addedPathElement) throws IOException {

		String overriddenPaths = addedPathElement + getPathSeperator();
		if (platform == Platform.WINDOWS) {
			overriddenPaths += "%PATH%";
		} else {
			overriddenPaths += "$PATH";
		}

		return overriddenPaths;
	}
}
