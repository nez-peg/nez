package nez.ast.script.asm;

import java.lang.reflect.Method;

import konoha.Function;
import nez.ast.Tree;
import nez.ast.script.CommonSymbols;
import nez.ast.script.TypeSystem;
import nez.ast.script.TypedTree;
import nez.main.Verbose;

public class ScriptCompiler {
	TypeSystem typeSystem;
	final ScriptClassLoader cLoader;
	private ScriptCompilerAsm asm;

	public ScriptCompiler(TypeSystem typeSystem) {
		this.typeSystem = typeSystem;
		this.cLoader = new ScriptClassLoader();
		this.asm = new ScriptCompilerAsm(this.typeSystem, this.cLoader);
		this.typeSystem.init(this);
	}

	public Class<?> compileGlobalVariable(Class<?> type, String name) {
		return this.asm.compileGlobalVariableClass(type, name);
	}

	public Function compileStaticFunctionObject(Method staticMethod) {
		Class<?> c = this.asm.compileFunctionClass(staticMethod);
		try {
			return (Function) c.newInstance();
		} catch (InstantiationException e) {
			Verbose.traceException(e);
		} catch (IllegalAccessException e) {
			Verbose.traceException(e);
		}
		return null;
	}

	public Function compileFunction(Tree<?> node) {
		return null;
	}

	public void compileClassDecl(Tree<?> node) {

	}

	public void compileFuncDecl(Tree<?> node) {
		Class<?> function = this.asm.compileStaticFuncDecl(node.getText(CommonSymbols._name, null), (TypedTree) node);
		typeSystem.loadStaticFunctionClass(function, true);
	}
}
