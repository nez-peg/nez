package nez.ast.script;

import java.io.IOException;
import java.lang.reflect.Type;

import nez.Parser;
import nez.io.SourceContext;
import nez.util.ConsoleUtils;

public class ScriptContext {
	public final static boolean verbose = true;
	private Parser parser;
	private TypeSystem typeSystem;
	private TypeChecker typechecker;
	private Interpreter interpreter;

	public ScriptContext(Parser parser) {
		this.parser = parser;
		this.typeSystem = new TypeSystem(this, null/* FIXME */);
		this.typechecker = new TypeChecker(this, typeSystem);
		this.interpreter = new Interpreter(this, typeSystem);
		// new TypeChecker2();
	}

	public void load(String path) throws IOException {
		eval(SourceContext.newFileContext(path));
	}

	public Object eval2(String uri, int linenum, String script) {
		return eval(SourceContext.newStringContext(uri, linenum, script));
	}

	private Object eval(SourceContext source) {
		TypedTree node = (TypedTree) this.parser.parse(source, new TypedTree());
		if (node == null) {
			println(source.getSyntaxErrorMessage());
			return this; // nothing
		}
		if (verbose) {
			ConsoleUtils.println("[typechcking]");
			ConsoleUtils.println("|    ", node);
		}
		typechecker.doType(node);
		if (verbose) {
			ConsoleUtils.println("[evaluating]");
			ConsoleUtils.println("|    ", node);
		}
		return interpreter.eval(node);
	}

	public Object get(String name) {
		GlobalVariable gv = this.typeSystem.getGlobalVariable(name);
		if (gv != null) {
			return Reflector.getStatic(gv.getField());
		}
		return null;
	}

	public void set(String name, Object value) {
		GlobalVariable gv = this.typeSystem.getGlobalVariable(name);
		if (gv == null) {
			Type type = Reflector.infer(value);
			gv = this.typeSystem.newGlobalVariable(type, name);
		}
		Reflector.setStatic(gv.getField(), value);
	}

	public final void println(Object o) {
		ConsoleUtils.println(o);
	}

	public void log(String msg) {
		ConsoleUtils.println(msg);
	}

}
