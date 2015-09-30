package nez.ast.script.asm;

import nez.ast.Tree;
import nez.ast.script.CommonSymbols;
import nez.ast.script.TypeSystem;
import nez.ast.script.TypedTree;

public class ScriptCompiler {
	TypeSystem typeSystem;
	final ScriptClassLoader cLoader;
	private ScriptCompilerAsm compilerAsm;

	public ScriptCompiler(TypeSystem typeSystem) {
		this.typeSystem = typeSystem;
		this.cLoader = new ScriptClassLoader();
		this.compilerAsm = new ScriptCompilerAsm(this.typeSystem, this.cLoader);
	}

	public void compileClassDecl(Tree<?> node) {

	}

	public void compileFuncDecl(Tree<?> node) {
		Class<?> function = this.compilerAsm.compileFuncDecl(node.getText(CommonSymbols._name, null), (TypedTree) node);
		typeSystem.add(function);
	}
}
