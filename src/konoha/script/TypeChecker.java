package konoha.script;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import konoha.Function;
import konoha.asm.FunctorFactory;
import konoha.message.Message;
import konoha.script.TypeSystem.BinaryTypeUnifier;
import nez.ast.Symbol;
import nez.ast.TreeVisitor2;
import nez.util.StringUtils;
import nez.util.UList;

public class TypeChecker extends TreeVisitor2<konoha.script.TypeChecker.Undefined> implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;

	// TypeScope scope;

	public TypeChecker(ScriptContext context, TypeSystem typeSystem) {
		// super(TypedTree.class);
		this.context = context;
		this.typeSystem = typeSystem;
		init(new Undefined());
	}

	public class Undefined {
		public Type accept(TypedTree node) {
			node.formatSourceMessage("error", "unsupproted type rule " + node);
			typeSystem.TODO("TypeChecker for %s", node);
			return void.class;
		}
	}

	public class Error {
		public Type type(TypedTree t) {
			context.log(t.getText(_msg, ""));
			return void.class;
		}
	}

	FunctionBuilder function = null;

	public final FunctionBuilder enterFunction(String name) {
		this.function = new FunctionBuilder(this.function, name);
		return this.function;
	}

	public final void exitFunction() {
		this.function = this.function.pop();
	}

	public final boolean inFunction() {
		return this.function != null;
	}

	public Type visit(TypedTree node) {
		Type c = node.getType();
		if (c == null) {
			c = find(node).accept(node);
			if (c != null) {
				node.setType(c);
			}
		}
		return c;
	}

	private String name(Type t) {
		return TypeSystem.name(t);
	}

	public void typed(TypedTree node, Type c) {
		node.setType(c);
	}

	/* TopLevel */
	public class Source extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			Type t = null;
			for (int i = 0; i < node.size(); i++) {
				TypedTree sub = node.get(i);
				try {
					t = visit(sub);
				} catch (TypeCheckerException e) {
					sub = e.errorTree;
					node.set(i, sub);
					t = sub.getType();
				}
			}
			return t;
		}
	}

	public class Import extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeImport(node);
		}
	}

	public Type typeImport(TypedTree node) {
		String path = join(node.get(_name));
		try {
			typeSystem.importStaticClass(path);
		} catch (ClassNotFoundException e) {
			throw error(node.get(_name), "undefined class name: %s", path);
		}
		node.done();
		return void.class;
	}

	private String join(TypedTree node) {
		if (node.size() == 0) {
			return node.toText();
		}
		StringBuilder sb = new StringBuilder();
		join(sb, node);
		return sb.toString();
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
	private static Type[] EmptyTypes = new Type[0];

	public class FuncDecl extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeFuncDecl(node);
		}
	}

	public Type typeFuncDecl(TypedTree node) {
		String name = node.getText(_name, null);
		TypedTree bodyNode = node.get(_body, null);
		Type returnType = resolveType(node.get(_type, null), null);
		Type[] paramTypes = EmptyTypes;
		TypedTree params = node.get(_param, null);
		if (node.has(_param)) {
			int c = 0;
			paramTypes = new Type[params.size()];
			for (TypedTree p : params) {
				paramTypes[c] = resolveType(p.get(_type, null), Object.class);
				c++;
			}
		}
		/* prototye declration */
		if (bodyNode == null) {
			Class<?> funcType = this.typeSystem.getFuncType(returnType, paramTypes);
			if (typeSystem.hasGlobalVariable(name)) {
				throw error(node.get(_name), "duplicated name: %s", name);
			}
			typeSystem.newGlobalVariable(funcType, name);
			node.done();
			return void.class;
		}
		FunctionBuilder f = this.enterFunction(name);
		if (returnType != null) {
			f.setReturnType(returnType);
			typed(node.get(_type), returnType);
		}
		if (node.has(_param)) {
			int c = 0;
			for (TypedTree sub : params) {
				String pname = sub.getText(_name, null);
				f.setVarType(pname, paramTypes[c]);
				typed(sub, paramTypes[c]);
			}
		}
		f.setParameterTypes(paramTypes);
		try {
			visit(bodyNode);
		} catch (TypeCheckerException e) {
			node.set(_body, e.errorTree);
		}
		this.exitFunction();
		if (f.getReturnType() == null) {
			f.setReturnType(void.class);
		}
		typed(node.get(_name), f.getReturnType());
		return void.class;
	}

	public class Return extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeReturn(node);
		}
	}

	public Type typeReturn(TypedTree node) {
		if (!inFunction()) {
			throw this.error(node, "return must be inside function");
		}
		Type t = this.function.getReturnType();
		if (t == null) { // type inference
			if (node.has(_expr)) {
				this.function.setReturnType(visit(node.get(_expr)));
			} else {
				this.function.setReturnType(void.class);
			}
			return void.class;
		}
		if (t == void.class) {
			if (node.size() > 0) {
				node.removeSubtree();
			}
		} else {
			this.enforceType(t, node, _expr);
		}
		return void.class;
	}

	/* Statement */

	public class Block extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			if (inFunction()) {
				function.beginLocalVarScope();
			}
			typeStatementList(node);
			if (inFunction()) {
				function.endLocalVarScope();
			}
			return void.class;
		}
	}

	public class StatementList extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeStatementList(node);
		}
	}

	public Type typeStatementList(TypedTree node) {
		for (int i = 0; i < node.size(); i++) {
			TypedTree sub = node.get(i);
			try {
				visit(sub);
			} catch (TypeCheckerException e) {
				sub = e.errorTree;
				node.set(i, sub);
			}
		}
		return void.class;
	}

	public class Assert extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			enforceType(boolean.class, node, _cond);
			if (node.has(_msg)) {
				enforceType(String.class, node, _msg);
			} else {
				String msg = node.get(_cond).formatSourceMessage("assert", "failed");
				node.make(_cond, node.get(_cond), _msg, node.newStringConst(msg));
			}
			node.setInterface(Hint.StaticInvocation2, KonohaRuntime.System_assert(), null);
			return void.class;
		}
	}

	public class If extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			enforceType(boolean.class, node, _cond);
			visit(node.get(_then));
			if (node.has(_else)) {
				visit(node.get(_else));
			}
			return void.class;
		}
	}

	public class Conditional extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			enforceType(boolean.class, node, _cond);
			Type then_t = visit(node.get(_then));
			Type else_t = visit(node.get(_else));
			if (then_t != else_t) {
				enforceType(then_t, node, _else);
			}
			return then_t;
		}
	}

	public class While extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			enforceType(boolean.class, node, _cond);
			visit(node.get(_body));
			return void.class;
		}
	}

	public class Continue extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return void.class;
		}
	}

	public class Break extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return void.class;
		}
	}

	public class For extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			if (inFunction()) {
				function.beginLocalVarScope();
			}
			if (node.has(_init)) {
				visit(node.get(_init));
			}
			if (node.has(_cond)) {
				enforceType(boolean.class, node, _cond);
			}
			if (node.has(_iter)) {
				visit(node.get(_iter));
			}
			visit(node.get(_body));
			if (inFunction()) {
				function.endLocalVarScope();
			}
			return void.class;
		}
	}

	// public class ForEach extends Undefined {
	// @Override
	// public Type accept(TypedTree node) {
	// return typeForEach(node);
	// }
	// }
	//
	// public Type typeForEach(TypedTree node) {
	// Type req_t = null;
	// if (node.has(_type)) {
	// req_t = this.typeSystem.resolveType(node.get(_type), null);
	// }
	// String name = node.getText(_name, "");
	// req_t = typeIterator(req_t, node.get(_iter));
	// if (inFunction()) {
	// this.function.beginLocalVarScope();
	// }
	// this.function.setVarType(name, req_t);
	// visit(node.get(_body));
	// if (inFunction()) {
	// this.function.endLocalVarScope();
	// }
	// return void.class;
	// }

	protected Type[] EmptyArgument = new Type[0];

	// private Type typeIterator(Type req_t, TypedTree node) {
	// Type iter_t = visit(node.get(_iter));
	// Method m = typeSystem.resolveObjectMethod(req_t, this.bufferMatcher,
	// "iterator", EmptyArgument, null, null);
	// if (m != null) {
	// TypedTree iter = node.newInstance(_MethodApply, 0, null);
	// iter.make(_recv, node.get(_iter), _param, node.newInstance(_List, 0,
	// null));
	// iter_t = iter.setMethod(Hint.MethodApply, m, this.bufferMatcher);
	// // TODO
	// // if(req_t != null) {
	// // }
	// // node.set(index, node)
	// }
	// throw error(node.get(_iter), "unsupported iterator for %s",
	// name(iter_t));
	// }

	public class VarDecl extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeVarDecl(node);
		}
	}

	public Type typeVarDecl(TypedTree node) {
		boolean isArrayName = false;
		String name = node.getText(_name, null);
		if (node.get(_name).is(_ArrayName)) {
			name = node.get(_name).getText(_name, null);
			isArrayName = true;
		}
		Type type = resolveType(node.get(_type, null), null);
		if (type != null) {
			if (isArrayName) {
				type = typeSystem.newArrayType(type);
			}
			if (node.has(_expr)) {
				enforceType(type, node, _expr);
			}
		} else { /* type inference from the expression */
			if (!node.has(_expr)) { // untyped
				this.typeSystem.reportWarning(node.get(_name), "type is ungiven");
				type = Object.class;
			} else {
				type = visit(node.get(_expr, null));
			}
		}
		defineVariable(node, type, name);
		return void.class;
		// typed(node.get(_name), type); // name is typed
		//
		// if (this.inFunction()) {
		// // TRACE("local variable");
		// this.function.setVarType(name, type);
		// return void.class;
		// }
		// // TRACE("global variable");
		// GlobalVariable gv = typeSystem.getGlobalVariable(name);
		// if (gv != null) {
		// if (gv.getType() != type) {
		// throw error(node.get(_name), "already defined name: %s as %s", name,
		// name(gv.getType()));
		// }
		// } else {
		// gv = typeSystem.newGlobalVariable(type, name);
		// }
		// if (!node.has(_expr)) {
		// node.done();
		// return void.class;
		// }
		// // Assign
		// node.rename(_VarDecl, _Assign);
		// return node.setField(Hint.SetField, gv.field);
	}

	public class MultiVarDecl extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			Type type = resolveType(node.get(_type), null);
			for (TypedTree sub : node.get(_list)) {
				typeVarDecl(type, sub);
			}
			return void.class;
		}
	}

	public void typeVarDecl(Type type, TypedTree node) {
		String name = node.getText(_name, null);
		if (node.get(_name).is(_ArrayName)) {
			name = node.get(_name).getText(_name, null);
			type = typeSystem.newArrayType(type);
		}
		if (node.has(_expr)) {
			enforceType(type, node, _expr);
		}
		defineVariable(node, type, name);
	}

	private void defineVariable(TypedTree node, Type type, String name) {
		typed(node.get(_name), type); // name is typed
		if (this.inFunction()) {
			// TRACE("local variable");
			this.function.setVarType(name, type);
			typed(node, void.class);
			return;
		}
		// TRACE("global variable");
		GlobalVariable gv = typeSystem.getGlobalVariable(name);
		if (gv != null) {
			if (gv.getType() != type) {
				throw error(node.get(_name), "already defined name: %s as %s", name, name(gv.getType()));
			}
		} else {
			gv = typeSystem.newGlobalVariable(type, name);
		}
		node.setField(Hint.SetField, gv.field);
		typed(node, void.class);
	}

	/* StatementExpression */

	public class Expression extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return visit(node.get(0));
		}
	}

	/* Expression */

	public class Name extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			Type t = tryCheckNameType(node, true);
			if (t == null) {
				String name = node.toText();
				throw error(node, Message.UndefinedName_, name);
			}
			return t;
		}
	}

	private Type tryCheckNameType(TypedTree node, boolean rewrite) {
		String name = node.toText();
		if (this.inFunction()) {
			if (this.function.containsVariable(name)) {
				return this.function.getVarType(name);
			}
		}
		if (this.typeSystem.hasGlobalVariable(name)) {
			GlobalVariable gv = this.typeSystem.getGlobalVariable(name);
			if (rewrite) {
				node.setField(Hint.GetField, gv.field);
			}
			return gv.getType();
		}
		return null;
	}

	public class Assign extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeAssign(node);
		}
	}

	public void checkAssignable(TypedTree node) {
		if (node.is(_Name) || node.is(_Field) || node.is(_Indexer)) {
			return;
		}
		throw error(node, Message.LeftHandAssignment);
	}

	public Type typeAssign(TypedTree node) {
		TypedTree leftnode = node.get(_left);
		checkAssignable(leftnode);
		if (typeSystem.shellMode && !this.inFunction() && leftnode.is(_Name)) {
			String name = node.getText(_left, null);
			if (!this.typeSystem.hasGlobalVariable(name)) {
				this.typeSystem.newGlobalVariable(Object.class, name);
			}
		}
		if (leftnode.is(_Indexer)) {
			return typeSetIndexer(node, //
					node.get(_left).get(_recv), //
					node.get(_left).get(_param), //
					node.get(_right));
		}
		Type left = visit(leftnode);
		this.enforceType(left, node, _right);
		if (leftnode.hint == Hint.GetField) {
			Field f = leftnode.getField();
			if (Modifier.isFinal(f.getModifiers())) {
				throw error(node.get(_left), Message.ReadOnly);
			}
			if (!Modifier.isStatic(f.getModifiers())) {
				node.set(_left, leftnode.get(_recv));
				node.rename(_left, _recv);
			}
			node.rename(_right, _expr);
			node.setField(Hint.SetField, f);
		}
		return left;
	}

	/* Expression */

	public class Cast extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeCast(node);
		}
	}

	public Type typeCast(TypedTree node) {
		Type inner = visit(node.get(_expr));
		Type t = this.resolveType(node.get(_type), null);
		if (t == null) {
			throw error(node.get(_type), Message.UndefinedType_, node.getText(_type, ""));
		}
		Class<?> req = TypeSystem.toClass(t);
		Class<?> exp = TypeSystem.toClass(inner);
		Method m = typeSystem.getCastMethod(exp, req);
		if (m == null) {
			m = typeSystem.getConvertMethod(exp, req);
		}
		if (m != null) {
			node.makeFlattenedList(node.get(_expr));
			return node.setInterface(Hint.StaticInvocation2, FunctorFactory.newMethod(m));
		}
		if (req.isAssignableFrom(exp)) { // upcast
			node.setTag(_UpCast);
			return t;
		}
		if (exp.isAssignableFrom(req)) { // downcast
			node.setTag(_DownCast);
			return t;
		}
		throw error(node.get(_type), Message.UndefinedCast__, name(inner), name(t));
	}

	// public Type[] typeList(TypedTree node) {
	// Type[] args = new Type[node.size()];
	// for (int i = 0; i < node.size(); i++) {
	// args[i] = type(node.get(i));
	// }
	// return args;
	// }

	public class _Field extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeField(node);
		}
	}

	private Type typeField(TypedTree node) {
		if (isStaticClassName(node)) {
			return typeStaticField(node);
		}
		visit(node.get(_recv));
		Class<?> recvClass = node.get(_recv).getClassType();
		String name = node.getText(_name, "");
		Field f = typeSystem.getField(recvClass, name);
		if (f != null) {
			return node.setField(Hint.GetField, f);
		}
		if (typeSystem.isDynamic(recvClass)) {
			return node.setInterface(Hint.StaticInvocation2, KonohaRuntime.Object_getField(), null);
		}
		throw error(node.get(_name), Message.UndefinedField__, name(recvClass), name);
	}

	private Type typeStaticField(TypedTree node) {
		Class<?> recvClass = this.resolveClass(node.get(_recv), null);
		String name = node.getText(_name, "");
		java.lang.reflect.Field f = typeSystem.getField(recvClass, name);
		if (f != null) {
			if (!Modifier.isStatic(f.getModifiers())) {
				throw error(node, Message.UndefinedField__, name(recvClass), name);
			}
			return node.setField(Hint.GetField, f);
		}
		throw error(node.get(_name), Message.UndefinedField__, name(recvClass), name);
	}

	public class Apply extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeApply(node);
		}
	}

	public Type typeApply(TypedTree node) {
		String name = node.getText(_name, "");
		typeArguments(node.get(_param));
		if (isRecursiveCall(node, name)) {
			return node.getType();
		}
		// Type func_t = this.tryCheckNameType(node.get(_name), true);
		// if (this.typeSystem.isFuncType(func_t)) {
		// return typeFuncApply(node, func_t, node.get(_param));
		// }
		TypeMatcher matcher = this.initTypeMatcher(null);
		Functor inf = this.resolveFunction(matcher, name, node.get(_param));
		if (inf == null) {
			return undefinedMethod(node, matcher, Message.Function_, name);
		}
		return node.setInterface(Hint.StaticApplyInterface, inf, matcher);
	}

	private void typeArguments(TypedTree params) {
		for (int i = 0; i < params.size(); i++) {
			visit(params.get(i));
		}
	}

	// private Type[] typeApplyArguments(TypedTree args) {
	// Type[] types = new Type[args.size()];
	// for (int i = 0; i < args.size(); i++) {
	// types[i] = visit(args.get(i));
	// }
	// return types;
	// }

	private boolean isRecursiveCall(TypedTree node, String name) {
		if (inFunction() && name.equals(function.getName())) {
			Type[] paramTypes = function.getParameterTypes();
			if (accept(paramTypes, node.get(_param))) {
				Type returnType = function.getReturnType();
				if (returnType == null) {
					throw error(node, Message.UndefinedReturnType_, name);
				}
				node.setHint(Hint.RecursiveApply, returnType);
				return true;
			}
		}
		return false;
	}

	private Type typeFuncApply(TypedTree node, Type func_t, Type[] params_t, TypedTree params) {
		if (typeSystem.isStaticFuncType(func_t)) {
			Class<?>[] p = typeSystem.getFuncParameterTypes(func_t);
			if (accept(p, params)) {
				node.rename(_name, _recv);
				return node.setInterface(Hint.MethodApply2, FunctorFactory.newMethod(Reflector.findInvokeMethod((Class<?>) func_t)));
			}
			throw error(node, "mismatched %s", Reflector.findInvokeMethod((Class<?>) func_t));
		} else {
			for (int i = 0; i < params.size(); i++) {
				params.set(i, enforceType(Object.class, params.get(i)));
			}
			// node.makeFlattenedList(node.get(_name), params);
			return node.setInterface(Hint.StaticInvocation2, KonohaRuntime.Object_invokeFunction());
		}
	}

	private Type typeUnary(TypedTree node, String name) {
		Type left = visit(node.get(_expr));
		Type common = typeSystem.PrimitiveType(left);
		if (left != common) {
			left = this.tryCastBeforeMatching(common, node, _expr);
		}
		TypeMatcher matcher = initTypeMatcher(null);
		Functor inf = this.resolveFunction(matcher, name, node);
		if (inf == null) {
			return this.undefinedMethod(node, matcher, Message.Unary__, OperatorNames.name(name), name(left));
		}
		return node.setInterface(Hint.StaticUnaryInterface, inf, matcher);
	}

	private Type typeBinary(TypedTree node, String name, BinaryTypeUnifier unifier) {
		Type left = visit(node.get(_left));
		Type right = visit(node.get(_right));
		Type common = unifier.unify(typeSystem.PrimitiveType(left), typeSystem.PrimitiveType(right));
		if (left != common) {
			left = this.tryCastBeforeMatching(common, node, _left);
		}
		if (right != common) {
			right = this.tryCastBeforeMatching(common, node, _right);
		}
		TypeMatcher matcher = initTypeMatcher(null);
		Functor inf = this.resolveFunction(matcher, name, node);
		if (inf == null) {
			return this.undefinedMethod(node, matcher, Message.Binary___, name(left), OperatorNames.name(name), name(right));
		}
		return node.setInterface(Hint.StaticBinaryInterface, inf, matcher);
	}

	private Type tryCastBeforeMatching(Type req, TypedTree node, Symbol label) {
		TypedTree child = node.get(label);
		Method m = typeSystem.getCastMethod(child.getType(), req);
		if (m != null) {
			TypedTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, child);
			newnode.setInterface(Hint.StaticInvocation2, FunctorFactory.newMethod(m));
			newnode.setType(req);
			node.set(label, newnode);
			return req;
		}
		return node.getType();
	}

	public class And extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			enforceType(boolean.class, node, _left);
			enforceType(boolean.class, node, _right);
			return boolean.class;
		}
	}

	public class Or extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			enforceType(boolean.class, node, _left);
			enforceType(boolean.class, node, _right);
			return boolean.class;
		}
	}

	public class Not extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			enforceType(boolean.class, node, _expr);
			return typeUnary(node, "opNot");
		}
	}

	public class TypeOf extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			try {
				Type t = visit(node.get(_expr));
				node.setConst(String.class, name(t));
			} catch (TypeCheckerException e) {
				context.log(e.getMessage());
				node.setConst(String.class, null);
			}
			return String.class;
		}
	}

	public class Instanceof extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			Class<?> c = TypeSystem.toClass(visit(node.get(_left)));
			Class<?> t = resolveClass(node.get(_right), null);
			if (!t.isAssignableFrom(c)) {
				typeSystem.reportWarning(node, "incompatible instanceof operation: %s", name(t));
				node.setConst(boolean.class, false);
			}
			node.setValue(t);
			return boolean.class;
		}
	}

	public class Add extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opAdd", TypeSystem.UnifyAdditive);
		}
	}

	public class Sub extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opSub", TypeSystem.UnifyAdditive);
		}
	}

	public class Mul extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opMul", TypeSystem.UnifyAdditive);
		}
	}

	public class Div extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opDiv", TypeSystem.UnifyAdditive);
		}
	}

	public class Mod extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opMod", TypeSystem.UnifyAdditive);
		}
	}

	public class Plus extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeUnary(node, "opPlus");
		}
	}

	public class Minus extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeUnary(node, "opMinus");
		}
	}

	public class Equals extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opEquals", TypeSystem.UnifyEquator);
		}
	}

	public class NotEquals extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opNotEquals", TypeSystem.UnifyEquator);
		}
	}

	public class LessThan extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opLessThan", TypeSystem.UnifyComparator);
		}
	}

	public class LessThanEquals extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opLessThanEquals", TypeSystem.UnifyComparator);
		}
	}

	public class GreaterThan extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opGreaterThan", TypeSystem.UnifyComparator);
		}
	}

	public class GreaterThanEquals extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opGreaterThanEquals", TypeSystem.UnifyComparator);
		}
	}

	public class LeftShift extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opLeftShift", TypeSystem.UnifyBitwise);
		}
	}

	public class RightShift extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opRightShift", TypeSystem.UnifyBitwise);
		}
	}

	public class LogicalRightShift extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opLogicalRightShift", TypeSystem.UnifyBitwise);
		}
	}

	public class BitwiseAnd extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opBitwiseAnd", TypeSystem.UnifyBitwise);
		}
	}

	public class BitwiseOr extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opBitwiseOr", TypeSystem.UnifyBitwise);
		}
	}

	public class BitwiseXor extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeBinary(node, "opBitwiseXor", TypeSystem.UnifyBitwise);
		}
	}

	public class Compl extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeUnary(node, "opCompl");
		}
	}

	public class Null extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return Object.class;
		}
	}

	public class True extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return node.setConst(boolean.class, true);
		}
	}

	public class False extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return node.setConst(boolean.class, false);
		}
	}

	public class _Integer extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			try {
				String n = node.toText().replace("_", "");
				if (n.startsWith("0b") || n.startsWith("0B")) {
					return node.setConst(int.class, Integer.parseInt(n.substring(2), 2));
				} else if (n.startsWith("0x") || n.startsWith("0X")) {
					return node.setConst(int.class, Integer.parseInt(n.substring(2), 16));
				}
				return node.setConst(int.class, Integer.parseInt(n));
			} catch (NumberFormatException e) {
				typeSystem.reportWarning(node, e.getMessage());
			}
			return node.setConst(int.class, 0);
		}
	}

	public class _Long extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			try {
				String n = node.toText();
				return node.setConst(long.class, Long.parseLong(n));
			} catch (NumberFormatException e) {
				typeSystem.reportWarning(node, e.getMessage());
			}
			return node.setConst(long.class, 0L);
		}
	}

	public class _Float extends _Double {
	}

	public class _Double extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			try {
				String n = node.toText();
				return node.setConst(double.class, Double.parseDouble(n));
			} catch (NumberFormatException e) {
				typeSystem.reportWarning(node, e.getMessage());
			}
			return node.setConst(double.class, 0.0);
		}
	}

	public class Text extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return node.setConst(String.class, node.toText());
		}
	}

	public class _String extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			String t = node.toText();
			return node.setConst(String.class, StringUtils.unquoteString(t));
		}
	}

	public class Character extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			String t = StringUtils.unquoteString(node.toText());
			if (t.length() == 1) {
				return node.setConst(char.class, t.charAt(0));
			}
			return node.setConst(String.class, t);
		}
	}

	public class Interpolation extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			for (int i = 0; i < node.size(); i++) {
				TypedTree sub = node.get(i);
				visit(sub);
				if (sub.getType() != Object.class) {
					node.set(i, enforceType(Object.class, sub));
				}
			}
			return node.setInterface(Hint.Unique, KonohaRuntime.String_join());
		}
	}

	public class New extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			Type newType = resolveType(node.get(_type), null);
			typeArguments(node.get(_param));
			TypeMatcher matcher = initTypeMatcher(newType);
			Functor inf = resolveConstructor(matcher, newType, node.get(_param));
			if (inf != null) {
				return node.setInterface(Hint.ConstructorInterface, inf, matcher);
			}
			return undefinedMethod(node, matcher, Message.Constructor_, name(newType));
		}
	}

	public class MethodApply extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			if (isStaticClassName(node)) {
				return typeStaticMethodApply(node);
			}
			Type recvType = visit(node.get(_recv));
			typeArguments(node.get(_param));
			String name = node.getText(_name, "");
			TypeMatcher matcher = initTypeMatcher(recvType);
			Functor inf = resolveObjectMethod(matcher, recvType, name, node.get(_param));
			if (inf != null) {
				return node.setInterface(Hint.MethodApply2, inf, matcher);
			}
			// if (typeSystem.isDynamic(recvType)) {
			// m = Reflector.getInvokeDynamicMethod(node.get(_param).size());
			// if (m != null) {
			// node.makeFlattenedList(node.get(_recv),
			// node.newStringConst(name), node.get(_param));
			// return node.setMethod(Hint.StaticDynamicInvocation, m, null);
			// }
			// }
			return undefinedMethod(node, matcher, Message.Method__, name(recvType), name);
		}
	}

	private boolean isStaticClassName(TypedTree node) {
		if (node.get(_recv).is(_Name)) {
			Type t = this.typeSystem.getType(node.get(_recv).toText());
			return t != null;
		}
		return false;
	}

	private Type typeStaticMethodApply(TypedTree node) {
		Class<?> staticClass = this.resolveClass(node.get(_recv), null);
		String name = node.getText(_name, "");
		this.typeArguments(node.get(_param));
		TypeMatcher matcher = initTypeMatcher(null);
		Functor inf = resolveStaticMethod(matcher, staticClass, name, node.get(_param));
		if (inf == null) {
			return this.undefinedMethod(node, matcher, Message.Method__, name, name(staticClass));
		}
		return node.setInterface(Hint.StaticApplyInterface, inf, matcher);
	}

	public Type typeIndexer(TypedTree node) {
		Type recvType = visit(node.get(_recv));
		typeArguments(node.get(_param));
		TypeMatcher matcher = initTypeMatcher(recvType);
		Functor inf = resolveObjectMethod(matcher, recvType, "get", node.get(_param));
		if (inf != null) {
			return node.setInterface(Hint.MethodApply2, inf, matcher);
		}
		if (typeSystem.isDynamic(recvType)) {
			TODO("Dynamic Indexer");
			// node.makeFlattenedList(node.get(_recv), node.get(_param));
			// return node.setMethod(Hint.StaticInvocation,
			// typeSystem.ObjectIndexer, null);
		}
		return this.undefinedMethod(node, matcher, Message.Indexer_, name(recvType));
	}

	private Type typeSetIndexer(TypedTree node, TypedTree recv, TypedTree param, TypedTree expr) {
		param.makeFlattenedList(param, expr);
		node.make(_recv, recv, _param, param);

		Type recvType = visit(node.get(_recv));
		typeArguments(node.get(_param));
		TypeMatcher matcher = initTypeMatcher(recvType);
		Functor inf = resolveObjectMethod(matcher, recvType, "get", node.get(_param));
		if (inf != null) {
			return node.setInterface(Hint.MethodApply2, inf, matcher);
		}
		if (typeSystem.isDynamic(recvType)) {
			TODO("Dynamic Indexer");
			// node.makeFlattenedList(node.get(_recv), node.get(_param));
			// return node.setMethod(Hint.StaticInvocation,
			// typeSystem.ObjectSetIndexer, null);
		}
		return this.undefinedMethod(node, matcher, Message.Indexer_, name(recvType));
	}

	/* array */

	private Type typeCollectionElement(TypedTree node, int step) {
		if (node.size() == 0) {
			return Object.class;
		}
		boolean mixed = false;
		Type elementType = Object.class;
		int shift = step == 2 ? 1 : 0;
		for (int i = 0; i < node.size(); i += step) {
			TypedTree sub = node.get(i + shift);
			Type t = visit(sub);
			if (t == elementType) {
				continue;
			}
			if (elementType == null) {
				elementType = t;
			} else {
				mixed = true;
				elementType = Object.class;
			}
		}
		if (mixed) {
			for (int i = 0; i < node.size(); i += step) {
				TypedTree sub = node.get(i + shift);
				if (sub.getType() != Object.class) {
					node.set(i, enforceType(Object.class, sub));
				}
			}
		}
		return elementType;
	}

	public class Array extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			Type elementType = typeCollectionElement(node, 1);
			Type arrayType = typeSystem.newArrayType(elementType);
			return arrayType;
		}
	}

	public class Set extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			Type elementType = typeCollectionElement(node, 1);
			Type arrayType = typeSystem.newArrayType(elementType);
			return arrayType;
		}
	}

	public class Dict extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			Type elementType = typeCollectionElement(node, 1);
			Type arrayType = typeSystem.newArrayType(elementType);
			return arrayType;
		}
	}

	// Syntax Sugar

	private Type typeSelfAssignment(TypedTree node, Symbol optag) {
		TypedTree op = node.newInstance(optag, 0, null);
		op.make(_left, node.get(_left).dup(), _right, node.get(_right));
		node.set(_right, op);
		node.setTag(_Assign);
		return typeAssign(node);
	}

	public class AssignAdd extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _Add);
		}
	}

	public class AssignSub extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _Sub);
		}
	}

	public class AssignMul extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _Mul);
		}
	}

	public class AssignDiv extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _Div);
		}
	}

	public class AssignMod extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _Mod);
		}
	}

	public class AssignLeftShift extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _LeftShift);
		}
	}

	public class AssignRightShift extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _RightShift);
		}
	}

	public class AssignLogicalRightShift extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _LogicalRightShift);
		}
	}

	public class AssignBitwiseAnd extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _BitwiseAnd);
		}
	}

	public class AssignBitwiseXOr extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _BitwiseXor);
		}
	}

	public class AssignBitwiseOr extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			return typeSelfAssignment(node, _BitwiseOr);
		}
	}

	private TypedTree desugarInc(TypedTree expr, Symbol optag) {
		TypedTree op = expr.newInstance(optag, 0, null);
		op.make(_left, expr.dup(), _right, expr.newIntConst(1));
		return op;
	}

	private TypedTree desugarAssign(TypedTree node, TypedTree expr, Symbol optag) {
		TypedTree op = expr.newInstance(optag, 0, null);
		op.make(_left, expr.dup(), _right, expr.newIntConst(1));
		node.make(_left, expr, _right, desugarInc(expr, _Add));
		node.setTag(_Assign);
		return node;
	}

	public class PreInc extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			TypedTree expr = node.get(_expr);
			return visit(desugarAssign(node, expr, _Add));
		}
	}

	public class PreDec extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			TypedTree expr = node.get(_expr);
			return visit(desugarAssign(node, expr, _Sub));
		}
	}

	public class Inc extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			TypedTree expr = node.get(_recv);
			TypedTree assign = desugarAssign(node.newInstance(_Assign, 2, null), expr.dup(), _Add);
			node.make(_expr, expr, _body, assign);
			Type t = visit(expr);
			visit(assign);
			return t;
		}
	}

	public class Dec extends Undefined {
		@Override
		public Type accept(TypedTree node) {
			TypedTree expr = node.get(_recv);
			TypedTree assign = desugarAssign(node.newInstance(_Assign, 2, null), expr.dup(), _Sub);
			node.make(_expr, expr, _body, assign);
			Type t = visit(expr);
			visit(assign);
			return t;
		}
	}

	// new interface
	private TypeMatcher defaultMatcher = new TypeMatcher(this.typeSystem, this);

	private TypeMatcher initTypeMatcher(Type recvType) {
		defaultMatcher.init(recvType);
		return defaultMatcher;
	}

	private Type undefinedMethod(TypedTree node, TypeMatcher matcher, Message fmt, Object... args) {
		String methods = matcher.getErrorMessage();
		String msg = String.format(fmt.toString(), args);
		if (methods == null) {
			msg = String.format(Message.UndefinedFunctor_.toString(), msg);
		} else {
			msg = String.format(Message.MismatchedFunctor__.toString(), msg, methods);
		}
		throw error(node, msg);
	}

	// resolve

	public final Type resolveType(TypedTree node, Type deftype) {
		if (node == null) {
			return deftype;
		}
		if (node.size() == 0) {
			Type t = this.typeSystem.getType(node.toText());
			if (t == null) {
				throw this.error(node, Message.UndefinedType_, node.toText());
			}
			return t == null ? deftype : t;
		}
		if (node.is(_TypeOf)) {
			try {
				return visit(node.get(_expr));
			} catch (TypeCheckerException e) {
				context.log(e.getMessage());
			}
			return Object.class;
		}
		if (node.is(_ArrayType)) {
			return GenericType.newType(konoha.Array.class, resolveType(node.get(_base), Object.class));
		}
		if (node.is(_GenericType)) {
			Class<?> base = this.resolveClass(node.get(_base), null);
			if (base == null) {
				return deftype;
			}
			TypedTree params = node.get(_param);
			if (base == Function.class) {
				Class<?>[] p = new Class<?>[params.size() - 1];
				for (int i = 0; i < p.length; i++) {
					p[0] = resolveClass(params.get(i + 1), Object.class);
				}
				return this.typeSystem.getFuncType(resolveClass(params.get(0), void.class), p);
			} else {
				int paramSize = base.getTypeParameters().length;
				if (node.get(_param).size() != paramSize) {
					throw this.error(node, "mismatched parameter number %s", base);
				}
				Type[] p = new Type[paramSize];
				int c = 0;
				for (TypedTree sub : node.get(_param)) {
					p[c] = resolveType(sub, Object.class);
					c++;
				}
				return GenericType.newType(base, p);
			}
		}
		return deftype;
	}

	public final Class<?> resolveClass(TypedTree node, Class<?> deftype) {
		Type t = this.resolveType(node, deftype);
		if (t == null) {
			return deftype;
		}
		return TypeSystem.toClass(t);
	}

	public final Functor resolveFunction(TypeMatcher matcher, String name, TypedTree params) {
		UList<Object> symbolList = typeSystem.getSymbolList();
		for (int i = symbolList.size() - 1; i >= 0; i--) {
			Object symbol = symbolList.ArrayValues[i];
			if (symbol instanceof Method) {
				Method m = (Method) symbol;
				if (name.equals(m.getName())) {
					Type[] p = m.getGenericParameterTypes();
					if (Functor.match(matcher, p, params)) {
						return FunctorFactory.newMethod(m);
					}
				}
				continue;
			}
			Functor inf = (Functor) symbol;
			// TODO
		}
		for (int i = symbolList.size() - 1; i >= 0; i--) {
			Object symbol = symbolList.ArrayValues[i];
			if (symbol instanceof Method) {
				Method m = (Method) symbol;
				if (name.equals(m.getName())) {
					Type[] p = m.getGenericParameterTypes();
					if (p.length == params.size()) {
						matcher.addCandidate(FunctorFactory.newMethod(m));
					}
				}
				continue;
			}
		}
		return matchCandidate(matcher, params);
	}

	private final Functor resolveConstructor(TypeMatcher matcher, Type newType, TypedTree params) {
		Class<?> newClass = TypeSystem.toClass(newType);
		Constructor<?>[] cList = newClass.getConstructors();
		for (Constructor<?> c : cList) {
			Type[] p = c.getGenericParameterTypes();
			if (Functor.match(matcher, p, params)) {
				return FunctorFactory.newConstructor(newType, c);
			}
		}
		for (Constructor<?> c : cList) {
			Type[] p = c.getGenericParameterTypes();
			if (p.length == params.size()) {
				matcher.addCandidate(FunctorFactory.newConstructor(newType, c));
			}
		}
		return matchCandidate(matcher, params);
	}

	private final Functor resolveStaticMethod(TypeMatcher matcher, Type recvType, String name, TypedTree params) {
		return this.resolveMethod(matcher, recvType, true, name, params);
	}

	private final Functor resolveObjectMethod(TypeMatcher matcher, Type recvType, String name, TypedTree params) {
		return this.resolveMethod(matcher, recvType, false, name, params);
	}

	private final Functor resolveMethod(TypeMatcher matcher, Type recvType, boolean isStaticOnly, String name, TypedTree params) {
		Class<?> recvClass = TypeSystem.toClass(recvType);
		while (recvClass != null) {
			Functor inf = this.matchClassMethod(matcher, isStaticOnly, recvClass, name, params);
			if (inf != null) {
				return inf;
			}
			if (recvClass == Object.class) {
				break;
			}
			recvClass = recvClass.getSuperclass();
		}
		return matchCandidate(matcher, params);
	}

	private Functor matchClassMethod(TypeMatcher matcher, boolean isStaticOnly, Class<?> recvClass, String name, TypedTree params) {
		Method[] mList = recvClass.getDeclaredMethods();
		for (Method m : mList) {
			if (matchMethod(m, isStaticOnly, name)) {
				Type[] p = m.getGenericParameterTypes();
				if (Functor.match(matcher, p, params)) {
					return FunctorFactory.newMethod(m);
				}
			}
		}
		for (Method m : mList) {
			if (matchMethod(m, isStaticOnly, name)) {
				Type[] p = m.getGenericParameterTypes();
				if (p.length == params.size()) {
					matcher.addCandidate(FunctorFactory.newMethod(m));
				}
			}
		}
		return null;
	}

	private boolean matchMethod(Method m, boolean isStaticOnly, String name) {
		if (!Modifier.isPublic(m.getModifiers())) {
			return false;
		}
		if (isStaticOnly && !Modifier.isStatic(m.getModifiers())) {
			return false;
		}
		return name.equals(m.getName());
	}

	// matching library

	private void enforceType(Type req, TypedTree node, Symbol label) {
		TypedTree unode = node.get(label, null);
		if (unode == null) {
			throw this.error(node, Message.SyntaxError, label);
		}
		visit(unode);
		node.set(label, this.enforceType(req, unode));
	}

	private final TypedTree enforceType(Type reqt, TypedTree node) {
		TypedTree n = accept(reqt, node);
		if (n == null) {
			throw error(node, Message.TypeError__, name(reqt), name(node.getType()));
		}
		return n;
	}

	private final TypedTree enforceType(TypeMatcher matcher, Type reqt, TypedTree node) {
		TypedTree n = accept(matcher, reqt, node);
		if (n == null) {
			throw error(node, Message.TypeError__, name(reqt), name(node.getType()));
		}
		return n;
	}

	private final Functor matchCandidate(TypeMatcher matcher, TypedTree params) {
		TypedTree[] buf = null;
		for (Functor inf : matcher.candiateList) {
			if (buf == null) {
				buf = new TypedTree[params.size()];
			}
			if (accept(matcher, inf.getParameterTypes(), params, buf)) {
				return inf;
			}
		}
		buf = null;
		return null;
	}

	private final boolean accept(TypeMatcher matcher, Type[] p, TypedTree params, TypedTree[] buf) {
		if (p.length != params.size()) {
			return false;
		}
		if (p.length > 0) {
			for (int i = 0; i < p.length; i++) {
				TypedTree sub = params.get(i);
				buf[i] = accept(matcher, p[i], sub);
				if (buf[i] == null) {
					return false;
				}
			}
			for (int i = 0; i < p.length; i++) {
				params.set(i, buf[i]);
			}
		}
		return true;
	}

	private final boolean accept(Type[] p, TypedTree params) {
		if (p.length != params.size()) {
			return false;
		}
		if (p.length > 0) {
			TypedTree[] buf = new TypedTree[p.length];
			for (int i = 0; i < p.length; i++) {
				TypedTree sub = params.get(i);
				buf[i] = accept(p[i], sub);
				if (buf[i] == null) {
					return false;
				}
			}
			for (int i = 0; i < p.length; i++) {
				params.set(i, buf[i]);
			}
		}
		return true;
	}

	private final TypedTree accept(TypeMatcher matcher, Type reqt, TypedTree node) {
		if (Functor.match(matcher, reqt, node.getType())) {
			return node;
		}
		Type resolved = matcher.resolve(reqt, null);
		if (resolved != null) {
			return tryCoersion(resolved, node);
		}
		return null;
	}

	private final TypedTree accept(Type reqt, TypedTree node) {
		if (Functor.match(null, reqt, node.getType())) {
			return node;
		}
		return tryCoersion(reqt, node);
	}

	private final TypedTree tryCoersion(Type reqt, TypedTree node) {
		Type expt = node.getType();
		Method m = typeSystem.getCastMethod(expt, reqt);
		if (m != null) {
			TypedTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setInterface(Hint.StaticInvocation2, FunctorFactory.newMethod(m), null);
			return newnode;
		}
		if (expt == Object.class) { // auto upcast
			TypedTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setHint(Hint.DownCast, reqt);
			return newnode;
		}
		return null;
	}

	private TypeCheckerException error(TypedTree node, Message fmt, Object... args) {
		return new TypeCheckerException(node, fmt.toString(), args);
	}

	private TypeCheckerException error(TypedTree node, String fmt, Object... args) {
		return new TypeCheckerException(node, fmt, args);
	}

	// debug message

	void TRACE(String fmt, Object... args) {
		typeSystem.TRACE(fmt, args);
	}

	void TODO(String fmt, Object... args) {
		typeSystem.TODO(fmt, args);
	}

	void DEBUG(String fmt, Object... args) {
		typeSystem.DEBUG(fmt, args);
	}

}
