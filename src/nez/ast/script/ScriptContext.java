package nez.ast.script;

import java.io.IOException;
import java.lang.reflect.Type;

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
		this.typeSystem = new TypeSystem(this);
		this.typechecker = new TypeChecker(this, typeSystem);
		this.interpreter = new Interpreter(this, typeSystem);
		// new TypeChecker2();
	}

	public void setShellMode(boolean b) {
		this.typeSystem.setShellMode(b);
	}

	public void setVerboseMode(boolean b) {
		this.typeSystem.setVerboseMode(b);
	}

	public final void load(String path) throws IOException {
		eval(SourceContext.newFileContext(path));
	}

	public final Object eval2(String uri, int linenum, String script) {
		return eval(SourceContext.newStringContext(uri, linenum, script));
	}

	public final Object eval(SourceContext source) {
		TypedTree node = (TypedTree) this.parser.parse(source, new TypedTree());
		if (node == null) {
			println(source.getSyntaxErrorMessage());
			return Interpreter.empty; // nothing
		}
		if (node.is(CommonSymbols._Source)) {
			return evalSource(node);
		}
		return evalTopLevel(node);
	}

	Object evalSource(TypedTree node) {
		Object result = Interpreter.empty;
		boolean foundError = false;
		for (TypedTree sub : node) {
			if (this.typeSystem.verboseMode) {
				ConsoleUtils.println("[Parsed]");
				ConsoleUtils.println("    ", sub);
			}
			try {
				typechecker.visit(sub);
				if (this.typeSystem.verboseMode) {
					ConsoleUtils.println("[Typed]");
					ConsoleUtils.println("    ", sub);
				}
				if (!foundError) {
					result = interpreter.eval(sub);
					if (sub.getType() == void.class) {
						result = Interpreter.empty;
					}
				}
			} catch (TypeCheckerException e) {
				foundError = true;
				log(e.getMessage());
			}
		}
		return foundError ? Interpreter.empty : result;
	}

	Object evalTopLevel(TypedTree sub) {
		if (this.typeSystem.verboseMode) {
			ConsoleUtils.println("[Parsed]");
			ConsoleUtils.println("    ", sub);
		}
		try {
			typechecker.visit(sub);
			if (this.typeSystem.verboseMode) {
				ConsoleUtils.println("[Typed]");
				ConsoleUtils.println("    ", sub);
			}
			return interpreter.eval(sub);
		} catch (TypeCheckerException e) {
			log(e.getMessage());
		}
		return Interpreter.empty;
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
		int c = msg.indexOf("[error]") > 0 ? 31 : 32;
		ConsoleUtils.begin(c);
		ConsoleUtils.println(msg);
		ConsoleUtils.end();
	}

}
