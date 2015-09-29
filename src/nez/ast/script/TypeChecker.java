package nez.ast.script;

import java.lang.reflect.Method;

import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.ast.script.asm.CommonSymbols;
import nez.util.ConsoleUtils;

public class TypeChecker extends TreeVisitor implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;

	public TypeChecker(ScriptContext context, TypeSystem typeSystem) {
		this.context = context;
		this.typeSystem = typeSystem;
	}

	public Class<?> type(Tree<?> node) {
		Class<?> c = (Class<?>) visit("type", node);
		if (c != null && node instanceof TypedTree) {
			((TypedTree) node).setType(c);
		}
		return c;
	}

	public Class<?> typeAdd(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", "opAdd", node, left, right);
	}

	public Class<?> typeString(Tree<?> node) {
		return String.class;
	}

	public Class<?> typeInteger(Tree<?> node) {
		return int.class;
	}

	public Class<?> typeDouble(Tree<?> node) {
		return double.class;
	}

	// Utilities

	private Class<?> resolve(String method, String name, Tree<?> node, Class<?>... args) {
		Method m = typeSystem.findMethod(name, args);
		if (m == null) {
			perror(node, "undefined %s: %s", method, args);
			return null;
		}
		if (node instanceof TypedTree) {
			((TypedTree) node).setMethod(m);
		}
		return m.getReturnType();
	}

	private void perror(Tree<?> node, String msg) {
		msg = node.formatSourceMessage("error", msg);
		ConsoleUtils.println(msg);
		// throw new RuntimeException(msg);
	}

	private void perror(Tree<?> node, String fmt, Object... args) {
		perror(node, String.format(fmt, args));
	}

}
