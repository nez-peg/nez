package nez.ast.script;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

import konoha.Function;
import nez.ast.Tree;
import nez.ast.script.asm.ScriptCompiler;
import nez.util.UList;
import nez.util.UMap;

public class TypeSystem implements CommonSymbols {
	ScriptContext context;
	ScriptCompiler compl;

	public TypeSystem(ScriptContext context, ScriptCompiler compl) {
		this.context = context;
		this.compl = compl;
		init();
		initMethod();
		initDebug();
	}

	public void init(ScriptCompiler compl) {
		this.compl = compl; // this is called when the complier is instatiated
	}

	void init() {
		loadStaticFunctionClass(DynamicOperator.class, false);
		loadStaticFunctionClass(StaticOperator.class, false);
		loadStaticFunctionClass(StringOperator.class, false);
		this.setType("void", void.class);
		this.setType("boolean", boolean.class);
		this.setType("byte", byte.class);
		this.setType("int", int.class);
		this.setType("long", long.class);
		this.setType("double", double.class);
		this.setType("String", String.class);
		this.setType("Array", konoha.Array.class);
		this.setType("Dict", UMap.class);
		this.setType("Func", Function.class);

	}

	void initDebug() {
		this.setType("Math", Math.class);
		this.setType("System", System.class);
	}

	/* Types */

	HashMap<String, Type> TypeNames = new HashMap<>();

	public void setType(String name, Type type) {
		this.TypeNames.put(name, type);
	}

	public final Type resolveType(Tree<?> node, Type deftype) {
		if (node == null) {
			return deftype;
		}
		if (node.size() == 0) {
			Type t = this.TypeNames.get(node.toText());
			return t == null ? deftype : t;
		}
		if (node.is(_ArrayType)) {
			return GenericType.newType(konoha.Array.class, resolveType(node.get(_base), Object.class));
		}
		return deftype;
	}

	public final Class<?> resolveClass(Tree<?> node, Class<?> deftype) {
		Type t = this.resolveType(node, deftype);
		if (t == null) {
			return deftype;
		}
		return TypeSystem.toClass(t);
	}

	public Type newArrayType(Type elementType) {
		return GenericType.newType(konoha.Array.class, elementType);
	}

	/* GlobalVariables */

	HashMap<String, GlobalVariable> GlobalVariables = new HashMap<>();

	public boolean hasGlobalVariable(String name) {
		return this.GlobalVariables.containsKey(name);
	}

	public GlobalVariable getGlobalVariable(String name) {
		return this.GlobalVariables.get(name);
	}

	public GlobalVariable newGlobalVariable(Type type, String name) {
		Class<?> varClass = this.compl.compileGlobalVariable(TypeSystem.toClass(type), name);
		GlobalVariable gv = new GlobalVariable(type, varClass);
		this.GlobalVariables.put(name, gv);
		return gv;
	}

	/* Method Map */
	private UList<Method> StaticFunctionMethodList = new UList<Method>(new Method[256]);
	private HashMap<String, Method> methodMap = new HashMap<String, Method>();

	private Method getMethodMap(String key) {
		return this.methodMap.get(key);
	}

	private Method setMethodMap(String key, Method method) {
		return this.methodMap.put(key, method);
	}

	private String cast_key(Class<?> f, Class<?> t) {
		return f.getName() + "&" + t.getName();
	}

	public void addCastMethod(Class<?> f, Class<?> t, Method m) {
		// System.out.println("cast: " + cast_key(f, t) + " " + m);
		this.setMethodMap(cast_key(f, t), m);
	}

	public Method getCastMethod(Type f, Type t) {
		return this.getMethodMap(cast_key(TypeSystem.toClass(f), TypeSystem.toClass(t)));
	}

	public Method getCastMethod(Class<?> f, Class<?> t) {
		// System.out.println("cast: " + cast_key(f, t) + " ? ");
		return this.getMethodMap(cast_key(f, t));
	}

	private String convert_key(Class<?> f, Class<?> t) {
		return f.getName() + "!" + t.getName();
	}

	public void addConvertMethod(Class<?> f, Class<?> t, Method m) {
		this.setMethodMap(convert_key(f, t), m);
	}

	public Method getConvertMethod(Class<?> f, Class<?> t) {
		return this.getMethodMap(convert_key(f, t));
	}

	public void loadStaticFunctionClass(Class<?> c, boolean isGenerated) {
		// StaticFunctionMethodList.add(c);
		for (Method m : c.getMethods()) {
			if (isStatic(m)) {
				String name = m.getName();
				if (name.startsWith("to")) {
					Class<?>[] p = m.getParameterTypes();
					if (p.length == 1) {
						Class<?> f = p[0];
						Class<?> t = m.getReturnType();
						if (name.startsWith("to_")) {
							addCastMethod(f, t, m);
						} else {
							addConvertMethod(f, t, m);
						}
					}
					if (!isGenerated) {
						continue;
					}
				}
				this.StaticFunctionMethodList.add(m);
			}
		}
	}

	public void importStaticClass(String path) throws ClassNotFoundException {
		Class<?> c = Class.forName(path);
		loadStaticFunctionClass(c, false);
		this.setType(c.getSimpleName(), c);
	}

	/**
	 * Resolving method
	 * 
	 */

	private Method matchMethod(Class<?> c, boolean isStaticOnly, TypeVarMatcher matcher, String name, Type[] types, UList<Method> buf) {
		for (Method m : isStaticOnly ? c.getMethods() : c.getDeclaredMethods()) {
			if (Modifier.isPublic(m.getModifiers())) {
				if (matchMethod(m, isStaticOnly, matcher, name, types, buf)) {
					return m;
				}
			}
		}
		return null;
	}

	private boolean matchMethod(Method m, boolean isStaticOnly, TypeVarMatcher matcher, String name, Type[] types, UList<Method> buf) {
		if (isStaticOnly && !this.isStatic(m)) {
			return false;
		}
		if (!name.equals(m.getName())) {
			return false;
		}
		Class<?>[] p = m.getParameterTypes();
		if (p.length != types.length) {
			return false;
		}
		if (matcher == null || !isGenericMethod(m)) {
			if (this.acceptParameters(null, p, types)) {
				return true;
			}
		} else {
			Type[] gp = m.getGenericParameterTypes();
			if (this.acceptParameters(matcher, gp, types)) {
				return true;
			}
			matcher.init();
		}
		if (buf != null) {
			buf.add(m);
		}
		return false;
	}

	private boolean isGenericMethod(Method m) {
		Type r = m.getGenericReturnType();
		if (!(r instanceof Class<?>)) {
			return true;
		}
		for (Type t : m.getGenericParameterTypes()) {
			if (!(t instanceof Class<?>)) {
				return true;
			}
		}
		return false;
	}

	private boolean acceptParameters(TypeVarMatcher matcher, Type[] p, Type[] types) {
		for (int j = 0; j < types.length; j++) {
			if (!accept(matcher, p[j], types[j])) {
				return false;
			}
		}
		return true;
	}

	private final boolean accept(TypeVarMatcher matcher, Type p, Type a) {
		if (a == null || p == a) {
			return true;
		}
		if (p instanceof Class<?> || matcher == null) {
			if (((Class<?>) p).isAssignableFrom(TypeSystem.toClass(a))) {
				return true;
			}
			return false;
		}
		return matcher.match(p, a);
	}

	private Method checkMethodTypeEnforcement(int start, UList<Method> buf, TypeVarMatcher matcher, TypedTree params) {
		if (buf != null && start < buf.size()) {
			TypedTree[] results = new TypedTree[params.size()];
			for (int j = start; j < buf.size(); j++) {
				Method m = buf.ArrayValues[j];
				Arrays.fill(results, null);
				Type[] p = m.getParameterTypes();
				if (matcher != null && isGenericMethod(m)) {
					p = m.getGenericParameterTypes();
				}
				if (this.checkArgumentTypeEnforcement(results, matcher, p, params)) {
					for (int i = 0; i < results.length; i++) {
						params.set(i, results[i]);
					}
					buf.clear(start);
					return m;
				}
			}
		}
		return null;
	}

	private boolean checkArgumentTypeEnforcement(TypedTree[] results, TypeVarMatcher matcher, Type[] p, TypedTree params) {
		for (int i = 0; i < p.length; i++) {
			TypedTree sub = params.get(i);
			results[i] = matcher != null ? this.checkTypeEnforce(matcher, p[i], sub) : this.checkTypeEnforce((Class<?>) p[i], sub);
			if (results[i] == null) {
				matcher.init();
				return false;
			}
		}
		return true;
	}

	public TypedTree checkTypeEnforce(TypeVarMatcher matcher, Type p, TypedTree node) {
		if (accept(matcher, p, node.getType())) {
			return node;
		}
		Type resolved = matcher.resolve(p, null);
		if (resolved != null) {
			Method m = this.getCastMethod(node.getType(), resolved);
			if (m != null) {
				TypedTree newnode = node.newInstance(_Cast, 1, null);
				newnode.set(0, _expr, node);
				newnode.setMethod(Hint.StaticInvocation, m, null);
				return newnode;
			}
		}
		return null;
	}

	public TypedTree checkTypeEnforce(Class<?> vt, TypedTree node) {
		Class<?> et = node.getClassType();
		if (accept(null, vt, et)) {
			return node;
		}
		Method m = this.getCastMethod(et, vt);
		if (m != null) {
			TypedTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setMethod(Hint.StaticInvocation, m, null);
			return newnode;
		}
		return null;
	}

	/* static method */

	public Method resolveStaticMethod(Class<?> c, String name, Type[] types, UList<Method> buf, TypedTree params) {
		int start = buf != null ? buf.size() : 0;
		Method m = matchMethod(c, true/* StaticOnly */, null, name, types, buf);
		if (m != null) {
			return m;
		}
		return this.checkMethodTypeEnforcement(start, buf, null, params);
	}

	// function, operator

	public Method resolveFunctionMethod(String name, Type[] types, UList<Method> buf, TypedTree params) {
		int start = buf != null ? buf.size() : 0;
		for (int i = StaticFunctionMethodList.size() - 1; i >= 0; i--) {
			Method m = StaticFunctionMethodList.ArrayValues[i];
			if (this.matchMethod(m, true, null, name, types, buf)) {
				return m;
			}
		}
		return this.checkMethodTypeEnforcement(start, buf, null, params);
	}

	// object method

	public Method resolveObjectMethod(Type t, TypeVarMatcher matcher, String name, Type[] types, UList<Method> buf, TypedTree params) {
		int start = buf != null ? buf.size() : 0;
		Class<?> c = TypeSystem.toClass(t);
		while (c != null) {
			Method m = this.matchMethod(c, false, matcher, name, types, buf);
			if (m != null) {
				return m;
			}
			if (c == Object.class) {
				break;
			}
			c = c.getSuperclass();
		}
		return this.checkMethodTypeEnforcement(start, buf, matcher, params);
	}

	// type check

	public TypeCheckerException error(TypedTree node, String fmt, Object... args) {
		return new TypeCheckerException(this, 1, node, fmt, args);
	}

	public TypedTree newError(Type req, TypedTree node, String fmt, Object... args) {
		TypedTree newnode = node.newInstance(_Error, 1, null);
		String msg = node.formatSourceMessage("error", String.format(fmt, args));
		newnode.set(0, _msg, node.newStringConst(msg));
		context.log(msg);
		newnode.setMethod(Hint.StaticInvocation, this.StaticErrorMethod, null);
		return newnode;
	}

	public String name(Type t) {
		if (t == null) {
			return "untyped";
		}
		if (t instanceof Class<?>) {
			String n = ((Class) t).getName();
			if (n.startsWith("java.lang.") || n.startsWith("java.util.")) {
				return n.substring(10);
			}
			return n;
		}
		return t.toString();
	}

	public String reportWarning(TypedTree node, String msg) {
		msg = node.formatSourceMessage("warning", msg);
		context.log(msg);
		return msg;
	}

	public String reportWarning(TypedTree node, String fmt, Object... args) {
		return reportWarning(node, String.format(fmt, args));
	}

	public TypedTree enforceType(Type req, TypedTree node) {
		Class<?> vt = toClass(req);
		Class<?> et = node.getClassType();
		if (accept(null, vt, et)) {
			return node;
		}
		Method m = this.getCastMethod(et, vt);
		if (m != null) {
			TypedTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setMethod(Hint.StaticInvocation, m, null);
			return newnode;
		}
		if (et.isAssignableFrom(vt)) {
			reportWarning(node, "unexpected downcast: %s => %s", name(et), name(vt));
			TypedTree newnode = node.newInstance(_DownCast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setClass(Hint.DownCast, vt);
			newnode.setType(req);
			return newnode;
		}
		return this.newError(req, node, "type mismatch: requested=%s given=%s", name(req), name(node.getType()));
	}

	public TypedTree makeCast(Type req, TypedTree node) {
		Method m = this.getCastMethod(node.getClassType(), toClass(req));
		if (m != null) {
			TypedTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setMethod(Hint.StaticInvocation, m, null);
			newnode.setType(req);
			return newnode;
		}
		return node;
	}

	// boolean acceptArguments(boolean autoBoxing, Method m, Type... args) {
	// Class<?>[] p = m.getParameterTypes();
	// if (args.length != p.length) {
	// return false;
	// }
	// for (int j = 0; j < args.length; j++) {
	// if (!accept(autoBoxing, p[j], args[j])) {
	// return false;
	// }
	// }
	// return true;
	// }

	public Type PrimitiveType(Type t) {
		if (t == Double.class || t == Float.class || t == float.class) {
			return double.class;
		}
		if (t == Long.class) {
			return long.class;
		}
		if (t == Integer.class || t == Short.class || t == short.class) {
			return int.class;
		}
		if (t == Character.class) {
			return char.class;
		}
		if (t == Character.class) {
			return char.class;
		}
		if (t == Boolean.class) {
			return boolean.class;
		}
		if (t == Byte.class) {
			return byte.class;
		}
		return t;
	}

	public static interface BinaryTypeUnifier {
		Type unify(Type t, Type t2);
	}

	private static class Additive implements BinaryTypeUnifier {
		@Override
		public Type unify(Type t, Type t2) {
			if (t == t2) {
				return t;
			}
			if (t == Object.class || t2 == Object.class) {
				return Object.class;
			}
			if (t == BigDecimal.class || t2 == BigDecimal.class) {
				return BigDecimal.class;
			}
			if (t == BigInteger.class || t2 == BigInteger.class) {
				return BigInteger.class;
			}
			if (t == double.class || t2 == double.class) {
				return double.class;
			}
			if (t == long.class || t2 == long.class) {
				return long.class;
			}
			if (t == int.class || t2 == int.class || t == byte.class || t2 == byte.class) {
				return int.class;
			}
			return t;
		}
	}

	private static class Equator implements BinaryTypeUnifier {
		@Override
		public Type unify(Type t, Type t2) {
			if (t == t2) {
				return t;
			}
			if (t == Object.class || t2 == Object.class) {
				return Object.class;
			}
			if (t == BigDecimal.class || t2 == BigDecimal.class) {
				return BigDecimal.class;
			}
			if (t == BigInteger.class || t2 == BigInteger.class) {
				return BigInteger.class;
			}
			if (t == double.class || t2 == double.class) {
				return double.class;
			}
			if (t == long.class || t2 == long.class) {
				return long.class;
			}
			if (t == int.class || t2 == int.class) {
				return int.class;
			}
			return t;
		}
	}

	private static class TComparator implements BinaryTypeUnifier {
		@Override
		public Type unify(Type t, Type t2) {
			if (t == t2) {
				return t;
			}
			if (t == BigDecimal.class || t2 == BigDecimal.class) {
				return BigDecimal.class;
			}
			if (t == BigInteger.class || t2 == BigInteger.class) {
				return BigInteger.class;
			}
			if (t == double.class || t2 == double.class) {
				return double.class;
			}
			if (t == long.class || t2 == long.class) {
				return long.class;
			}
			if (t == int.class || t2 == int.class) {
				return int.class;
			}
			return t;
		}
	}

	private static class Bitwise implements BinaryTypeUnifier {
		@Override
		public Type unify(Type t, Type t2) {
			if (t == Object.class || t2 == Object.class) {
				return Object.class;
			}
			if (t == BigInteger.class || t2 == BigInteger.class) {
				return BigInteger.class;
			}
			if (t == long.class || t2 == long.class) {
				return long.class;
			}
			if (t == int.class || t2 == int.class) {
				return int.class;
			}
			return t;
		}
	}

	public static BinaryTypeUnifier UnifyAdditive = new Additive();
	public static BinaryTypeUnifier UnifyEquator = new Equator();
	public static BinaryTypeUnifier UnifyComparator = new TComparator();
	public static BinaryTypeUnifier UnifyBitwise = new Bitwise();

	protected Method DynamicGetter = null;
	protected Method DynamicSetter = null;
	protected Method ObjectIndexer = null;
	protected Method ObjectSetIndexer = null;

	protected Method StaticErrorMethod = null;
	protected Method InterpolationMethod = null;

	protected Method[] invokeDynamicMethods = new Method[4];

	void initMethod() {
		try {
			this.DynamicGetter = this.getClass().getMethod("getDynamicField", Object.class, String.class);
			this.DynamicSetter = this.getClass().getMethod("setDynamicField", Object.class, String.class, Object.class);
			this.ObjectIndexer = this.getClass().getMethod("getObjectIndexer", Object.class, Object.class);
			this.ObjectSetIndexer = this.getClass().getMethod("setObjectIndexer", Object.class, Object.class, Object.class);
			this.StaticErrorMethod = this.getClass().getMethod("throwStaticError", String.class);
			this.InterpolationMethod = this.getClass().getMethod("joinString", Object[].class);
			this.invokeDynamicMethods[0] = this.getClass().getMethod("invoke0", Object.class, String.class);
			this.invokeDynamicMethods[1] = this.getClass().getMethod("invoke1", Object.class, String.class, Object.class);
			this.invokeDynamicMethods[2] = this.getClass().getMethod("invoke2", Object.class, String.class, Object.class, Object.class);
			this.invokeDynamicMethods[3] = this.getClass().getMethod("invoke3", Object.class, String.class, Object.class, Object.class, Object.class);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}

	public Method getInvokeDynamicFunction(int paramsize) {
		if (paramsize < this.invokeDynamicMethods.length) {
			return this.invokeDynamicMethods[paramsize];
		}
		return null;
	}

	private final static Class<?> prim(Object o) {
		if (o instanceof Number) {
			Class<?> t = o.getClass();
			if (t == Integer.class) {
				return int.class;
			}
			if (t == Double.class) {
				return double.class;
			}
			if (t == Long.class) {
				return long.class;
			}
			if (t == Byte.class) {
				return byte.class;
			}
			if (t == Float.class) {
				return float.class;
			}
			if (t == Character.class) {
				return char.class;
			}
			if (t == Short.class) {
				return short.class;
			}
			return t;
		}
		if (o instanceof Boolean) {
			return boolean.class;
		}
		return o.getClass();
	}

	public final static Object invoke0(Object self, String name) {
		try {
			Method m = self.getClass().getMethod(name);
			return m.invoke(self);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			// e.printStackTrace();
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Object invoke1(Object self, String name, Object a1) {
		try {
			Class<?>[] p = { prim(a1) };
			Method m = self.getClass().getMethod(name, p);
			return m.invoke(self, a1);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			// e.printStackTrace();
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Object invoke2(Object self, String name, Object a1, Object a2) {
		try {
			Class<?>[] p = { prim(a1), prim(a2) };
			Method m = self.getClass().getMethod(name, p);
			return m.invoke(self, a1, a2);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			// e.printStackTrace();
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Object invoke3(Object self, String name, Object a1, Object a2, Object a3) {
		try {
			Class<?>[] p = { prim(a1), prim(a2), prim(a3) };
			Method m = self.getClass().getMethod(name, p);
			return m.invoke(self, a1, a2, a3);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			// e.printStackTrace();
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Object getObjectIndexer(Object o, Object index) {
		// Method m = o.getClass().getMethod("get", index.getClass());
		return null;
	}

	public final static Object setObjectIndexer(Object o, Object index, Object value) {
		// Method m = o.getClass().getMethod("get", index.getClass());
		return null;
	}

	public final static Object getDynamicField(Object o, String name) {
		try {
			Field f = o.getClass().getField(name);
			return f.get(o);
		} catch (NoSuchFieldException e) {
			// e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		throw new ScriptRuntimeException("undefined field: %s of %s", name, o.getClass().getSimpleName());
	}

	public final static void setDynamicField(Object o, String name, Object v) {
		try {
			Field f = o.getClass().getField(name);
			f.set(o, v);
			return;
		} catch (NoSuchFieldException e) {
			// e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		throw new ScriptRuntimeException("undefined field: %s of %s", name, o.getClass().getSimpleName());
	}

	public final static void throwStaticError(String msg) {
		throw new ScriptRuntimeException(msg);
	}

	public final static String joinString(Object... args) {
		StringBuilder sb = new StringBuilder();
		for (Object a : args) {
			sb.append(a);
		}
		return sb.toString();
	}

	// typeof

	public Class<?> typeof(Tree<?> node) {
		if (node instanceof TypedTree) {
			Class<?> type = ((TypedTree) node).getClassType();
			if (type != null) {
				return type;
			}
		}
		return Object.class; // untyped
	}

	public final boolean isStatic(Method m) {
		return Modifier.isStatic(m.getModifiers());
	}

	public final static Class<?> toClass(Type type) {
		if (type instanceof Class<?>) {
			return (Class<?>) type;
		}
		return ((GenericType) type).base;
	}

	public Field getField(Class<?> c, String name) {
		try {
			Field f = c.getField(name);
			if (Modifier.isPublic(f.getModifiers())) {
				return f;
			}
		} catch (NoSuchFieldException e) {
		} catch (SecurityException e) {
		}
		return null;
	}

	public boolean isDynamic(Type c) {
		return c == Object.class;
	}

	public Type dynamicType() {
		return Object.class;
	}

}