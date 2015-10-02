package nez.ast.script;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import nez.ast.Symbol;
import nez.ast.TreeVisitor;
import nez.ast.script.TypeSystem.BinaryTypeUnifier;
import nez.util.StringUtils;
import nez.util.UList;

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

	public Type typeField(TypedTree node) {
		Class<?> c = TypeSystem.toClass(type(node.get(_recv)));
		String name = node.getText(_name, "");
		Field f = typeSystem.getField(c, name);
		if (f != null) {
			node.setValue(f);
			return f.getType();
		}
		if (typeSystem.isDynamic(c)) {
			node.setMethod(true, typeSystem.DynamicGetter);
			return typeSystem.dynamicType();
		}
		throw new TypeCheckerException(typeSystem, node, "undefined field %s of %s", name, typeSystem.name(c));
	}

	public Type typeApply(TypedTree node) {
		String name = node.getText(_name, "");
		TypedTree args = node.get(_param);
		Type[] types = typeApplyArguments(args);
		int start = this.bufferMethods.size();
		Method m = this.typeSystem.resolveFunctionMethod(name, types, bufferMethods, args);
		return this.resolvedMethod(node, false, m, start, "funciton: %s", name);
	}

	private Type[] typeApplyArguments(TypedTree args) {
		Type[] types = new Type[args.size()];
		for (int i = 0; i < args.size(); i++) {
			types[i] = type(args.get(i));
		}
		return types;
	}

	UList<Method> bufferMethods = new UList<Method>(new Method[128]);

	private String methods(UList<Method> bufferMethods, int start) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < bufferMethods.size(); i++) {
			sb.append(" ");
			sb.append(bufferMethods.ArrayValues[i]);
		}
		bufferMethods.clear(start);
		return sb.toString();
	}

	private Type resolvedMethod(TypedTree node, boolean isStaticNormalMethod, Method m, int start, String fmt, Object... args) {
		if (m != null) {
			node.setMethod(true, m);
			return m.getReturnType();
		}
		String msg = String.format(fmt, args);
		if (this.bufferMethods.size() > start) {
			msg = "mismatched " + msg + methods(bufferMethods, start);
		} else {
			msg = "undefined " + msg;
		}
		throw new TypeCheckerException(this.typeSystem, node, msg);
	}

	public Type typeMethodApply(TypedTree node) {
		if (isStaticMethodApply(node)) {
			return this.typeStaticMethodApply(node);
		}
		Class<?> c = TypeSystem.toClass(type(node.get(_recv)));
		String name = node.getText(_name, "");
		TypedTree args = node.get(_param);
		Type[] types = this.typeApplyArguments(args);
		int start = this.bufferMethods.size();
		Method m = this.typeSystem.resolveObjectMethod(c, name, types, bufferMethods, args);
		return this.resolvedMethod(node, false, m, start, "method %s of %s", name, typeSystem.name(c));
	}

	private boolean isStaticMethodApply(TypedTree node) {
		if (node.get(_recv).is(_Name)) {
			Type t = this.typeSystem.resolveType(node.get(_recv), null);
			return t != null;
		}
		return false;
	}

	public Type typeStaticMethodApply(TypedTree node) {
		Class<?> c = TypeSystem.toClass(this.typeSystem.resolveType(node.get(_recv), null));
		String name = node.getText(_name, "");
		TypedTree args = node.get(_param);
		Type[] types = this.typeApplyArguments(args);
		int start = this.bufferMethods.size();
		Method m = this.typeSystem.resolveStaticMethod(c, name, types, bufferMethods, args);
		return this.resolvedMethod(node, true, m, start, "static method %s of %s", name, typeSystem.name(c));
	}

	private Type typeUnary(TypedTree node, String name) {
		Type left = type(node.get(_expr));
		Type common = typeSystem.PrimitiveType(left);
		if (left != common) {
			left = this.tryPrecast(common, node, _expr);
		}
		Type[] types = new Type[] { left };
		int start = this.bufferMethods.size();
		Method m = this.typeSystem.resolveFunctionMethod(name, types, bufferMethods, node);
		return this.resolvedMethod(node, true, m, start, "operator %s", OperatorNames.name(name));
	}

	private Type typeBinary(TypedTree node, String name, BinaryTypeUnifier unifier) {
		Type left = type(node.get(_left));
		Type right = type(node.get(_right));
		Type common = unifier.unify(typeSystem.PrimitiveType(left), typeSystem.PrimitiveType(right));
		if (left != common) {
			left = this.tryPrecast(common, node, _left);
		}
		if (right != common) {
			right = this.tryPrecast(common, node, _right);
		}

		Type[] types = new Type[] { left, right };
		int start = this.bufferMethods.size();
		Method m = this.typeSystem.resolveFunctionMethod(name, types, bufferMethods, node);
		return this.resolvedMethod(node, true, m, start, "operator %s", OperatorNames.name(name));
	}

	private Type tryPrecast(Type req, TypedTree node, Symbol label) {
		TypedTree unode = node.get(label);
		TypedTree cnode = this.typeSystem.makeCast(req, unode);
		if (unode == cnode) {
			return node.getType();
		}
		node.set(label, cnode);
		return req;
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
		return this.typeUnary(node, "opPlus");
	}

	public Type typeMinus(TypedTree node) {
		return this.typeUnary(node, "opMinus");
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
		return this.typeUnary(node, "opCompl");
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
