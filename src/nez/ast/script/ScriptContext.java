package nez.ast.script;

import java.io.IOException;

import nez.Parser;
import nez.io.SourceContext;
import nez.util.ConsoleUtils;

public class ScriptContext {
	private Parser parser;
	private TypeSystem typeSystem;
	private TypeChecker typechecker;
	private Interpreter interpreter;

	public ScriptContext(Parser parser) {
		this.parser = parser;
		this.typeSystem = new TypeSystem();
		this.typechecker = new TypeChecker(this, typeSystem);
		this.interpreter = new Interpreter(this, typeSystem);
	}

	public void load(String path) throws IOException {
		eval(SourceContext.newFileContext(path));
	}

	public Object eval(String uri, int linenum, String script) {
		return eval(SourceContext.newStringContext(uri, linenum, script));
	}

	private Object eval(SourceContext source) {
		TypedTree node = (TypedTree) this.parser.parse(source, new TypedTree());
		if (node == null) {
			println(source.getSyntaxErrorMessage());
			return this; // nothing
		}
		System.out.println("typechecking " + node + "...\n");
		typechecker.type(node);
		System.out.println("evaluating " + node + "...\n");
		return interpreter.eval(node);
	}

	public Object get(String name) {
		return null;
	}

	public void set(String name, Object value) {

	}

	public final void println(Object o) {
		ConsoleUtils.println(o);
	}

}
