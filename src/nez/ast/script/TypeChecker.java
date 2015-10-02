package nez.ast.script;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import nez.ast.Symbol;
import nez.ast.TreeVisitor;
import nez.ast.script.TypeSystem.BinaryTypeUnifier;
import nez.util.StringUtils;

public class TypeChecker extends TreeVisitor implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;
	TypeScope scope;

	public TypeChecker(ScriptContext context, TypeSystem typeSystem) {
		super(TypedTree.class);
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

	public Type type(TypedTree node) {
		Type c = (Type) visit("type", node);
		if (c != null) {
			node.setType(c);
		}
		return c;
	}

	public void enforceType(Type req, TypedTree node, Symbol label) {
		TypedTree unode = node.get(label, null);
		if (unode != null) {
			type(unode);
			node.set(label, this.typeSystem.enforceType(req, unode));
		}
	}

	public void makeCast(Type req, TypedTree node, Symbol label) {
		TypedTree unode = node.get(label, null);
		if (unode != null) {
			node.set(label, this.typeSystem.makeCast(req, unode));
		}
	}

	public void typed(TypedTree node, Type c) {
		node.setType(c);
	}

	/* TopLevel */

	public Type typeSource(TypedTree node) {
		Type t = null;
		for (int i = 0; i < node.size(); i++) {
			TypedTree sub = node.get(i);
			try {
				t = type(sub);
			} catch (TypeCheckerException e) {
				sub = e.errorTree;
				node.set(i, sub);
				t = sub.getType();
			}
		}
		return t;
	}

	public Type typeImport(TypedTree node) {
		StringBuilder sb = new StringBuilder();
		join(sb, node.get(_name));
		String path = sb.toString();
		try {
			typeSystem.addBaseClass(path);
		} catch (ClassNotFoundException e) {
			perror(node, "undefined class name: %s", path);
		}
		node.done();
		return void.class;
	}

	private void join(StringBuilder sb, TypedTree node) {
		TypedTree prefix = node.get(_prefix);
		if (prefix.size() == 2) {
			join(sb, prefix);
		} else {
			sb.append(prefix.toText());
		}
		sb.append(".").append(node.getText(_name, null));
	}

	/* FuncDecl */

	public Type typeFuncDecl(TypedTree node) {
		String name = node.getText(_name, null);
		TypedTree bodyNode = node.get(_body, null);
		if (bodyNode != null) {
			this.enterFunction();
			Type type = typeSystem.resolveType(node.get(_type, null), null);
			if (type != null) {
				this.setReturnType(type);
				typed(node.get(_type), type);
			}
			TypedTree paramsNode = node.get(_param, null);
			if (paramsNode != null) {
				for (TypedTree p : paramsNode) {
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

	public Type typeReturn(TypedTree node) {
		Type t = this.getReturnType();
		TypedTree exprNode = node.get(_expr, null);
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

	public Type typeBlock(TypedTree node) {
		for (int i = 0; i < node.size(); i++) {
			TypedTree sub = node.get(i);
			try {
				type(sub);
			} catch (TypeCheckerException e) {
				sub = e.errorTree;
				node.set(i, sub);
			}
		}
		return void.class;
	}

	public Type typeIf(TypedTree node) {
		this.enforceType(boolean.class, node, _cond);
		type(node.get(_then));
		if (node.get(_else, null) != null) {
			type(node.get(_else));
		}
		return void.class;
	}

	public Type typeWhile(TypedTree node) {

		return void.class;
	}

	public Type typeVarDecl(TypedTree node) {
		String name = node.getText(_name, null);
		Type type = typeSystem.resolveType(node.get(_type, null), null);
		TypedTree exprNode = node.get(_expr, null);
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

	public void addVariable(TypedTree nameNode, String name, Type type) {
		if (inFunction) {
			scope.setVarType(name, type);
		} else {
			typeSystem.declGlobalVariable(name, type);
		}
	}

	public Type typeName(TypedTree node) {
		String name = node.toText();
		if (this.inFunction) {
			if (this.scope.containsVariable(name)) {
				return this.scope.getVarType(name);
			}
		}
		Type t = this.typeSystem.resolveGlobalVariableType(name, null);
		if (t != null) {
			return t;
		}
		throw new TypeCheckerException(this.typeSystem, node, "undefined name: %s", name);
	}

	/* StatementExpression */
	public Type typeExpression(TypedTree node) {
		type(node.get(0));
		return void.class;
	}

	/* Expression */

	public Type typeCast(TypedTree node) {
		Type inner = type(node.get(_expr));
		Type t = this.typeSystem.resolveType(node.get(_type), null);
		if (t == null) {
			perror(node.get(_type), "undefined type");
			return inner;
		}
		Class<?> req = TypeSystem.toClass(t);
		Class<?> exp = TypeSystem.toClass(inner);
		Method m = typeSystem.getCastMethod(exp, req);
		if (m == null) {
			m = typeSystem.getConvertMethod(exp, req);
		}
		if (m == null) {
			if (req.isAssignableFrom(exp)) { // upcast
				node.setTag(_UpCast);
				return t;
			}
			if (exp.isAssignableFrom(req)) { // downcast
				node.setTag(_DownCast);
				return t;
			}
			node.setTag(_StupidCast);
			return t;
		}
		node.setMethod(true, m);
		return t;
	}

	public Type typeAssign(TypedTree node) {
		String name = node.getText(_left, null);
		Type right = type(node.get(_right));
		this.scope.setVarType(name, right);
		return right;
	}

	public Type[] typeList(TypedTree node) {
		Type[] args = new Type[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = type(node.get(i));
		}
		return args;
	}

	public Type typeApply(TypedTree node) {
		String name = node.getText(_name, "");
		TypedTree args = node.get(_param);
		Type[] types = new Type[args.size()];
		for (int i = 0; i < args.size(); i++) {
			types[i] = type(node.get(i));
		}
		return this.resolveStaticMethod("function", node, name, args, types);
	}

	public Type typeMethodApply(TypedTree node) {
		Type recv = type(node.get(_recv));
		String name = node.getText(_name, "");
		TypedTree args = node.get(_param);
		Type[] types = new Type[args.size()];
		for (int i = 0; i < args.size(); i++) {
			types[i] = type(node.get(i));
		}
		return this.resolveMethod("method", node, recv, name, args, types);
	}

	public Type typeAnd(TypedTree node) {
		this.enforceType(boolean.class, node, _left);
		this.enforceType(boolean.class, node, _right);
		return boolean.class;
	}

	public Type typeOr(TypedTree node) {
		this.enforceType(boolean.class, node, _left);
		this.enforceType(boolean.class, node, _right);
		return boolean.class;
	}

	public Type typeNot(TypedTree node) {
		this.enforceType(boolean.class, node, _expr);
		return boolean.class;
	}

	private Type typeBinary(TypedTree node, String name, BinaryTypeUnifier unifier) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		Type common = unifier.unify(typeSystem.PrimitiveType(left), typeSystem.PrimitiveType(right));
		if (left != common) {
			this.makeCast(common, node, _left);
		}
		if (right != common) {
			this.makeCast(common, node, _right);
		}
		return this.resolveStaticMethod("operator", node, name, node, common, common);
	}

	public Type typeAdd(TypedTree node) {
		return typeBinary(node, "opAdd", TypeSystem.UnifyAdditive);
	}

	public Type typeSub(TypedTree node) {
		return typeBinary(node, "opSub", TypeSystem.UnifyAdditive);
	}

	public Type typeMul(TypedTree node) {
		return typeBinary(node, "opMul", TypeSystem.UnifyAdditive);
	}

	public Type typeDiv(TypedTree node) {
		return typeBinary(node, "opDiv", TypeSystem.UnifyAdditive);
	}

	public Type typePlus(TypedTree node) {
		Type t = type(node.get(_expr));
		return this.resolveStaticMethod("operator", node, "opPlus", node, t);
	}

	public Type typeMinus(TypedTree node) {
		Type t = type(node.get(_expr));
		return this.resolveStaticMethod("operator", node, "opMinus", node, t);
	}

	public Type typeEquals(TypedTree node) {
		return typeBinary(node, "opEquals", TypeSystem.UnifyEquator);
	}

	public Type typeNotEquals(TypedTree node) {
		return typeBinary(node, "opNotEquals", TypeSystem.UnifyEquator);
	}

	public Type typeLessThan(TypedTree node) {
		return typeBinary(node, "opLessThan", TypeSystem.UnifyComparator);
	}

	public Type typeLessThanEquals(TypedTree node) {
		return typeBinary(node, "opLessThanEquals", TypeSystem.UnifyComparator);
	}

	public Type typeGreaterThan(TypedTree node) {
		return typeBinary(node, "opGreaterThan", TypeSystem.UnifyComparator);
	}

	public Type typeGreaterThanEquals(TypedTree node) {
		return typeBinary(node, "opGreaterThanEquals", TypeSystem.UnifyComparator);
	}

	public Type typeBitwiseAnd(TypedTree node) {
		return typeBinary(node, "opBitwiseAnd", TypeSystem.UnifyBitwise);
	}

	public Type typeBitwiseOr(TypedTree node) {
		return typeBinary(node, "opBitwiseOr", TypeSystem.UnifyBitwise);
	}

	public Type typeBitwiseXor(TypedTree node) {
		return typeBinary(node, "opBitwiseXor", TypeSystem.UnifyBitwise);
	}

	public Type typeCompl(TypedTree node) {
		Type t = type(node.get(_expr));
		return this.resolveStaticMethod("operator", node, "opCompl", node, t);
	}

	public Type typeNull(TypedTree node) {
		return Object.class;
	}

	public Type typeTrue(TypedTree node) {
		node.setValue(true);
		return boolean.class;
	}

	public Type typeFalse(TypedTree node) {
		node.setValue(false);
		return boolean.class;
	}

	public Type typeShort(TypedTree node) {
		return typeInteger(node);
	}

	public Type typeInteger(TypedTree node) {
		try {
			String n = node.toText();
			node.setValue(Integer.parseInt(n));
		} catch (NumberFormatException e) {
			perror(node, e.getMessage());
			node.setValue(0);
		}
		return int.class;
	}

	public Type typeLong(TypedTree node) {
		try {
			String n = node.toText();
			node.setValue(Long.parseLong(n));
		} catch (NumberFormatException e) {
			perror(node, e.getMessage());
			node.setValue(0L);
		}
		return long.class;
	}

	public Type typeFloat(TypedTree node) {
		return typeDouble(node);
	}

	public Type typeDouble(TypedTree node) {
		try {
			String n = node.toText();
			node.setValue(Double.parseDouble(n));
		} catch (NumberFormatException e) {
			perror(node, e.getMessage());
			node.setValue(0L);
		}
		return double.class;
	}

	public Type typeString(TypedTree node) {
		String t = node.toText();
		node.setValue(StringUtils.unquoteString(t));
		return String.class;
	}

	public Type typeCharacter(TypedTree node) {
		String t = StringUtils.unquoteString(node.toText());
		if (t.length() == 1) {
			node.setValue(t.charAt(0));
			return char.class;
		}
		node.setValue(t);
		return String.class;
	}

	public Type typeInterpolation(TypedTree node) {
		for (TypedTree sub : node) {
			type(sub);
		}
		node.setMethod(true, this.typeSystem.InterpolationMethod);
		return String.class;
	}

	// Utilities

	private Type resolveStaticMethod(String method, TypedTree node, String name, TypedTree argsNode, Type... args) {
		Method m = typeSystem.findCompiledMethod(name, args);
		if (m == null && argsNode instanceof TypedTree) {
			TypedTree typedNode = argsNode;
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
		node.setMethod(true, m);
		return m.getReturnType();
	}

	private Type resolveMethod(String method, TypedTree node, Type recv, String name, TypedTree argsNode, Type... args) {
		Method m = typeSystem.findCompiledMethod(TypeSystem.toClass(recv), name, args);
		if (m == null && argsNode instanceof TypedTree) {
			TypedTree typedNode = argsNode;
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
		node.setMethod(false, m);
		if (recv instanceof GenericType) {
			return ((GenericType) recv).resolve(m.getGenericReturnType());
		}
		return m.getReturnType();
	}

	private void perror(TypedTree node, String msg) {
		msg = node.formatSourceMessage("error", msg);
		this.context.log(msg);
	}

	private void perror(TypedTree node, String fmt, Object... args) {
		perror(node, String.format(fmt, args));
	}

	public void pushScope() {
		this.scope = new TypeScope(this.scope);
	}

	public void popScope() {
		this.scope = this.scope.parent;
	}

}
