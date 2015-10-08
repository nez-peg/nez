package nez.ast.script;

public class CommonContext {

	protected boolean shellMode = false;
	protected boolean verboseMode = false;
	protected boolean debugMode = false;

	public void setShellMode(boolean b) {
		this.shellMode = b;
	}

	public void setVerboseMode(boolean b) {
		this.verboseMode = b;
	}

	public void setDebugMode(boolean b) {
		this.debugMode = b;
	}

}
