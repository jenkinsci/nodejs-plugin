package jenkins.plugins.nodejs.tools;

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
