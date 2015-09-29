package nez.ast.script.asm;

import nez.ast.Tree;
import nez.ast.script.TypeSystem;

public class ScriptCompiler {
	TypeSystem typeSystem;
	final ScriptClassLoader cLoader = null;

	public ScriptCompiler(TypeSystem typeSystem) {
		this.typeSystem = typeSystem;
		this.cLoader = new ScriptClassLoader();
	}

	public void compileClassDecl(Tree<?> node) {

	}

	public void compileFuncDecl(Tree<?> node) {

	}
}
