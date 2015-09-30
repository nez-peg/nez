package nez.ast.script;

import java.lang.reflect.Method;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.util.ConsoleUtils;

public class TypeChecker extends TreeVisitor implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;
	TypeScope scope;

	public TypeChecker(ScriptContext context, TypeSystem typeSystem) {
		this.context = context;
		this.typeSystem = typeSystem;
		this.scope = new TypeScope();
	}

	boolean inFunction = false;

	public void enterFunction() {
		inFunction = true;
	}

	public void exitFunction() {
		inFunction = false;
	}

	public Class<?> type(Tree<?> node) {
		Class<?> c = (Class<?>) visit("type", node);
		if (c != null && node instanceof TypedTree) {
			((TypedTree) node).setType(c);
		}
		return c;
	}

	public void enforceType(Class<?> req, Tree<?> node, Symbol label) {
		Tree<?> unode = node.get(label, null);
		if (unode instanceof TypedTree) {
			TypedTree tnode = (TypedTree) unode;
			type(tnode);
			((TypedTree) node).set(label, this.typeSystem.enforceType(req, tnode));
			return;
		}
		if (unode != null) {
			type(unode);
		}
	}

	/* TopLevel */

	public Class<?> typeSource(Tree<?> node) {
		Class<?> t = null;
		for (Tree<?> sub : node) {
			t = type(sub);
		}
		return t;
	}

	public Class<?> typeImport(Tree<?> node) {
		return null;
	}

	/* Statement */

	public Class<?> typeVarDecl(Tree<?> node) {
		String name = node.getText(_name, null);
		Class<?> type = typeSystem.resolveType(node.get(_type, null), null);
		Tree<?> exprNode = node.get(_expr, null);
		if (type == null) {
			if (exprNode == null) {
				perror(node.get(_name), "ungiven type");
				type = Object.class;
			} else {
				type = type(exprNode);
			}
		} else {
			if (exprNode != null) {
				enforceType(type, node, _expr);
			}
		}
		performVarDecl(node.get(_name), name, type);
		return void.class;
	}

	public void performVarDecl(Tree<?> nameNode, String name, Class<?> type) {
		if (inFunction) {
			System.out.printf("TODO: var decl %s : %s\n", name, type);

		} else {
			typeSystem.declGlobalVariable(name, type);
		}
	}

	public Class<?> typeName(Tree<?> node) {
		String name = node.toText();
		if (!this.scope.containsVariable(name)) {
			perror(node, "undefined name: " + name);
		}
		return this.scope.getVarType(name);
	}

	/* StatementExpression */
	public Class<?> typeExpression(Tree<?> node) {
		type(node.get(0));
		return void.class;
	}

	/* Expression */

	public Class<?> typeAssign(Tree<?> node) {
		String name = node.getText(_left, null);
		Class<?> right = type(node.get(_right));
		this.scope.setVarType(name, right);
		return right;
	}

	public Class<?>[] typeList(Tree<?> node) {
		Class<?>[] args = new Class<?>[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = type(node.get(i));
		}
		return args;
	}

	public Class<?> typeApply(Tree<?> node) {
		String name = node.getText(_name, "");
		Tree<?> args = node.get(_param);
		Class<?>[] types = new Class<?>[args.size()];
		for (int i = 0; i < args.size(); i++) {
			types[i] = type(node.get(i));
		}
		return this.resolve("function", node, name, args, types);
	}

	public Class<?> typeAdd(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opAdd", node, left, right);
	}

	public Class<?> typeSub(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opSub", node, left, right);
	}

	public Class<?> typeMul(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opMul", node, left, right);
	}

	public Class<?> typeDiv(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opDiv", node, left, right);
	}

	public Class<?> typeEquals(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opEquals", node, left, right);
	}

	public Class<?> typeNotEquals(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opNotEquals", node, left, right);
	}

	public Class<?> typeLessThan(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opLessThan", node, left, right);
	}

	public Class<?> typeLessThanEquals(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opLessThanEquals", node, left, right);
	}

	public Class<?> typeGreaterThan(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opGreaterThan", node, left, right);
	}

	public Class<?> typeGreaterThanEquals(Tree<?> node) {
		Class<?> left = type(node.get(_left));
		Class<?> right = type(node.get(_right));
		return this.resolve("operator", node, "opGreaterThanEquals", node, left, right);
	}

	public Class<?> typeNull(Tree<?> node) {
		return Object.class;
	}

	public Class<?> typeTrue(Tree<?> node) {
		return boolean.class;
	}

	public Class<?> typeFalse(Tree<?> node) {
		return boolean.class;
	}

	public Class<?> typeShort(Tree<?> node) {
		return int.class;
	}

	public Class<?> typeInteger(Tree<?> node) {
		return int.class;
	}

	public Class<?> typeLong(Tree<?> node) {
		return long.class;
	}

	public Class<?> typeFloat(Tree<?> node) {
		return double.class;
	}

	public Class<?> typeDouble(Tree<?> node) {
		return double.class;
	}

	public Class<?> typeString(Tree<?> node) {
		return String.class;
	}

	// Utilities

	private Class<?> resolve(String method, Tree<?> node, String name, Tree<?> argsNode, Class<?>... args) {
		Method m = typeSystem.findCompiledMethod(name, args);
		if (m == null && argsNode instanceof TypedTree) {
			TypedTree typedNode = (TypedTree) argsNode;
			m = typeSystem.findDefaultMethod(name, args.length);
			if (m != null) {
				Class<?>[] p = m.getParameterTypes();
				for (int i = 0; i < p.length; i++) {
					typedNode.set(i, typeSystem.enforceType(p[i], typedNode.get(i)));
				}
			}
		}
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

	public void pushScope() {
		this.scope = new TypeScope(this.scope);
	}

	public void popScope() {
		this.scope = this.scope.parent;
	}

}
