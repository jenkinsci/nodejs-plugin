package jenkins.plugins.nodejs.tools;

/**
 * This enum eases adding support for additional execution commands.
 * Each command is initiated with windows and unix variation.
 * @author hanetz
 *
 */
public enum NodeJSExecType {
	NODEJS(NodeJSCommands.WINDOWS_NODEJS_COMMAND, NodeJSCommands.UNIX_NODEJS_COMMAND),
	GRUNTJS(NodeJSCommands.WINDOWS_GRUNTJS_COMMAND, NodeJSCommands.UNIX_GRUNTJS_COMMAND);
	
	
	private String windowsCommand;
	private String unixCommand;
	
	private NodeJSExecType(String windowsCommand, String unixCommand) {
		this.windowsCommand = windowsCommand;
		this.unixCommand = unixCommand;
	}
	
	
	public String getWindowsCommand() {
		return windowsCommand;
	}
	
	public String getUnixCommand() {
		return unixCommand;
	}
}
