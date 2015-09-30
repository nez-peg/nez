package nez.ast.script;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.util.ConsoleUtils;
import nez.util.UList;

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
	Type returnType = null;

	public void enterFunction() {
		returnType = null;
		inFunction = true;
	}

	public void exitFunction() {
		inFunction = false;
	}

	public void setReturnType(Type t) {
		this.returnType = t;
	}

	public Type getReturnType() {
		return this.returnType;
	}

	public Type type(Tree<?> node) {
		Type c = (Type) visit("type", node);
		if (c != null && node instanceof TypedTree) {
			((TypedTree) node).setType(c);
		}
		return c;
	}

	public void enforceType(Type req, Tree<?> node, Symbol label) {
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

	public void typed(Tree<?> node, Type c) {
		if (node instanceof TypedTree) {
			((TypedTree) node).setType(c);
		}
	}

	/* TopLevel */

	public Type typeSource(Tree<?> node) {
		Type t = null;
		for (Tree<?> sub : node) {
			t = type(sub);
		}
		return t;
	}

	public Type typeImport(Tree<?> node) {
		return null;
	}

	/* FuncDecl */

	public Type typeFuncDecl(Tree<?> node) {
		String name = node.getText(_name, null);
		Tree<?> bodyNode = node.get(_body, null);
		if (bodyNode != null) {
			this.enterFunction();
			Type type = typeSystem.resolveType(node.get(_type, null), null);
			if (type != null) {
				this.setReturnType(type);
				typed(node.get(_type), type);
			}
			Tree<?> paramsNode = node.get(_param, null);
			if (paramsNode != null) {
				for (Tree<?> p : paramsNode) {
					String pname = p.getText(_name, null);
					Type ptype = typeSystem.resolveType(p.get(_type, null), Object.class);
					this.addVariable(p.get(_name), pname, ptype);
					typed(p, ptype);
				}
			}
			type(bodyNode);
			this.exitFunction();
		}
		if (this.getReturnType() == null) {
			this.setReturnType(void.class);
		}
		typed(node.get(_name), this.getReturnType());
		return void.class;
	}

	public Type typeReturn(Tree<?> node) {
		Type t = this.getReturnType();
		Tree<?> exprNode = node.get(_expr, null);
		if (t == null) {
			if (exprNode == null) {
				this.setReturnType(void.class);
			} else {
				this.setReturnType(type(exprNode));
			}
			return void.class;
		}
		if (t != void.class) {
			this.enforceType(t, node, _expr);
		}
		return void.class;
	}

	/* Statement */

	public Type typeBlock(Tree<?> node) {
		Type t = null;
		for (Tree<?> sub : node) {
			t = type(sub);
		}
		return void.class;
	}

	public Type typeVarDecl(Tree<?> node) {
		String name = node.getText(_name, null);
		Type type = typeSystem.resolveType(node.get(_type, null), null);
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
		addVariable(node.get(_name), name, type);
		return void.class;
	}

	public void addVariable(Tree<?> nameNode, String name, Type type) {
		if (inFunction) {
			scope.setVarType(name, type);
		} else {
			typeSystem.declGlobalVariable(name, type);
		}
	}

	public Type typeName(Tree<?> node) {
		String name = node.toText();
		if (!this.scope.containsVariable(name)) {
			perror(node, "undefined name: " + name);
		}
		return this.scope.getVarType(name);
	}

	/* StatementExpression */
	public Type typeExpression(Tree<?> node) {
		type(node.get(0));
		return void.class;
	}

	/* Expression */

	public Type typeAssign(Tree<?> node) {
		String name = node.getText(_left, null);
		Type right = type(node.get(_right));
		this.scope.setVarType(name, right);
		return right;
	}

	public Type[] typeList(Tree<?> node) {
		Type[] args = new Type[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = type(node.get(i));
		}
		return args;
	}

	public Type typeApply(Tree<?> node) {
		String name = node.getText(_name, "");
		Tree<?> args = node.get(_param);
		Type[] types = new Type[args.size()];
		for (int i = 0; i < args.size(); i++) {
			types[i] = type(node.get(i));
		}
		return this.resolve("function", node, name, args, types);
	}

	public Type typeMethodApply(Tree<?> node) {
		Type recv = type(node.get(_recv));
		String name = node.getText(_name, "");
		Tree<?> args = node.get(_param);
		Type[] types = new Type[args.size()];
		for (int i = 0; i < args.size(); i++) {
			types[i] = type(node.get(i));
		}
		return this.resolve("method", node, recv, name, args, types);
	}

	public Type typeAdd(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opAdd", node, left, right);
	}

	public Type typeSub(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opSub", node, left, right);
	}

	public Type typeMul(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opMul", node, left, right);
	}

	public Type typeDiv(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opDiv", node, left, right);
	}

	public Type typeEquals(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opEquals", node, left, right);
	}

	public Type typeNotEquals(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opNotEquals", node, left, right);
	}

	public Type typeLessThan(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opLessThan", node, left, right);
	}

	public Type typeLessThanEquals(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opLessThanEquals", node, left, right);
	}

	public Type typeGreaterThan(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opGreaterThan", node, left, right);
	}

	public Type typeGreaterThanEquals(Tree<?> node) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		return this.resolve("operator", node, "opGreaterThanEquals", node, left, right);
	}

	public Type typeNull(Tree<?> node) {
		return UList.class;
	}

	public Type typeTrue(Tree<?> node) {
		return boolean.class;
	}

	public Type typeFalse(Tree<?> node) {
		return boolean.class;
	}

	public Type typeShort(Tree<?> node) {
		return int.class;
	}

	public Type typeInteger(Tree<?> node) {
		return int.class;
	}

	public Type typeLong(Tree<?> node) {
		return long.class;
	}

	public Type typeFloat(Tree<?> node) {
		return double.class;
	}

	public Type typeDouble(Tree<?> node) {
		return double.class;
	}

	public Type typeString(Tree<?> node) {
		return String.class;
	}

	// Utilities

	private Type resolve(String method, Tree<?> node, String name, Tree<?> argsNode, Type... args) {
		Method m = typeSystem.findCompiledMethod(name, args);
		if (m == null && argsNode instanceof TypedTree) {
			TypedTree typedNode = (TypedTree) argsNode;
			m = typeSystem.findDefaultMethod(name, args.length);
			if (m != null) {
				Type[] p = m.getParameterTypes();
				for (int i = 0; i < p.length; i++) {
					typedNode.set(i, typeSystem.enforceType(p[i], typedNode.get(i)));
				}
			}
		}
		if (m == null) {
			perror(node, "undefined %s: %s", method, name);
			return null;
		}
		if (node instanceof TypedTree) {
			((TypedTree) node).setMethod(m);
		}
		return m.getReturnType();
	}

	private Type resolve(String method, Tree<?> node, Type recv, String name, Tree<?> argsNode, Type... args) {
		Method m = typeSystem.findCompiledMethod(TypeSystem.toClass(recv), name, args);
		if (m == null && argsNode instanceof TypedTree) {
			TypedTree typedNode = (TypedTree) argsNode;
			m = typeSystem.findDefaultMethod(TypeSystem.toClass(recv), name, args.length);
			if (m != null) {
				Type[] p = m.getParameterTypes();
				for (int i = 0; i < p.length; i++) {
					typedNode.set(i, typeSystem.enforceType(p[i], typedNode.get(i)));
				}
			}
		}
		if (m == null) {
			perror(node, "undefined %s: %s", method, name);
			return null;
		}
		if (node instanceof TypedTree) {
			((TypedTree) node).setMethod(m);
		}
		if (recv instanceof GenericType) {
			return ((GenericType) recv).resolve(m.getGenericReturnType());
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
