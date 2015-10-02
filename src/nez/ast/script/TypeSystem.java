package nez.ast.script;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

import nez.ast.Tree;
import nez.util.UList;
import nez.util.UMap;

public class TypeSystem implements CommonSymbols {
	ScriptContext context;
	HashMap<String, Type> nameMap = new HashMap<>();
	UList<Class<?>> classList = new UList<Class<?>>(new Class<?>[4]);

	public TypeSystem(ScriptContext context) {
		this.context = context;
		init();
		initMethod();
	}

	void init() {
		addBaseClass(DynamicOperator.class);
		addBaseClass(StaticOperator.class);
		addBaseClass(StringOperator.class);
		this.setType("void", void.class);
		this.setType("boolean", boolean.class);
		this.setType("byte", byte.class);
		this.setType("int", int.class);
		this.setType("long", long.class);
		this.setType("double", double.class);
		this.setType("String", String.class);
		this.setType("Array", UList.class);
		this.setType("Dict", UMap.class);
	}

	public void setType(String name, Type type) {
		this.nameMap.put(name, type);
	}

	public final Type resolveType(Tree<?> node, Type deftype) {
		if (node == null) {
			return deftype;
		}
		if (node.size() == 0) {
			Type t = this.nameMap.get(node.toText());
			return t == null ? deftype : t;
		}
		if (node.is(_ArrayType)) {
			return GenericType.newType(UList.class, resolveType(node.get(_base), Object.class));
		}
		return deftype;
	}

	public boolean declGlobalVariable(String name, Type type) {
		Type t = this.nameMap.get(name);
		this.nameMap.put(name, type);
		return true;
	}

	public Type resolveGlobalVariableType(String name, Type deftype) {
		Type t = this.nameMap.get(name);
		return t == null ? deftype : t;
	}

	/* Method Map */

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
		this.setMethodMap(cast_key(f, t), m);
	}

	public Method getCastMethod(Class<?> f, Class<?> t) {
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

	public void addBaseClass(Class<?> c) {
		classList.add(c);
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
				}
			}
		}
	}

	// find method

	public Method matchMethod(Class<?> c, boolean isStaticOnly, String name, Type[] types, UList<Method> buf) {
		for (Method m : c.getMethods()) {
			if (isStaticOnly && !this.isStatic(m)) {
				continue;
			}
			if (!name.equals(m.getName())) {
				continue;
			}
			Class<?>[] p = m.getParameterTypes();
			if (p.length != types.length) {
				continue;
			}
			if (this.acceptParameters(p, types)) {
				return m;
			}
			if (buf != null) {
				buf.add(m);
			}
		}
		return null;
	}

	private boolean acceptParameters(Class<?>[] p, Type[] types) {
		for (int j = 0; j < types.length; j++) {
			if (!accept(false, p[j], types[j])) {
				return false;
			}
		}
		return true;
	}

	private Method tryTypeEnforcement(int start, UList<Method> buf, TypedTree params) {
		if (buf != null && start < buf.size()) {
			TypedTree[] a = new TypedTree[params.size()];
			for (int j = start; j < buf.size(); j++) {
				Method m = buf.ArrayValues[j];
				Arrays.fill(a, null);
				Class<?>[] p = m.getParameterTypes();
				if (this.checkTypeEnforce(a, p, params)) {
					for (int i = 0; i < a.length; i++) {
						params.set(i, a[i]);
					}
					buf.clear(start);
					return m;
				}
			}
		}
		return null;
	}

	private boolean checkTypeEnforce(TypedTree[] a, Class<?>[] p, TypedTree params) {
		for (int i = 0; i < p.length; i++) {
			TypedTree sub = params.get(i);
			a[i] = this.checkTypeEnforce(p[i], sub);
			if (a[i] == null) {
				return false;
			}
		}
		return true;
	}

	/* static method */

	public Method resolveStaticMethod(Class<?> c, String name, Type[] types, UList<Method> buf, TypedTree params) {
		int start = buf != null ? buf.size() : 0;
		Method m = matchMethod(c, true/* StaticOnly */, name, types, buf);
		if (m != null) {
			return m;
		}
		return this.tryTypeEnforcement(start, buf, params);
	}

	// function, operator

	public Method resolveFunctionMethod(String name, Type[] types, UList<Method> buf, TypedTree params) {
		int start = buf != null ? buf.size() : 0;
		for (int i = classList.size() - 1; i >= 0; i--) {
			Class<?> c = classList.ArrayValues[i];
			Method m = this.matchMethod(c, true, name, types, buf);
			if (m != null) {
				return m;
			}
		}
		return this.tryTypeEnforcement(start, buf, params);
	}

	// object method

	public Method resolveObjectMethod(Class<?> c, String name, Type[] types, UList<Method> buf, TypedTree params) {
		int start = buf != null ? buf.size() : 0;
		while (c != null) {
			Method m = this.matchMethod(c, true, name, types, buf);
			if (m != null) {
				return m;
			}
			if (c == Object.class) {
				break;
			}
			c = c.getSuperclass();
		}
		return this.tryTypeEnforcement(start, buf, params);
	}

	// type check

	public String pwarn(TypedTree node, String msg) {
		msg = node.formatSourceMessage("warning", msg);
		context.log(msg);
		return msg;
	}

	public String pwarn(TypedTree node, String fmt, Object... args) {
		return pwarn(node, String.format(fmt, args));
	}

	public TypedTree newError(Type req, TypedTree node, String fmt, Object... args) {
		TypedTree newnode = node.newInstance(_StupidCast, 1, null);
		String msg = node.formatSourceMessage("error", String.format(fmt, args));
		newnode.set(0, _msg, node.newStringConst(msg));
		context.log(msg);
		newnode.setMethod(true, this.StaticErrorMethod);
		newnode.setType(req);
		return newnode;
	}

	public String name(Type t) {
		return t == null ? "untyped" : t.toString();
	}

	public TypedTree checkTypeEnforce(Class<?> vt, TypedTree node) {
		Class<?> et = node.getClassType();
		if (accept(false, vt, et)) {
			return node;
		}
		Method m = this.getCastMethod(vt, et);
		if (m != null) {
			TypedTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setMethod(true, m);
			return newnode;
		}
		return null;
	}

	public TypedTree enforceType(Type req, TypedTree node) {
		Class<?> vt = toClass(req);
		Class<?> et = node.getClassType();
		if (accept(false, vt, et)) {
			return node;
		}
		Method m = this.getCastMethod(vt, et);
		if (m != null) {
			TypedTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setMethod(true, m);
			newnode.setType(req);
			return newnode;
		}
		if (et.isAssignableFrom(vt)) {
			pwarn(node, "unexpected downcast: %s => %s", name(et), name(vt));
			TypedTree newnode = node.newInstance(_DownCast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setValue(vt);
			newnode.setType(req);
			return newnode;
		}
		return this.newError(req, node, "type mismatch: requested=%s given=%s", name(req), name(node.getType()));
	}

	public TypedTree makeCast(Type req, TypedTree node) {
		Method m = this.getCastMethod(toClass(req), node.getClassType());
		if (m != null) {
			TypedTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setMethod(true, m);
			newnode.setType(req);
			return newnode;
		}
		return node;
	}

	public void addBaseClass(String path) throws ClassNotFoundException {
		addBaseClass(Class.forName(path));
	}

	boolean acceptArguments(boolean autoBoxing, Method m, Type... args) {
		Class<?>[] p = m.getParameterTypes();
		if (args.length != p.length) {
			return false;
		}
		for (int j = 0; j < args.length; j++) {
			if (!accept(autoBoxing, p[j], args[j])) {
				return false;
			}
		}
		return true;
	}

	boolean accept(boolean autoBoxing, Type p, Type a) {
		if (a == null || p == a) {
			return true;
		}
		if (autoBoxing) {
			if (p == int.class && a == Integer.class) {
				return true;
			}
			if (p == double.class && a == Double.class) {
				return true;
			}
		}
		// System.out.printf("%s %s %s\n", p, a, p.isAssignableFrom(a));
		if (TypeSystem.toClass(p).isAssignableFrom(TypeSystem.toClass(a))) {
			return true;
		}
		return false;
	}

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

	protected Method StaticErrorMethod = null;
	protected Method InterpolationMethod = null;

	void initMethod() {
		try {
			this.StaticErrorMethod = this.getClass().getMethod("throwStaticError", String.class);
			this.InterpolationMethod = this.getClass().getMethod("joinString", Object[].class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public final static String throwStaticError(String msg) {
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

}