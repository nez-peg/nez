package konoha.script;

import nez.util.FileBuilder;

public class ScriptBuilder {
	FileBuilder f;

	ScriptBuilder() {
		f = new FileBuilder();
	}

	public void write(String t) {
		f.write(t);
	}

	boolean needsWhiteSpace = false;

	public void push(String token) {
		if (this.needsWhiteSpace) {
			f.write(" ");
		}
		f.write(token);
		this.needsWhiteSpace = true;
	}

	public void openBlock(String s) {
		this.push(s);
		f.incIndent();
	}

	public void closeBlock(String s) {
		f.incIndent();
		if (s != null) {
			f.writeIndent(s);
			this.needsWhiteSpace = false;
		}
	}

	public void beginStatement(String s) {
		if (s == null) {
			f.writeIndent();
		} else {
			f.writeIndent(s);
		}
		this.needsWhiteSpace = false;
	}

	public void endStatement(String s) {
		if (s != null) {
			f.write(s);
			this.needsWhiteSpace = true;
		}
	}

}
