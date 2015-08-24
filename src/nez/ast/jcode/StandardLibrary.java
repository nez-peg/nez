package nez.ast.jcode;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class StandardLibrary {

	public static class console {

		public static Object log(int x) {
			System.out.println(x);
			return null;
		}

		public static Object log(double x) {
			System.out.println(x);
			return null;
		}

		public static Object log(boolean x) {
			System.out.println(x);
			return null;
		}

		public static Object log(String x) {
			System.out.println(x);
			return null;
		}

		public static Object log(Object x) {
			System.out.println(x);
			return null;
		}
	}

	private static String prefixName = "nez.ast.jcode.StandardLibrary$";

	public static CallSite bootstrap(Lookup lookup, String methodName, MethodType type, String className)
			throws NoSuchMethodException, IllegalAccessException {
		Class<?> ownerClass = getClass("console");
		MethodHandle mh = lookup.findStatic(ownerClass, methodName,
				MethodType.methodType(type.returnType(), type.parameterArray()));
		return new ConstantCallSite(mh);
	}

	public static Class<?> getClass(String className) {
		ClassLoader loader = ClassLoader.getSystemClassLoader();
		try {
			Class<?> findedClass = loader.loadClass(prefixName + className);
			return findedClass;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

}