package jenkins.plugins.nodejs;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.util.Map;

import jenkins.plugins.nodejs.tools.NodeJSExecType;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * This class executes a GruntJs task using the given parameters.
 * 
 * @author ohanetz
 */
public class GruntJsTaskExecutor extends Builder {

	private String workingPath;
	private String gruntFilePath;
	private String gruntTask;
	private String nodeJSInstallationName;

	/**
	 * Constructs a {@link GruntJsTaskExecutor} with specified command.
	 * @param command
	 *            the NodeJS script
	 */
	@DataBoundConstructor
	public GruntJsTaskExecutor(final String workingPath, final String gruntFilePath, final String gruntTask, final String nodeJSInstallationName) {
		super();
		this.workingPath = workingPath;
		this.gruntFilePath = gruntFilePath;
		this.gruntTask = gruntTask;
		this.nodeJSInstallationName = nodeJSInstallationName;
	}

	@Override
	public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
		return perform(build,launcher,(TaskListener)listener);
	}

	public boolean perform(AbstractBuild<?,?> build, Launcher launcher, TaskListener listener) throws InterruptedException {
		FilePath ws = build.getWorkspace();
		if (ws == null) {
			Node node = build.getBuiltOn();
			if (node == null) {
				throw new NullPointerException("no such build node: " + build.getBuiltOnStr());
			}
			throw new NullPointerException("no workspace from node " + node + " which is computer " + node.toComputer() + " and has channel " + node.getChannel());
		}

		workingPath = ws.getRemote().concat(workingPath);
		if (gruntFilePath.equals("")) {
			gruntFilePath = "Gruntfile.js";
		} else {
			gruntFilePath = ws.getRemote().concat(gruntFilePath);
		}


		int r;
		try {
			EnvVars envVars = build.getEnvironment(listener);
			// on Windows environment variables are converted to all upper case,
			// but no such conversions are done on Unix, so to make this cross-platform,
			// convert variables to all upper cases.
			for(Map.Entry<String,String> e : build.getBuildVariables().entrySet())
				envVars.put(e.getKey(),e.getValue());

			// Building arguments
			ArgumentListBuilder args = new ArgumentListBuilder();

			NodeJSInstallation selectedInstallation = NodeJSPlugin.instance().findInstallationByName(nodeJSInstallationName);
			selectedInstallation = selectedInstallation.forNode(build.getBuiltOn(), listener);
			selectedInstallation = selectedInstallation.forEnvironment(envVars);
			String exe = selectedInstallation.getExecutable(launcher, NodeJSExecType.GRUNTJS);
			args.add(exe);

			args.add("-v");
			args.add("--gruntfile");
			args.add(gruntFilePath);
			args.add(gruntTask);

			r = launcher.launch().cmds(args).envs(envVars).stdout(listener).pwd(workingPath).join();
		} catch (IOException e) {
			Util.displayIOException(e,listener);
			e.printStackTrace(listener.fatalError(hudson.tasks.Messages.CommandInterpreter_CommandFailed()));
			r = -1;
		}
		return r==0;

	}


	/**
	 * @return the descriptor
	 */
	@Override
	public Descriptor<Builder> getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension
	public static final NodeJsDescriptor DESCRIPTOR = new NodeJsDescriptor();

	/**
	 * Provides builder details for the job configuration page.
	 * @author cliffano, ohanetz
	 */
	public static final class NodeJsDescriptor extends Descriptor<Builder> {

		/**
		 * Constructs a {@link NodeJsDescriptor}.
		 */
		private NodeJsDescriptor() {
			super(GruntJsTaskExecutor.class);
		}

		/**
		 * Retrieve the NodeJS script from the job configuration page, pass it
		 * to a new command interpreter.
		 * @param request
		 *            the Stapler request
		 * @param json
		 *            the JSON object
		 * @return new instance of {@link GruntJsTaskExecutor}
		 */
		@Override
		public Builder newInstance(final StaplerRequest request,
				final JSONObject json) {
			return new GruntJsTaskExecutor(
					json.getString("workingPath"), 
					json.getString("gruntFilePath"), 
					json.getString("gruntTask"), 
					json.getString("nodejs_installationName"));
		}

		/**
		 * @return the builder instruction
		 */
		public String getDisplayName() {
			return "Execute GruntJs task";
		}

		/**
		 * @return available node js installations
		 */
		public NodeJSInstallation[] getInstallations() {
			return NodeJSPlugin.instance().getInstallations();
		}

		/**
		 * @return the help file URL path
		 */
		@Override
		public String getHelpFile() {
			return "/plugin/nodejs/help.html";
		}
	}
}
